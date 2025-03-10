# Principles of Programming Assignment - Project

## Objective

Develop an interpreter for a toy language.
- Problem 1: Replace `todo!()` in `eval.rs` with appropriate Rust code to implement the evaluation function without memoization.
- Problem 2 (Bonus): Replace `todo!()` in `eval_memo.rs` with Rust code to implement the evaluation function with memoization.

## Requirements
Your interpreter must adhere to the semantics of the toy language described in `project.pdf`.

Failure to meet any of these requirements will result in **zero points** for the project:
1. **Safe Rust**:
   - Use only safe Rust. Any use of `unsafe` code will result in rejection.
2. **Library Restrictions**:
   - You may only use the following standard library data types and methods:
     - [`std::rc::Rc`](https://doc.rust-lang.org/std/rc/struct.Rc.html)
     - [`std::cell::RefCell`](https://doc.rust-lang.org/std/cell/struct.RefCell.html)
     - [`std::collections::HashMap`](https://doc.rust-lang.org/std/collections/struct.HashMap.html)
     - [`std::option::Option`](https://doc.rust-lang.org/std/option/enum.Option.html)
     - Methods from the [`std::iter::Iterator`](https://doc.rust-lang.org/std/iter/trait.Iterator.html) trait.

### Examples of Allowed Methods:
- `std::rc::Rc::{new, clone, as_ref, deref}`
- `std::cell::RefCell::{new, borrow, borrow_mut}`
- `std::collections::HashMap::{new, get, insert}`
- `std::option::Option::{is_some, is_none, map, map_or_else, unwrap, unwrap_or_else}`
- `std::iter::fold`
- [`std::panic!`](https://doc.rust-lang.org/std/macro.panic.html)


## Grading Considerations
### Memory Leaks:
- Points will be deducted for memory leaks, even if all test cases pass.
- Use `test_eval_leak_check` (or `test_eval_memo_leak_check`) to ensure that the number of created environments matches the number of dropped environments.

### Testing Without Memory Leak Checks:
- Use `test_eval` and `test_eval_memo` for testing functionality without checking for memory leaks.

## Further Notes:
- Completing **Problem 1** is sufficient to earn full credit for the project.
- Successfully completing **Problem 2** will earn extra points, which can compensate for lost marks in other assignments, the midterm, or the final exam.
- Grading will include additional test cases beyond those provided.

Timeout: 30 sec.

## How to Test Your Code
Run the following command to compile and execute the tests in `lib.rs`:
```bash
cargo test
```

If `cargo test` fails, there is an issue with your code.

You can also run `main.rs` by the following command.
```bash
cargo run
```

## How to Format and Lint Your Code
To automatically format your code:
```bash
cargo fmt
```

To analyze your code and receive suggestions for fixes:
```bash
cargo clippy
```

## Submission Guidelines
- Only the provided `eval.rs` and `eval_memo.rs` files in the `src` directory will be used for testing. **Any additional files you create will not be included.**
- Submissions will be graded based on the latest commit to the main branch befor the deadline.
  Ensure your changes are committed and pushed to the repository.

```bash
git add src
git commit -m "Add a meaningful commit message"
git push
```

## Due date
- Deadline: 2024/12/23 23:59:59 KST

## Errata and Ambiguities
- A protected `errata` branch will contain any corrections or updates. Check this branch periodically for changes.
- If you ecounter ambiguities in the assignment description, report them via the GitHub issue tracker: https://github.com/snu-sf-class/pp202402/issues


