use text.string*
use primitives*

type StringBuilder : CharSequence {
    @override
    var length I32
    @override
    var chars Ptr<I16>
	var capacity I32
}

@inline
val StringBuilder (string String) StringBuilder -> StringBuilder {
    length: string.length
    chars: string.chars
    capacity: string.length
}

@inline
val StringBuilder () StringBuilder -> StringBuilder(512)

@inline
val StringBuilder (c I32) StringBuilder -> StringBuilder {
    length: 0
    chars: Ptr<I16> { length: c }
    capacity: c
}

val (s StringBuilder) appendChar (i16 I16) StringBuilder -> {
    s.capacity < s.length ? {
        @cname("realloc")
        val (p Ptr<I16>) realloc (bytes I32) Ptr<I16>

        s.capacity += 32
        s.chars = s.chars.realloc(s.capacity * 2)
    }
    unsafe {
        s.chars[s.length] = i16
        s.length += 1
    }
    s
}

val (s StringBuilder) append (string String) StringBuilder -> {
    use text.string*
    var i = 0
    val length = string.length
    loop {
        i >= length ? break
        s.appendChar(string[i])
        i += 1
    }
    s
}

//@inline
//val StringBuilder.append (i32 I32) StringBuilder -> s.append(i32.toString())

val (s StringBuilder) toString () String -> String {
    length: s.length
    chars: s.chars
}

val (s StringBuilder) invert () -> {
    var i = 0
    var j = s.length - 1
    loop {
        i >= j ? break
        val tmp = s[i]
        s[i] = s[j]
        s[j] = tmp
        i += 1
        j -= 1
    }
}

@inline
@operator
val (s StringBuilder) set(i I32, i16 I16) -> unsafe { s.chars[i] = i16 }