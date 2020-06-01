
val creatorsName = "leo"

fn main {
    val name = getName()

    if name == creatorsName {
        println("Hi, \" + name + \"!!")
    } else {
        println("Hi, \" + name + \"!")
    }

    println(name)
}

fn getName String -> ask("What's your name?")

fn ask (question String) String {
    println(question)
    return readln()
}