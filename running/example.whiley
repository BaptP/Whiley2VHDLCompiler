
type Hello is {int k}
type Bonjour is {byte e, bool u, Hello h}

function foo(int x) -> (int a)
ensures a == 5*x:
    int r
    r = bar(x,x)
    Bonjour t = A({k : x})
    int u
    u,r,x = swap(t.h.k,r-3),x+2
    return u+r+x-2

function bar(int x, int y) -> int :
    int a = x + 2
    int b = a-4
    return y + b + a
    
function swap(int t, int r) -> (int a, int c) :
    return r,t


function A(Hello h) -> Bonjour :
    Bonjour s = {e : 00b, u : false, h : h}
    s.h.k = s.h.k + 3
    return s
