
function s(int a, int b) -> int:
  int c = 0
  while a > 0:
    a = a - 1
    c = c + a
  return c+b
  
function t(int a, int b) -> int:
  int c = a+b
  if a==b:
    c = a+b
    skip
    c = c + 1
  else:
    c = a-b
  int d
  d = c * a
  skip
  c = c*2
  int e = 0
  while a > 0:
    a = a - 1
    int f = a - d
    skip
    if f == 0:
      c = c + f
    else:
      e = a
  b = b-d
  return c+b
  
type Int is {int x, bool b}

function s2(Int a, int b) -> int:
  int c = 0
  while a.x > 0:
    a.x = a.x - 1
    c = c + a.x
  if a.b:
    return c+b
  return a.x











//type T is int|byte
//type U is int|bool
//type V is T|U
/*
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
    return t*/
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