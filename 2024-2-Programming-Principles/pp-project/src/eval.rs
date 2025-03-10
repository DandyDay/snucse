use crate::expr::*;
use crate::linkedlist::*;
use crate::value::*;
use std::rc::Rc;
use EnvEntry::*;
use Expr::*;
use Val::*;

impl Expr {
    fn eval_env<'a, 'b>(&'a self, env_rc: &'b Rc<Env<'a>>) -> Val<'a> {

        fn eval_arith<'a, 'b>(
            op: fn(i64, i64) -> i64,
            e1: &'a Expr,
            e2: &'a Expr,
            env_rc: &'b Rc<Env<'a>>,
        ) -> Val<'a> {
            if let (VNum(n1), VNum(n2)) = (e1.eval_env(env_rc), e2.eval_env(env_rc))
            {
                VNum(op(n1, n2))
            }
            else
            {
                panic!("Both operands should be numbers")
            }
        }

        fn eval_if<'a, 'b>(
            e_cond: &'a Expr,
            e_then: &'a Expr,
            e_else: &'a Expr,
            env_rc: &'b Rc<Env<'a>>,
        ) -> Val<'a> {
            if e_cond.eval_env(env_rc) == VNum(0)
            {
                e_then.eval_env(env_rc)
            }
            else
            {
                e_else.eval_env(env_rc)
            }
        }

        fn eval_name<'a, 'b>(x: &'static str, env_rc: &'b Rc<Env<'a>>) -> Val<'a> {
            fn find_name<'a, 'b>(x: &'static str, env_rc: &'b Rc<Env<'a>>) -> Val<'a> {
                match &env_rc.entry {
                    EEVal(y, v) if *y == x => v.clone(),
                    EEFuns(fs) => {
                        if let Some((_, fdef)) = fs.iter().find(|(fname, _)| *fname == x) {
                            VClo(fdef, env_rc.clone())
                        } else {
                            match &env_rc.parent {
                                Some(p) => find_name(x, &p.clone()), // 재귀 호출 시 clone() 사용
                                None => panic!("Unbound name: {}", x),
                            }
                        }
                    },
                    _ => match &env_rc.parent {
                        Some(p) => find_name(x, &p.clone()), // 재귀 호출 시 clone() 사용
                        None => panic!("Unbound name: {}", x),
                    },
                }
            }
            find_name(x, &env_rc.clone())
        }

        fn eval_app<'a, 'b>(f: &'a Expr, arg: &'a Expr, env_rc: &'b Rc<Env<'a>>) -> Val<'a> {
            match f.eval_env(env_rc) {
                VClo((param, body), clo_env) => {
                    let v = arg.eval_env(env_rc);
                    let new_env = Rc::new(Env::new(Some(clo_env), EEVal(param, v)));
                    body.eval_env(&new_env)
                }
                _ => panic!("Application to a non-function"),
            }
        }

        fn eval_letval<'a, 'b>(
            x: &'static str,
            e1: &'a Expr,
            e2: &'a Expr,
            env_rc: &'b Rc<Env<'a>>,
        ) -> Val<'a> {
            let v = e1.eval_env(env_rc);
            let new_env = Rc::new(Env::new(Some(env_rc.clone()), EEVal(x, v)));
            e2.eval_env(&new_env)
        }

        fn eval_letfuns<'a, 'b>(
            binds: &'a List<Bind>,
            e: &'a Expr,
            env_rc: &'b Rc<Env<'a>>,
        ) -> Val<'a> {
            let mut funs = List::new();
            for &(_, fname, ref fdef) in binds.iter() {
                funs = List::cons((fname, fdef), funs); // cons를 사용하여 앞에 추가
            }
            let new_env = Rc::new(Env::new(Some(env_rc.clone()), EEFuns(funs)));
            e.eval_env(&new_env)
        }

        match self {
            ENum(n) => VNum(*n),
            EAdd(e1, e2) => eval_arith(|x, y| x + y, e1, e2, env_rc),
            ESub(e1, e2) => eval_arith(|x, y| x - y, e1, e2, env_rc),
            EMul(e1, e2) => eval_arith(|x, y| x * y, e1, e2, env_rc),
            EDiv(e1, e2) => eval_arith(|x, y| x / y, e1, e2, env_rc),
            EIf0(e_cond, e_then, e_else) => eval_if(e_cond, e_then, e_else, env_rc),
            EName(x) => eval_name(x, env_rc),
            EApp(f, arg) => eval_app(f, arg, env_rc),
            ELetVal(x, e1, e2) => eval_letval(x, e1, e2, env_rc),
            ELetFuns(binds, e) => eval_letfuns(binds, e, env_rc),
        }
    }

    pub fn eval(&self) -> Val<'_> {
        self.eval_env(&Rc::new(Env::new(None, EEFuns(List::new()))))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn tests_memo_leak_check() {
        // ((3 + 5) / 2) = 4
        let expr1 = Expr::ediv(Expr::eadd(Expr::enumb(3), Expr::enumb(5)), Expr::enumb(2));
        test_eval_leak_check(expr1, 4);

        // letfun memo:(☆ x := (x - 1)) in
        // ☆(5)
        // = 4
        let expr2 = Expr::eapp(
            Expr::elam_memo("x", Expr::esub(Expr::ename("x"), Expr::enumb(1))),
            Expr::enumb(5),
        );
        test_eval_leak_check(expr2, 4);

        // letfun (f x := (x - 1)) in
        // let n := f(42) in
        // let m := (n * 2) in
        // f(m)
        // = 81
        let expr3 = Expr::eletrec(
            vec![Expr::efun(
                "f",
                "x",
                Expr::esub(Expr::ename("x"), Expr::enumb(1)),
            )],
            Expr::eletval(
                "n",
                Expr::eapp(Expr::ename("f"), Expr::enumb(42)),
                Expr::eletval(
                    "m",
                    Expr::emul(Expr::ename("n"), Expr::enumb(2)),
                    Expr::eapp(Expr::ename("f"), Expr::ename("m")),
                ),
            ),
        );
        test_eval_leak_check(expr3, 81);

        // letfun (emul x := letfun (☆ y := (x * y)) in ☆)
        //      (fact op :=
        //          letfun (☆ i0 :=
        //              letfun memo:(self x :=
        //                  if x = 0 then i0
        //                  else op(x)(self((x - 1)))
        //              ) in self
        //          ) in ☆
        //      ) in
        //      fact(emul)(1)(10)
        // = 3628800
        let expr_fact = Expr::eletrec(
            vec![
                Expr::efun(
                    "fact",
                    "op",
                    Expr::elam(
                        "i0",
                        Expr::eletrec(
                            vec![Expr::efun_memo(
                                "self",
                                "x",
                                Expr::eif0(
                                    Expr::ename("x"),
                                    Expr::ename("i0"),
                                    Expr::eapp(
                                        Expr::eapp(Expr::ename("op"), Expr::ename("x")),
                                        Expr::eapp(
                                            Expr::ename("self"),
                                            Expr::esub(Expr::ename("x"), Expr::enumb(1)),
                                        ),
                                    ),
                                ),
                            )],
                            Expr::ename("self"),
                        ),
                    ),
                ),
                Expr::efun(
                    "emul",
                    "x",
                    Expr::elam("y", Expr::emul(Expr::ename("x"), Expr::ename("y"))),
                ),
            ],
            Expr::eapp(
                Expr::eapp(
                    Expr::eapp(Expr::ename("fact"), Expr::ename("emul")),
                    Expr::enumb(1),
                ),
                Expr::enumb(10),
            ),
        );
        test_eval_leak_check(expr_fact, 3628800);

        // letfun (eadd x := letfun (☆ y := (x + y)) in ☆)
        //      memo:(fib op :=
        //          letfun (☆ i0 :=
        //              letfun (☆ i1 :=
        //                  letfun memo:(self x :=
        //                      if x = 0 then i0
        //                      else if (x - 1) = 0 then i1
        //                      else op(self((x - 1)))(self((x - 2))) )
        //                  in self
        //              ) in ☆
        //          ) in ☆
        //      ) in
        //      fib(eadd)(0)(1)(50)
        // = 12586269025
        let expr_fib = Expr::eletrec(
            vec![
                Expr::efun_memo(
                    "fib",
                    "op",
                    Expr::elam(
                        "i0",
                        Expr::elam(
                            "i1",
                            Expr::eletrec(
                                vec![Expr::efun_memo(
                                    "self",
                                    "x",
                                    Expr::eif0(
                                        Expr::ename("x"),
                                        Expr::ename("i0"),
                                        Expr::eif0(
                                            Expr::esub(Expr::ename("x"), Expr::enumb(1)),
                                            Expr::ename("i1"),
                                            Expr::eapp(
                                                Expr::eapp(
                                                    Expr::ename("op"),
                                                    Expr::eapp(
                                                        Expr::ename("self"),
                                                        Expr::esub(
                                                            Expr::ename("x"),
                                                            Expr::enumb(1),
                                                        ),
                                                    ),
                                                ),
                                                Expr::eapp(
                                                    Expr::ename("self"),
                                                    Expr::esub(Expr::ename("x"), Expr::enumb(2)),
                                                ),
                                            ),
                                        ),
                                    ),
                                )],
                                Expr::ename("self"),
                            ),
                        ),
                    ),
                ),
                Expr::efun(
                    "eadd",
                    "x",
                    Expr::elam("y", Expr::eadd(Expr::ename("x"), Expr::ename("y"))),
                ),
            ],
            Expr::eapp(
                Expr::eapp(
                    Expr::eapp(
                        Expr::eapp(Expr::ename("fib"), Expr::ename("eadd")),
                        Expr::enumb(0),
                    ),
                    Expr::enumb(1),
                ),
                Expr::enumb(10),
            ),
        );
        test_eval_leak_check(expr_fib, 55);
    }
}
