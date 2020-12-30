use primitives*
use string*

[extern]
[cname: "putchar"]
val print (char I16)

val print (string String) -> {
	var i = 0
	loop {
	    i >= string.length ? break
		print(string[i])
		i += 1
	}

    [extern]
    [cname: "flushPrint"]
    val flushPrint ()

	flushPrint()
}

[inline]
val print (i Int) -> print(i.toString())

[inline]
val print (i Bool) -> print(i.toString())



[inline]
val println -> print('\n')

[inline]
val println (text String) -> {
    print(text)
    println()
}

[inline]
val println (char I16) -> {
    print(char)
    println()
}

[inline]
val println (i Int) -> println(i.toString())

[inline]
val println (i Bool) -> println(i.toString())



[extern]
[cname: "getchar"]
val readChar () I16

val readln String -> {
    use text.builder*

	val builder = StringBuilder {
	    length: 0
	    chars: Ptr<I16> { length: 512 }
	    capacity: 512
	}
    var ch = readChar()
    loop {
        ch == '\n' || ch == '\0' ? break
        builder.appendChar(ch)
        ch = readChar()
    }
    builder.toString()
}