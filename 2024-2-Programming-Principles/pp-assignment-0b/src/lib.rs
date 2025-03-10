// use crate::operation::operation;
pub mod operation;
pub use operation::*;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        let result = add(2, 2);
        assert_eq!(result, 4);
    }

    #[test]
    fn it_works2() {
        let result = add(2, 4);
        assert_eq!(result, 6);
    }
}
