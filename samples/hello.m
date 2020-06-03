
[extern]
[cname: "printf"]
fn print (text String)

[extern]
[cname: "puts"]
fn println (text String)


val creatorsName = "leo"

fn main {
    val name = getName()

    if name == creatorsName {
        println("Hi, \" + name + \"!!")
    }
    else {
        println("Hi, \" + name + \"!")
    }

    println(name)
}

[inline]
fn getName String -> ask("What's your name?")

[inline]
fn ask (question String) String {
    println(question)
    return readln()
}