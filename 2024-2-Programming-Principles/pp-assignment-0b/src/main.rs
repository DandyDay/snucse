use std::env;
use pp_assignment_0b::*;
fn main() {
    let args: Vec<String> = env::args().collect();
    let (x1, x2) = (&args[1], &args[2]);
    println!("The answer is {:?}.", add(x1.parse().unwrap(),x2.parse().unwrap()));
}