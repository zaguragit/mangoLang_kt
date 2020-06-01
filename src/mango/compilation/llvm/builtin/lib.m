

[extern]
[private]
fn getchar Int

[extern]
[private]
fn printf (text String)

[extern]
[private]
fn puts (text String)



[extern]
fn readln String



[inline]
fn read Int -> getchar()

[inline]
fn print (text String) -> printf(text)

[inline]
fn println (text String) -> puts(text)