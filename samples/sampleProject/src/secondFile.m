
[extern]
[cname: "printf"]
fn print (text String)

[extern]
[cname: "puts"]
fn println (text String)

[inline]
fn getName String -> ask("What's your name?")

[inline]
fn ask (question String) String {
    println(question)
    return "nonotext"
}