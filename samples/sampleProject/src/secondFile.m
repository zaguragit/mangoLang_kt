use dir.thirdFile*

[inline]
fn getName String -> ask("What's your name?")



// uses functions from the c standard library

[extern]
[cname: "print"]
fn print (text String)

[extern]
[cname: "println"]
fn println (text String)