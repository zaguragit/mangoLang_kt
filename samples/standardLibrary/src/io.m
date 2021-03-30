use primitives*
use text.string*

[cname: "putchar"]
val print (char I16)

val print (text CharSequence) -> {
	var i = 0
	loop {
	    i >= text.length ? break
		print(text[i])
		i += 1
	}

    [cname: "flushPrint"]
    val flushPrint ()

	flushPrint()
}

[inline]
val print (i I32) -> print(i.toString())

[inline]
val print (i Bool) -> print(i.toString())



[inline]
val println () -> print('\n')

[inline]
val println (text CharSequence) -> {
    print(text)
    println()
}

[inline]
val println (char I16) -> {
    print(char)
    println()
}

[inline]
val println (i I32) -> println(i.toString())

[inline]
val println (i Bool) -> println(i.toString())


[cname: "getchar"]
val readChar () I16

val readln () String -> {
    use text.builder*

	val builder = StringBuilder(512)
    var ch = readChar()
    loop {
        ch == '\n' || ch == '\0' ? break
        builder.appendChar(ch)
        ch = readChar()
    }
    builder.toString()
}