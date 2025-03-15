//--------------------------------------------------------------------------------------------------
// System Programming                         I/O Lab                                   Spring 2024
//
/// @file
/// @brief resursively traverse directory tree and list all entries
/// @author Jinho Choi
/// @studid 2020-12852
//--------------------------------------------------------------------------------------------------

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <dirent.h>
#include <errno.h>
#include <unistd.h>
#include <stdarg.h>
#include <assert.h>
#include <grp.h>
#include <pwd.h>

#define MAX_DIR 64            ///< maximum number of supported directories

/// @brief output control flags
#define F_DIRONLY   0x1       ///< turn on direcetory only option
#define F_SUMMARY   0x2       ///< enable summary
#define F_VERBOSE   0x4       ///< turn on verbose mode

/// @brief struct holding the summary
struct summary {
  unsigned int dirs;          ///< number of directories encountered
  unsigned int files;         ///< number of files
  unsigned int links;         ///< number of links
  unsigned int fifos;         ///< number of pipes
  unsigned int socks;         ///< number of sockets

  unsigned long long size;    ///< total size (in bytes)
};


/// @brief abort the program with EXIT_FAILURE and an optional error message
///
/// @param msg optional error message or NULL
void panic(const char *msg)
{
  if (msg) fprintf(stderr, "%s\n", msg);
  exit(EXIT_FAILURE);
}


/// @brief read next directory entry from open directory 'dir'. Ignores '.' and '..' entries
///
/// @param dir open DIR* stream
/// @retval entry on success
/// @retval NULL on error or if there are no more entries
struct dirent *getNext(DIR *dir)
{
  struct dirent *next;
  int ignore;

  do {
    errno = 0;
    next = readdir(dir);
    if (errno != 0) perror(NULL);
    ignore = next && ((strcmp(next->d_name, ".") == 0) || (strcmp(next->d_name, "..") == 0));
  } while (next && ignore);

  return next;
}


/// @brief qsort comparator to sort directory entries. Sorted by name, directories first.
///
/// @param a pointer to first entry
/// @param b pointer to second entry
/// @retval -1 if a<b
/// @retval 0  if a==b
/// @retval 1  if a>b
static int dirent_compare(const void *a, const void *b)
{
  struct dirent *e1 = (struct dirent*)a;
  struct dirent *e2 = (struct dirent*)b;

  // if one of the entries is a directory, it comes first
  if (e1->d_type != e2->d_type) {
    if (e1->d_type == DT_DIR) return -1;
    if (e2->d_type == DT_DIR) return 1;
  }

  // otherwise sorty by name
  return strcmp(e1->d_name, e2->d_name);
}

char get_file_type(mode_t mode)
{
  switch (mode & S_IFMT) {
    case S_IFBLK:
      return 'b';
    case S_IFCHR:
      return 'c';
    case S_IFDIR:
      return 'd';
    case S_IFIFO:
      return 'f';
    case S_IFLNK:
      return 'l';
    case S_IFREG:
      return ' ';
    case S_IFSOCK:
      return 's';
  }
  return ' ';
}

void get_file_perm(mode_t mode, char *str)
{
  str[0] = (mode & S_IRUSR) ? 'r' : '-';
  str[1] = (mode & S_IWUSR) ? 'w' : '-';
  str[2] = (mode & S_IXUSR) ? 'x' : '-';

  str[3] = (mode & S_IRGRP) ? 'r' : '-';
  str[4] = (mode & S_IWGRP) ? 'w' : '-';
  str[5] = (mode & S_IXGRP) ? 'x' : '-';

  str[6] = (mode & S_IROTH) ? 'r' : '-';
  str[7] = (mode & S_IWOTH) ? 'w' : '-';
  str[8] = (mode & S_IXOTH) ? 'x' : '-';

  str[9] = '\0';
}

void update_summary(unsigned char dtype, struct summary *stats)
{
  switch (dtype)
  {
    case DT_DIR:
      ++(stats->dirs);
      break;
    case DT_REG:
      ++(stats->files);
      break;
    case DT_LNK:
      ++(stats->links);
      break;
    case DT_FIFO:
      ++(stats->fifos);
      break;
    case DT_SOCK:
      ++(stats->socks);
      break;
  }
}

/// @brief recursively process directory @a dn and print its tree
///
/// @param dn absolute or relative path string
/// @param depth depth in directory tree
/// @param stats pointer to statistics
/// @param flags output control flags (F_*)
void processDir(const char *dn, unsigned int depth, struct summary *stats, unsigned int flags)
{
  // TODO
  DIR *d = opendir(dn);
  struct dirent *dir;       // temp dirent pointer for getNext()
  struct dirent *dirs;      // dynamically allocated dirents array
  char *full_path;          // file's full path to call lstat()
  struct stat stat_buffer;  // stat buffer to call lstat()

  if (d)
  {
    int dir_cnt = 0;
    while ((dir = getNext(d)))
      ++dir_cnt;
    dirs = calloc(dir_cnt, sizeof(struct dirent));
    if (!dirs)
      panic("Out of memory.");

    rewinddir(d);
    int i = 0;
    while ((dir = getNext(d)))
      dirs[i++] = *dir;

    qsort(dirs, dir_cnt, sizeof(struct dirent), dirent_compare);

    int dir_i = 0;
    while (dir_i < dir_cnt)
    {
      if (asprintf(&full_path, "%s/%s", dn, dirs[dir_i].d_name) < 0)
        panic("Out of memory.");

      if ((flags & F_DIRONLY && dirs[dir_i].d_type == DT_DIR) || !(flags & F_DIRONLY))
      {
        // stat update per file
        update_summary(dirs[dir_i].d_type, stats);
        // print directory with details
        if (flags & F_VERBOSE)
        {
          size_t filename_len = strlen(dirs[dir_i].d_name);

          // for padding
          if (2 * depth >= 54)
            printf("%51s...", "");
          else
          {
            int available_space = 54 - 2 * depth;
            printf("%*s", 2 * depth, "");
            if (filename_len > available_space)
              printf("%-.*s...", available_space - 3, dirs[dir_i].d_name);
            else
              printf("%-*s", available_space, dirs[dir_i].d_name);
          }

          if (lstat(full_path, &stat_buffer) < 0) //fail
          {
            printf("  %s\n", strerror(errno));
          }
          else  //success
          {
            struct passwd *pw = getpwuid(stat_buffer.st_uid);
            struct group *gr = getgrgid(stat_buffer.st_gid);
            char perm_str[10];
            get_file_perm(stat_buffer.st_mode, perm_str);
            char file_type = get_file_type(stat_buffer.st_mode);
            stats->size += stat_buffer.st_size;

            printf("  %8s:%-8s  %10ld %9s  %c\n", pw->pw_name, \
                                              gr->gr_name, \
                                              stat_buffer.st_size, \
                                              perm_str, \
                                              file_type);
          }
        }
        // print directory without details
        else
        {
          // for padding
          printf("%*s", 2 * depth, "");
          printf("%s\n", dirs[dir_i].d_name);
        }
        if (dirs[dir_i].d_type == DT_DIR)
          processDir(full_path, depth + 1, stats, flags);
        free(full_path);
      }
      dir_i++;
    }
    closedir(d);
  }
  else
  {
    // for padding
    printf("%*s", 2 * depth, "");
    printf("ERROR: %s\n", strerror(errno));
  }
}


/// @brief print program syntax and an optional error message. Aborts the program with EXIT_FAILURE
///
/// @param argv0 command line argument 0 (executable)
/// @param error optional error (format) string (printf format) or NULL
/// @param ... parameter to the error format string
void syntax(const char *argv0, const char *error, ...)
{
  if (error) {
    va_list ap;

    va_start(ap, error);
    vfprintf(stderr, error, ap);
    va_end(ap);

    printf("\n\n");
  }

  assert(argv0 != NULL);

  fprintf(stderr, "Usage %s [-d] [-s] [-v] [-h] [path...]\n"
                  "Gather information about directory trees. If no path is given, the current directory\n"
                  "is analyzed.\n"
                  "\n"
                  "Options:\n"
                  " -d        print directories only\n"
                  " -s        print summary of directories (total number of files, total file size, etc)\n"
                  " -v        print detailed information for each file. Turns on tree view.\n"
                  " -h        print this help\n"
                  " path...   list of space-separated paths (max %d). Default is the current directory.\n",
                  basename(argv0), MAX_DIR);

  exit(EXIT_FAILURE);
}


/// @brief program entry point
int main(int argc, char *argv[])
{
  //
  // default directory is the current directory (".")
  //
  const char CURDIR[] = ".";
  const char *directories[MAX_DIR];
  int   ndir = 0;

  struct summary tstat;
  unsigned int flags = 0;

  //
  // parse arguments
  //
  for (int i = 1; i < argc; i++) {
    if (argv[i][0] == '-') {
      // format: "-<flag>"
      if      (!strcmp(argv[i], "-d")) flags |= F_DIRONLY;
      else if (!strcmp(argv[i], "-s")) flags |= F_SUMMARY;
      else if (!strcmp(argv[i], "-v")) flags |= F_VERBOSE;
      else if (!strcmp(argv[i], "-h")) syntax(argv[0], NULL);
      else syntax(argv[0], "Unrecognized option '%s'.", argv[i]);
    } else {
      // anything else is recognized as a directory
      if (ndir < MAX_DIR) {
        directories[ndir++] = argv[i];
      } else {
        printf("Warning: maximum number of directories exceeded, ignoring '%s'.\n", argv[i]);
      }
    }
  }

  // if no directory was specified, use the current directory
  if (ndir == 0) directories[ndir++] = CURDIR;


  //
  // process each directory
  //
  // TODO
  //
  // Pseudo-code
  // - reset statistics (tstat)
  // - loop over all entries in 'directories' (number of entires stored in 'ndir')
  //   - reset statistics (dstat)
  //   - if F_SUMMARY flag set: print header
  //   - print directory name
  //   - call processDir() for the directory
  //   - if F_SUMMARY flag set: print summary & update statistics
  memset(&tstat, 0, sizeof(tstat));
  //...

  for (int i = 0; i < ndir; i++)
  {
    struct summary dstat;
    memset(&dstat, 0, sizeof(dstat));
    if (flags & F_SUMMARY)
    {
      if (flags & F_VERBOSE)
        printf("%-54s  %8s:%-8s  %10s %9s %s\n", "Name", "User", "Group", "Size", "Perms", "Type");
      else
        printf("Name\n");
      printf("----------------------------------------------------------------------------------------------------\n");
    }
    printf("%s\n", directories[i]);
    processDir(directories[i], 1, &dstat, flags);
    if (flags & F_SUMMARY)
    {
      char *summary_str;
      printf("----------------------------------------------------------------------------------------------------\n");
      if (flags & F_DIRONLY)
      {
        if (asprintf(&summary_str, "%d %s", dstat.dirs, dstat.dirs == 1 ? "directory" : "directories") < 0)
          panic("Out of memory.");
        printf("%-.68s\n", summary_str);
      }
      else
      {
        if (asprintf(&summary_str, "%d %s, %d %s, %d %s, %d %s, and %d %s",\
                  dstat.files, dstat.files == 1 ? "file" : "files", \
                  dstat.dirs, dstat.dirs == 1 ? "directory" : "directories", \
                  dstat.links, dstat.links == 1 ? "link" : "links", \
                  dstat.fifos, dstat.fifos == 1 ? "pipe" : "pipes", \
                  dstat.socks, dstat.socks == 1 ? "socket" : "sockets") < 0)
          panic("Out of memory.");
        if (flags & F_VERBOSE)
          printf("%-68.68s   %14lld\n", summary_str, dstat.size);
        else
          printf("%-.68s\n", summary_str);
      }
      free(summary_str);
      printf("\n");
    }

    // update total stats
    tstat.files += dstat.files;
    tstat.dirs += dstat.dirs;
    tstat.links += dstat.links;
    tstat.fifos += dstat.fifos;
    tstat.socks += dstat.socks;
    tstat.size += dstat.size;
  }

  //
  // print grand total
  //
  if ((flags & F_SUMMARY) && (ndir > 1)) {
    if (flags & F_DIRONLY)
    {
      printf("Analyzed %d directories:\n"
            "  total # of directories:  %16d\n",
            ndir, tstat.dirs);
    }
    else
    {
      printf("Analyzed %d directories:\n"
            "  total # of files:        %16d\n"
            "  total # of directories:  %16d\n"
            "  total # of links:        %16d\n"
            "  total # of pipes:        %16d\n"
            "  total # of sockets:      %16d\n",
            ndir, tstat.files, tstat.dirs, tstat.links, tstat.fifos, tstat.socks);

      if (flags & F_VERBOSE) {
        printf("  total file size:         %16llu\n", tstat.size);
      }
    }

  }

  //
  // that's all, folks!
  //
  return EXIT_SUCCESS;
}
