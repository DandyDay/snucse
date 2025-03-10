//! DO NOT CHANGE THIS FILE.

use crate::expr::*;
use crate::linkedlist::*;
use std::cell::RefCell;
use std::collections::HashMap;
use std::rc::Rc;
use std::sync::atomic::{AtomicI64, Ordering};

// Used in eval.rs: Version without memoization.

/// Values: either a number (i64) or a closure (a function definition along with its environment).
#[derive(Debug, PartialEq, Clone)]
pub enum Val<'a> {
    VNum(i64),
    VClo(&'a FunDef, Rc<Env<'a>>),
}

/// Formatting utility for displaying the Val type
impl std::fmt::Display for Val<'_> {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Val::VNum(n) => write!(f, "{}", n),
            Val::VClo(fdef, _) => write!(f, "efun ({}) {}", fdef.0, fdef.1),
        }
    }
}

/// Entries in the Env
#[derive(Debug, PartialEq, Clone)]
pub enum EnvEntry<'a> {
    EEVal(&'static str, Val<'a>),
    EEFuns(List<(&'static str, &'a FunDef)>),
}

/// Environment
#[derive(PartialEq, Debug)]
pub struct Env<'a> {
    pub parent: Option<Rc<Env<'a>>>,
    pub entry: EnvEntry<'a>,
    /// an ID only for debugging and memory leak checking.
    id: i64,
}
impl<'a> Env<'a> {
    pub fn new(parent: Option<Rc<Env<'a>>>, entry: EnvEntry<'a>) -> Env<'a> {
        let id = ENV_NUM.fetch_add(1, Ordering::SeqCst);
        Env { id, parent, entry }
    }
}
impl Drop for Env<'_> {
    fn drop(&mut self) {
        DROP_NUM.fetch_add(1, Ordering::SeqCst);
        self.parent = None
    }
}

// static(global) values for eval.rs, only for memory leak checks.
// You cannot change these values explicitly.

/// The number of created enviroments
static ENV_NUM: AtomicI64 = AtomicI64::new(0);
/// The number of dropped enviroments
static DROP_NUM: AtomicI64 = AtomicI64::new(0);

// Used in eval_memo.rs: Version with memoization
pub type Memo = HashMap<i64, i64>;

#[derive(Debug, PartialEq, Clone)]
pub enum ValM<'a> {
    VNum(i64),
    VClo(&'a FunDef, Rc<EnvM<'a>>, Option<Rc<RefCell<Memo>>>),
}
impl std::fmt::Display for ValM<'_> {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ValM::VNum(n) => write!(f, "{}", n),
            ValM::VClo(fdef, _, _) => write!(f, "efun ({}) {}", fdef.0, fdef.1),
        }
    }
}

#[allow(clippy::type_complexity)]
#[derive(Debug, PartialEq, Clone)]
pub enum EnvEntryM<'a> {
    EEVal(&'static str, ValM<'a>),
    EEFuns(List<(&'static str, &'a FunDef, Option<Rc<RefCell<Memo>>>)>),
}

#[derive(PartialEq, Debug)]
pub struct EnvM<'a> {
    pub parent: Option<Rc<EnvM<'a>>>,
    pub entry: EnvEntryM<'a>,
    // an ID only for debugging.
    id: i64,
}
impl<'a> EnvM<'a> {
    pub fn new(parent: Option<Rc<EnvM<'a>>>, entry: EnvEntryM<'a>) -> EnvM<'a> {
        let id = ENV_NUM2.fetch_add(1, Ordering::SeqCst);
        EnvM { id, parent, entry }
    }
}
impl Drop for EnvM<'_> {
    fn drop(&mut self) {
        DROP_NUM2.fetch_add(1, Ordering::SeqCst);
        self.parent = None
    }
}

// static(global) values for eval_memo.rs, only for memory leak checks.
// You cannot change these values explicitly.

/// The number of created enviroments
static ENV_NUM2: AtomicI64 = AtomicI64::new(0);
/// The number of dropped enviroments
static DROP_NUM2: AtomicI64 = AtomicI64::new(0);

// Testing functions

pub fn test_eval(e: Expr, n: i64) {
    println!("{}", e);
    let r = e.eval();
    println!("{}", r);
    assert_eq!(r, Val::VNum(n));
}
pub fn test_eval_leak_check(e: Expr, n: i64) {
    ENV_NUM.store(0, Ordering::SeqCst);
    DROP_NUM.store(0, Ordering::SeqCst);
    test_eval(e, n);
    // println!("{}, {}", ENV_NUM.load(Ordering::SeqCst), DROP_NUM.load(Ordering::SeqCst));
    assert_eq!(
        ENV_NUM.load(Ordering::SeqCst),
        DROP_NUM.load(Ordering::SeqCst)
    );
}

pub fn test_eval_memo(e: Expr, n: i64) {
    println!("{}", e);
    let r = e.eval_memo();
    println!("{}", r);
    assert_eq!(r, ValM::VNum(n));
}
pub fn test_eval_memo_leak_check(e: Expr, n: i64) {
    ENV_NUM2.store(0, Ordering::SeqCst);
    DROP_NUM2.store(0, Ordering::SeqCst);
    test_eval_memo(e, n);
    // println!("{}, {}", ENV_NUM.load(Ordering::SeqCst), DROP_NUM.load(Ordering::SeqCst));
    assert_eq!(
        ENV_NUM2.load(Ordering::SeqCst),
        DROP_NUM2.load(Ordering::SeqCst)
    );
}
