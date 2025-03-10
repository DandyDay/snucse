# Principles of Programming Assignment 0

## !!!IMPORTANT!!!: You cannot attend the midterm exam if you do not complete this assignment!

## How to get the code

1. Assign your SSH key from **User Settings > SSH Keys**. If you do not have any SSH key, create a new one.
   - Windows: https://haejun0317.tistory.com/271
   - Linux/MacOS: https://developerbee.tistory.com/241
2. Replace `<student_id>` to your student id from the below script, and run it
   - Pay attention to the port number (7001). We will not use the standard SSH port (22), and also the port number is different from the one you see in the website.

`git clone ssh://git@gl.kinetc.net:7001/<student_id>_pp2402_group/pp-assignment-0`

e.g.) If your student id is `2000-10000`, run below.

`git clone ssh://git@gl.kinetc.net:7001/2000-10000_pp2402_group/pp-assignment-0`

For the next assignments and exams, you must replace the address after `7001`.
That part of address is written in the website address of this page.

## Goal

1. Get used to basic syntax of Scala 3
2. Learn how to use Git and sbt in your console.

## Objective

1. Replace all the `???` in `src/main/scala/Main.scala`
   - `studentID`: Fill in your student id. (e.g. "2024-10000")
   - `studentName`: Fill in your name (e.g. "홍길동")
   - `laptopOS`: Write the OS of your Laptop. This is required due to the lack of usable desktops in CSE department at the current year.
   - `laptopCPU`: Write the name of CPU in your laptop (e.g. "AMD 4700G")
   - `splitDashFromID`: From the given student ID, split it and return the entrance year and the index of the student. (e.g. `splitDashFromID("2020-20202") == ("2020", "20202)`)
2. Compile it with sbt. Run below from your console.

```bash
sbt test # compile and run TestSuite.scala
```

3. Commit the code and push your answer to the GitLab. Run below from your console.

```bash
git add src
git commit -m 'Write some meaningful message'
git push
```

## How to Submit

- For all assignments, we will test your code only with the given `*.scala` files in `src/main/scala` directory. **All additional files you created will be lost!!**
- We will check git logs of the main branch, and grade the latest commit before the due date.

## Due date

- Until Friday of the previous week of the midterm exam.
- This assignment will not be graded.
- IMPORTANT: You cannot attend the midterm exam if you didn't complete this assignment!

http://gl.kinetc.net:20105/-/ide/project/2020-12852_pp2402_group/pp-assignment-0/edit/main/-/
