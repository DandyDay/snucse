mod expr;
mod linkedlist;
mod value;
mod eval;
mod eval_memo;

use expr::*;
use value::*;

fn main() {
    // 1. ENum: 숫자 평가
    let expr = Expr::enumb(10);
    assert_eq!(expr.eval(), Val::VNum(10));

    // 2. EAdd: 덧셈 평가
    let expr = Expr::eadd(Expr::enumb(5), Expr::enumb(3));
    assert_eq!(expr.eval(), Val::VNum(8));

    // 3. ESub: 뺄셈 평가
    let expr = Expr::esub(Expr::enumb(10), Expr::enumb(4));
    assert_eq!(expr.eval(), Val::VNum(6));

    // 4. EMul: 곱셈 평가
    let expr = Expr::emul(Expr::enumb(2), Expr::enumb(6));
    assert_eq!(expr.eval(), Val::VNum(12));

    // 5. EDiv: 나눗셈 평가
    let expr = Expr::ediv(Expr::enumb(15), Expr::enumb(3));
    assert_eq!(expr.eval(), Val::VNum(5));

    // 6. EIf0: 조건문 (참) 평가
    let expr = Expr::eif0(Expr::enumb(0), Expr::enumb(1), Expr::enumb(2));
    assert_eq!(expr.eval(), Val::VNum(1));

    // 7. EIf0: 조건문 (거짓) 평가
    let expr = Expr::eif0(Expr::enumb(5), Expr::enumb(1), Expr::enumb(2));
    assert_eq!(expr.eval(), Val::VNum(2));

    // 8. EName: 변수 평가 (간단한 변수)
    let expr = Expr::eletval("x", Expr::enumb(7), Expr::ename("x"));
    assert_eq!(expr.eval(), Val::VNum(7));

    // 9. EApp: 함수 적용 (간단한 람다 함수)
    let expr = Expr::eapp(Expr::elam("x", Expr::eadd(Expr::ename("x"), Expr::enumb(1))), Expr::enumb(4));
    assert_eq!(expr.eval(), Val::VNum(5));

    // 10. ELetVal: let 바인딩 평가
    let expr = Expr::eletval("y", Expr::enumb(3), Expr::eadd(Expr::ename("y"), Expr::enumb(2)));
    assert_eq!(expr.eval(), Val::VNum(5));

    // 11. ELetFuns: 재귀 함수 평가 (팩토리얼)
    let expr = Expr::eletrec(
        vec![Expr::efun(
            "fact",
            "n",
            Expr::eif0(
                Expr::ename("n"),
                Expr::enumb(1),
                Expr::emul(Expr::ename("n"), Expr::eapp(Expr::ename("fact"), Expr::esub(Expr::ename("n"), Expr::enumb(1)))),
            ),
        )],
        Expr::eapp(Expr::ename("fact"), Expr::enumb(5)),
    );
    assert_eq!(expr.eval(), Val::VNum(120));

    // 12. 복합 표현식 1: 덧셈과 곱셈
    let expr = Expr::eadd(Expr::emul(Expr::enumb(2), Expr::enumb(3)), Expr::enumb(4));
    assert_eq!(expr.eval(), Val::VNum(10));

    // 13. 복합 표현식 2: 조건문과 let 바인딩
    let expr = Expr::eif0(
        Expr::esub(Expr::enumb(5), Expr::enumb(2)),
        Expr::eletval("z", Expr::enumb(10), Expr::ename("z")),
        Expr::enumb(8),
    );
    assert_eq!(expr.eval(), Val::VNum(8));

    // 14. 복합 표현식 3: 함수 적용과 뺄셈
    let expr = Expr::esub(
        Expr::eapp(Expr::elam("a", Expr::eadd(Expr::ename("a"), Expr::enumb(3))), Expr::enumb(7)),
        Expr::enumb(2),
    );
    assert_eq!(expr.eval(), Val::VNum(8));

    // 15. 복합 표현식 4: 재귀 함수와 let 바인딩
    let expr = Expr::eletrec(
        vec![Expr::efun(
            "fib",
            "n",
            Expr::eif0(
                Expr::ename("n"),
                Expr::enumb(0),
                Expr::eif0(
                    Expr::esub(Expr::ename("n"), Expr::enumb(1)),
                    Expr::enumb(1),
                    Expr::eadd(
                        Expr::eapp(Expr::ename("fib"), Expr::esub(Expr::ename("n"), Expr::enumb(1))),
                        Expr::eapp(Expr::ename("fib"), Expr::esub(Expr::ename("n"), Expr::enumb(2))),
                    ),
                ),
            ),
        )],
        Expr::eletval("x", Expr::enumb(10), Expr::eapp(Expr::ename("fib"), Expr::ename("x"))),
    );
    assert_eq!(expr.eval(), Val::VNum(55));
}
