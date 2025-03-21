//--------------------------------------------------------------------------------------------------
// Shell Lab                               Spring 2024                           System Programming
//
/// @file
/// @brief csapsh - a tiny shell with job control
/// @author Jinho Choi
/// @studid 2020-12852
///
/// @section changelog Change Log
/// 2020/11/14 Bernhard Egger adapted from CS:APP lab
/// 2021/11/03 Bernhard Egger improved for 2021 class
/// 2024/05/11 ARC lab improved for 2024 class
///
/// @section license_section License
/// Copyright CS:APP authors
/// Copyright (c) 2020-2023, Computer Systems and Platforms Laboratory, SNU
/// Copyright (c) 2024, Architecture and Code Optimization Laboratory, SNU
/// All rights reserved.
///
/// Redistribution and use in source and binary forms, with or without modification, are permitted
/// provided that the following conditions are met:
///
/// - Redistributions of source code must retain the above copyright notice, this list of condi-
///   tions and the following disclaimer.
/// - Redistributions in binary form must reproduce the above copyright notice, this list of condi-
///   tions and the following disclaimer in the documentation and/or other materials provided with
///   the distribution.
///
/// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
/// IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED  TO, THE IMPLIED  WARRANTIES OF MERCHANTABILITY
/// AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
/// CONTRIBUTORS  BE LIABLE FOR ANY DIRECT,  INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY,  OR CONSE-
/// QUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
/// LOSS OF USE, DATA,  OR PROFITS; OR BUSINESS INTERRUPTION)  HOWEVER CAUSED AND ON ANY THEORY OF
/// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
/// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
/// DAMAGE.
//--------------------------------------------------------------------------------------------------

#define _GNU_SOURCE          // to get basename() in string.h
#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

#include "jobcontrol.h"
#include "parser.h"

//--------------------------------------------------------------------------------------------------
// Global variables
//

char prompt[] = "csapsh> ";  ///< command line prompt (DO NOT CHANGE)
int emit_prompt = 1;         ///< 1: emit prompt; 0: do not emit prompt
int verbose = 0;             ///< 1: verbose mode; 0: normal mode


//--------------------------------------------------------------------------------------------------
// Functions that you need to implement
//
// Refer to the detailed descriptions at each function implementation.

void eval(char *cmdline);
int  builtin_cmd(char *argv[]);
void do_bgfg(char *argv[]);
void waitfg(int jid);

void sigchld_handler(int sig);
void sigint_handler(int sig);
void sigtstp_handler(int sig);

void redirect_stream(int cidx, int **fds, int num_cmds, char *infile, char *outfile);
int execjob(char ***argv, int num_cmds, char *infile, char *outfile, char *cmdline, JobState mode);


//--------------------------------------------------------------------------------------------------
// Implemented functions - do not modify
//

// main & helper functions
int main(int argc, char **argv);
void usage(const char *program);
void unix_error(char *msg);
void app_error(char *msg);
void Signal(int signum, void (*handler)(int));
void sigquit_handler(int sig);
char* stripnewline(char *str);

#define VERBOSE(...)  { if (verbose) { fprintf(stderr, ##__VA_ARGS__); fprintf(stderr, "\n"); } }





/// @brief Program entry point.
int main(int argc, char **argv)
{
  char c;
  char cmdline[MAXLINE];

  // redirect stderr to stdout so that the driver will get all output on the pipe connected
  // to stdout.
  dup2(STDOUT_FILENO, STDERR_FILENO);

  // set Standard I/O's buffering mode for stdout and stderr to line buffering
  // to avoid any discrepancies between running the shell interactively or via the driver
  setlinebuf(stdout);
  setlinebuf(stderr);

  // parse command line
  while ((c = getopt(argc, argv, "hvp")) != EOF) {
    switch (c) {
      case 'h': usage(argv[0]);        // print help message
                break;
      case 'v': verbose = 1;           // emit additional diagnostic info
                break;
      case 'p': emit_prompt = 0;       // don't print a prompt
                break;                 // handy for automatic testing
      default:  usage(argv[0]);        // invalid option -> print help message
    }
  }

  // install signal handlers
  VERBOSE("Installing signal handlers...");
  Signal(SIGINT,  sigint_handler);     // Ctrl-c
  Signal(SIGTSTP, sigtstp_handler);    // Ctrl-z
  Signal(SIGCHLD, sigchld_handler);    // Terminated or stopped child
  Signal(SIGQUIT, sigquit_handler);    // Ctrl-Backslash (useful to exit shell)

  // execute read/eval loop
  VERBOSE("Execute read/eval loop...");
  while (1) {
    if (emit_prompt) { printf("%s", prompt); fflush(stdout); }

    if ((fgets(cmdline, MAXLINE, stdin) == NULL) && ferror(stdin)) {
      app_error("fgets error");
    }

    if (feof(stdin)) break;            // end of input (Ctrl-d)

    eval(cmdline);

    fflush(stdout);
  }

  // that's all, folks!
  return EXIT_SUCCESS;
}



/// @brief Evaluate the command line. The function @a parse_cmdline() does the heavy lifting of
///        parsing the command line and splitting it into separate char **argv[] arrays that
///        represent individual commands with their arguments.
///        A command line consists of one job or several jobs connected via ampersand('&'). And
///        a job consists of one process or several processes connected via pipes. Optionally,
///        the output of the entire job can be saved into a file specified by outfile.
///        The shell waits for jobs that are executed in the foreground, while jobs that run
///        in the background are not waited for.
/// @param cmdline command line

void eval(char *cmdline)
{
  #define P_READ  0                      // pipe read end
  #define P_WRITE 1                      // pipe write end

  char *str = strdup(cmdline);
  VERBOSE("eval(%s)", stripnewline(str));
  free(str);

  char ****argv  = NULL;
  char **infile  = NULL;
  char **outfile = NULL;
  char **commands = NULL;
  int *num_cmds = NULL;
  JobState *mode = NULL;

  // parse command line
  int njob = parse_cmdline(cmdline, &mode, &argv, &infile, &outfile, &num_cmds, &commands);
  VERBOSE("parse_cmdline(...) = %d", njob);
  if (njob == -1) return;              // parse error
  if (njob == 0)  return;              // no input
  assert(njob > 0);

  // dump parsed command line
  for (int job_idx=0; job_idx<njob; job_idx++) {
    if (verbose) dump_cmdstruct(argv[job_idx], infile[job_idx], outfile[job_idx], mode[job_idx]);
  }

  // if the command is a single built-in command (no pipes or redirection), do not fork. Instead,
  // execute the command directly in this process. Note that this is not just to be more efficient -
  // it is necessary for the 'quit' command to work.
  if ((njob == 1) && (num_cmds[0] == 1) && (outfile[0] == NULL)) {
    if (builtin_cmd(argv[0][0])) {
      free_cmdstruct(argv, infile, outfile, mode);
      return;
    }
  }

  //
  // TODO
  //

  int jid = -1;

  for (int jidx = 0; jidx < njob; jidx++)
  {
    VERBOSE("jidx: %d, numcmds: %d\n", jidx, num_cmds[jidx]);
    jid = execjob(argv[jidx], num_cmds[jidx], infile[jidx], outfile[jidx], commands[jidx], mode[jidx]);
    if (mode[jidx] == jsForeground) waitfg(jid);
    else printjob(jid);
  }

  free_cmdstruct(argv, infile, outfile, mode);

  // These lines are placeholder for minimal functionality.
  // You can either use these or not.
  // int jid = -1;
  // if (mode[0] == jsForeground) waitfg(jid);
  // else printjob(jid);
}


/// @brief Execute built-in commands
/// @param argv command
/// @retval 1 if the command was a built-in command
/// @retval 0 otherwise
int builtin_cmd(char *argv[])
{
  VERBOSE("builtin_cmd(%s)", argv[0]);
  if      (strcmp(argv[0], "quit") == 0) exit(EXIT_SUCCESS);
  else if (strcmp(argv[0], "jobs") == 0) listjobs();
  else if (strcmp(argv[0], "fg") == 0 || strcmp(argv[0], "bg") == 0) do_bgfg(argv);
  else    return 0;
  //
  // TODO
  //

  return 1;
}

/// @brief Execute the builtin bg and fg commands
/// @param argv char* argv[] array where
///           argv[0] is either "bg" or "fg"
///           argv[1] is either a job id "%<n>", a process group id "@<n>" or a process id "<n>"
void do_bgfg(char *argv[])
{
  VERBOSE("do_bgfg(%s, %s)", argv[0], argv[1]);

  if (argv[1] == NULL) {
    printf("%s command requires PID or %%jobid argument\n", argv[0]);
    return;
  }

  //
  // TODO
  //
  Job *job;

  if (argv[1][0] == '%')
  {
    int jid = atoi(&argv[1][1]);
    job = getjob_jid(jid);
    if (!job)
    {
      printf("[%%%d]: No such job\n", jid);
      return ;
    }
  }
  else if (argv[1][0] == '@')
  {
    int pgid = atoi(&argv[1][1]);
    job = getjob_pgid(pgid);
    if (!job)
    {
      printf("(@%d): No such process group\n", pgid);
      return ;
    }
  }
  else
  {
    int pid = atoi(argv[1]);
    job = getjob_pid(pid);
    if (!job)
    {
      printf("{%d}: No such process\n", pid);
      return ;
    }
  }

  if (strcmp(argv[0], "fg") == 0)
  {
    if (job->state == jsBackground)
    {
      job->state = jsForeground;
      waitfg(job->jid);
    }
    else if (job->state == jsStopped)
    {
      job->state = jsForeground;
      kill(-job->pgid, SIGCONT);
      waitfg(job->jid);
    }
    else
      app_error("fg");
  }
  else
  {
    if (job->state == jsStopped)
    {
      VERBOSE("Stopped job %%%d to bg\n", job->jid);
      job->state = jsBackground;
      kill(-job->pgid, SIGCONT);
      printjob(job->jid);
    }
    else
      app_error("bg");
  }
}

/// @brief Block until job jid is no longer in the foreground
/// @param jid job ID of foreground job
void waitfg(int jid)
{
  if (verbose) {
    fprintf(stderr, "waitfg(%%%d): ", jid);
    printjob(jid);
  }

  //
  // TODO
  //
  Job *job = getjob_jid(jid);
  if (!job)
    app_error("getjob_jid");

  while (job->state == jsForeground && waitpid(-job->pgid, 0, WNOHANG) == 0)
  {
    VERBOSE("waiting...\n");
    sleep(1);
  }
  if (job->state == jsForeground)
    deletejob(jid);
}


//--------------------------------------------------------------------------------------------------
// Signal handlers
//

/// @brief SIGCHLD handler. Sent to the shell whenever a child process terminates or stops because
///        it received a SIGSTOP or SIGTSTP signal. This handler reaps all zombies.
/// @param sig signal (SIGCHLD)
void sigchld_handler(int sig)
{
  VERBOSE("[SCH] SIGCHLD handler (signal: %d)", sig);

  //
  // TODO
  //
  pid_t wpid;
  int status;

  wpid = waitpid(-1, &status, WNOHANG | WUNTRACED);
  VERBOSE("wpid: %d", wpid);
  if (wpid > 0)
  {
    VERBOSE("caught %d in handler", wpid);
    Job *wjob = getjob_pid(wpid);
    if (!wjob)
      app_error("getjob_pid");
    if (WIFEXITED(status))
    {
      VERBOSE("    exited");
      if (--wjob->nproc_cur == 0 && wjob->state != jsForeground)
        deletejob(wjob->jid);
    }
    else if (WIFSTOPPED(status))
    {
      VERBOSE("    stopped");
      if (wjob->state != jsStopped)
      {
        kill(-wjob->pgid, SIGTSTP);
        wjob->state = jsStopped;
      }
    }
  }
  else if (wpid < 0)
    unix_error("sigchld_handler");
}

/// @brief SIGINT handler. Sent to the shell whenever the user types Ctrl-c at the keyboard.
///        Forward the signal to the foreground job.
/// @param sig signal (SIGINT)
void sigint_handler(int sig)
{
  VERBOSE("[SIH] SIGINT handler (signal: %d)", sig);

  //
  // TODO
  //

  Job *job = getjob_foreground();
  if (job)
  {
    kill(-job->pgid, SIGINT);
  }
}

/// @brief SIGTSTP handler. Sent to the shell whenever the user types Ctrl-z at the keyboard.
///        Forward the signal to the foreground job.
/// @param sig signal (SIGTSTP)
void sigtstp_handler(int sig)
{
  VERBOSE("[SSH] SIGTSTP handler (signal: %d)", sig);

  //
  // TODO
  //
  Job *job = getjob_foreground();
  if (job)
  {
    if (job->state == jsForeground)
    {
      job->state = jsStopped;
      kill(-job->pgid, SIGTSTP);
    }
  }
}


//--------------------------------------------------------------------------------------------------
// Other helper functions
//

/// @brief Print help message. Does not return.
__attribute__((noreturn))
void usage(const char *program)
{
  printf("Usage: %s [-hvp]\n", basename(program));
  printf("   -h   print this message\n");
  printf("   -v   print additional diagnostic information\n");
  printf("   -p   do not emit a command prompt\n");
  exit(EXIT_FAILURE);
}

/// @brief Print a Unix-level error message based on errno. Does not return.
/// param msg additional descriptive string (optional)
__attribute__((noreturn))
void unix_error(char *msg)
{
  if (msg != NULL) fprintf(stdout, "%s: ", msg);
  fprintf(stdout, "%s\n", strerror(errno));
  exit(EXIT_FAILURE);
}

/// @brief Print an application-level error message. Does not return.
/// @param msg error message
__attribute__((noreturn))
void app_error(char *msg)
{
  fprintf(stdout, "%s\n", msg);
  exit(EXIT_FAILURE);
}

/// @brief Wrapper for sigaction(). Installs the function @a handler as the signal handler
///        for signal @a signum. Does not return on error.
/// @param signum signal number to catch
/// @param handler signal handler to invoke
void Signal(int signum, void (*handler)(int))
{
  struct sigaction action;

  action.sa_handler = handler;
  sigemptyset(&action.sa_mask); // block sigs of type being handled
  action.sa_flags = SA_RESTART; // restart syscalls if possible

  if (sigaction(signum, &action, NULL) < 0) unix_error("Sigaction");
}

/// @brief SIGQUIT handler. Terminates the shell.
__attribute__((noreturn))
void sigquit_handler(int sig)
{
  printf("Terminating after receipt of SIGQUIT signal\n");
  exit(EXIT_SUCCESS);
}

/// @brief strip newlines (\n) from a string. Warning: modifies the string itself!
///        Inside the string, newlines are replaced with a space, at the end
///        of the string, the newline is deleted.
///
/// @param str string
/// @reval char* stripped string
char* stripnewline(char *str)
{
  char *p = str;
  while (*p != '\0') {
    if (*p == '\n') *p = *(p+1) == '\0' ? '\0' : ' ';
    p++;
  }

  return str;
}


int execjob(char ***argv, int num_cmds, char *infile, char *outfile, char *cmdline, JobState mode)
{
  int jid;
  int **pipes = malloc((num_cmds - 1) * sizeof(int *));
  if (!pipes)
    unix_error("execjob_fg");
  for (int i = 0; i < num_cmds - 1; i++)
  {
    pipes[i] = malloc(2 * sizeof(int));
    if (!pipes[i])
      unix_error("execjob_fg");
  }

  sigset_t  mask, prev;

  sigemptyset(&mask);
  sigaddset(&mask, SIGCHLD);
  sigprocmask(SIG_BLOCK, &mask, &prev);

  pid_t *pids = malloc(num_cmds * sizeof(pid_t));
  if (!pids)
    unix_error("malloc");

  for (int cidx = 0; cidx < num_cmds; cidx++)
  {

    if (cidx != num_cmds - 1 && pipe(pipes[cidx]) < 0)
      unix_error("execjob_fg");

    if ((pids[cidx] = fork()) < 0)
      unix_error("execjob_fg");
    else if (pids[cidx] == 0)
    {
      redirect_stream(cidx, pipes, num_cmds, infile, outfile);

      Signal(SIGINT, SIG_DFL);
      Signal(SIGTSTP, SIG_DFL);
      sigprocmask(SIG_SETMASK, &prev, 0);

      if (execvp(argv[cidx][0], argv[cidx]) < 0)
        unix_error(0);
    }
    else
    {
      setpgid(pids[cidx], pids[0]);
      if (cidx > 0)
      {
        close(pipes[cidx - 1][P_READ]);
        close(pipes[cidx - 1][P_WRITE]);
      }
    }
  }

  jid = addjob(pids[0], pids, num_cmds, mode, cmdline);

  for (int i = 0; i < num_cmds - 1; i++)
    free(pipes[i]);
  free(pipes);
  sigprocmask(SIG_SETMASK, &prev, 0);
  return jid;
}

void redirect_stream(int cidx, int **pipe, int num_cmds, char *infile, char *outfile)
{
  if (cidx == 0)
  {
    if (infile)
    {
      int fd = open(infile, O_RDONLY);
      if (fd < 0)
        unix_error("execjob_fg");
      dup2(fd, STDIN_FILENO);
    }
  }
  else
  {
    dup2(pipe[cidx - 1][P_READ], STDIN_FILENO);
    close(pipe[cidx - 1][P_WRITE]);
  }

  if (cidx == num_cmds - 1)
  {
    if (outfile)
    {
      int fd = open(outfile, O_WRONLY | O_CREAT, 0644);
      if (fd < 0)
        unix_error("execjob_fg");
      dup2(fd, STDOUT_FILENO);
    }
  }
  else
  {
    dup2(pipe[cidx][P_WRITE], STDOUT_FILENO);
    close(pipe[cidx][P_READ]);
  }
}
