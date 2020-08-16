use std.string*



[extern]
[cname: "print"]
fn print (text String)

[inline]
fn print (i Int) -> print(i.toString())

[inline]
fn print (i Bool) -> print(i.toString())



[inline]
fn println -> print("\n")

[inline]
fn println (text String) {
    print(text)
    println()
}

[inline]
fn println (i Int) -> println(i.toString())

[inline]
fn println (i Bool) -> println(i.toString())



[extern]
[cname: "readln"]
fn readln String


/*

[extern]
[cname: "getchar"]
fn readChar I8

[extern]
[cname: "putchar"]
fn print (char I8)

fn readln String {
	val string = new MutableString {
	    length: 0
	    chars: new I8[512]
	}
    var ch = readChar()
    while ch != '\n' && ch != '\0' && string.length < 512 {
        string[string.length++] = ch
        ch = readChar()
    }
    return string
}

fn print (string String) {
	val i = 0;
	while i < string.length {
		print(string.chars[i++])
	}

    [extern]
    [cname: "flushPrint"]
    fn flushPrint

	flushPrint()
}

*/