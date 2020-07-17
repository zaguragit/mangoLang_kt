use std.string

[extern]
[cname: "print"]
fn print (text String)

[inline]
fn print (i Int) {
    print(string.intToString(i))
}

[extern]
[cname: "println"]
fn println (text String)

[inline]
fn println (i Int) {
    println(string.intToString(i))
}

[extern]
[cname: "readln"]
fn readln String
