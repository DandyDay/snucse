# Principles of Programming Assignment 0B

## Objective

Implement the functions in `operation/lib.rs`, which are currently left blank as `todo!()`

**This assignment is not graded - it is only for practice before project and Final exam.**

Timeout: 30 sec.

## How to test your code

```bash
cargo test # compile and run tests in lib.rs
```

If `cargo test` does not work, then your code is wrong.

Fix your code until it works, or send a question to the TA.

You can also run `main.rs` by the following command.
```bash
cargo run 42 43 # compile and run main.rs with argument 42 and 43
```

## How to use formatter and linter

```bash
cargo fmt # formatting the code automatically
```

```bash
cargo clippy # printing problems of the code and how to fix them
```

## How to Submit

- For all assignments, we will test your code only with the given `lib.rs` files in `src` directory. **All the additional files you've created will be lost!!**
- We will check git logs of the main branch, and grade the latest commit before the due date.

```bash
git add src
git commit -m 'Write some meaningful message'
git push
```

## Due date

- None

## Errata

- Branch `errata` is protected. If any correction would be appeared on the code, I will update `errata` branch.
- If you find any ambiguity from the description, write an issue to the GitHub issue tracker.
  - https://github.com/snu-sf-class/pp202302/issues
