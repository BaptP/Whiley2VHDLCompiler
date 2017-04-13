

type Test is {int a, int b}

function foo(Test t, int e) -> (Test f, Test g):
    int x
    x = t.a+t.b-e
    int y
    y = 3*x
    skip
    Test t2 = {a : x, b : y}
    y = 42 + t.a
    y = y +t2.a
    if (y == x):
        y = 5
        int x = 2
        int p = 4
    t = t2
    t.a = t.b+t.a+y
    skip
    t2.b = t.a+t2.b
    return t,t2
    
/*
type Hello is {int k}
type Bonjour is {byte e, bool u, Hello h}

function foo2(int x) -> (int a)
ensures a == 5*x:
    int r
    r = bar(x,x)
    Bonjour t = A({k : x}, 0)
    skip
    int u
    u,r,x = swap(t.h.k,r-3),x+2
    return u+r+x-2
function bar(int x, int y) -> int :
    int a = x + 2
    int b = a-4
    return y + b + a
    
function swap(int t, int r) -> (int a, int c) :
    return r,t


function A(Hello h, int l) -> Bonjour :
    Bonjour s = {e : 00b, u : false, h : h}
    s.h.k = s.h.k + l
    return s
*/