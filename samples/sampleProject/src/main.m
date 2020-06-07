use secondFile*
use thirdFile*

val creatorsName = "leo"

[entry]
fn test {
    val name = getName()

    if name == creatorsName {
        println("Hi, \" + name + \"!!")
    }
    else {
        println("Hi, \" + name + \"!")
    }

    println(name)
}