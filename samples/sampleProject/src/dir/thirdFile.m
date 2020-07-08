use std.io*

[inline]
fn ask (question String) String {
    println(question)
    return readln()
}