use dir.thirdFile*

[inline]
fn getName String -> ask("What's your name?")



// uses functions from the c standard library

[extern]
[cname: "printf"]
fn print (text String)

[extern]
[cname: "puts"]
fn println (text String)