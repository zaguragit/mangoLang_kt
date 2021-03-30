use std.io*
use std.text.string*

[inline]
val ask (question String) String -> {
    println(question)
    readln()
}