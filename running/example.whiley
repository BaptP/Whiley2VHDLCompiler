//type T is int|byte
//type U is int|bool
//type V is T|U

function e(int c) -> int|bool:
    int|bool a = c
    if c == 0:
      a = false
    return a


type Test is {
    int|bool x, 
    byte y
}
type Test2 is {
    int x, 
    {int a,bool b} z
}

function inc2(Test|Test2 t) -> Test|Test2:
    if t is Test2:
        t.x = t.x + 1
    return t
/*
function ip(int a, int b) -> (int x, int y):
    int s = a+b
    int d = a-b
    return s,d*/
    
/*
type T1 is {int a, byte c}
type T2 is {int|bool a}


function f(T1|T2 t) -> int:
    int|bool a = t.a
    if a is int:
        return a
    return 0
    
function main(bool b) -> bool|byte|int:
    return f({a:b})*/