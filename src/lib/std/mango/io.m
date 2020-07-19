use std.string*

[extern]
[cname: "print"]
fn print (text String)

[inline]
fn print (i Int) -> print(i.toString())

[inline]
fn print (i Bool) -> print(i.toString())


[extern]
[cname: "println"]
fn println (text String)

[inline]
fn println (i Int) -> println(i.toString())

[inline]
fn println (i Bool) -> println(i.toString())


[extern]
[cname: "readln"]
fn readln String
