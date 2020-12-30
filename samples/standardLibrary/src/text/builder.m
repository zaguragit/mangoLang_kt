use string*
use primitives*

type StringBuilder : String {
    [override]
    var length I32
    [override]
    var chars Ptr<I16>
	var capacity I32
}

val StringBuilder.appendChar (i16 I16) StringBuilder -> {
    this.capacity < this.length ? {
        [extern]
        [cname: "realloc"]
        val Ptr<I16>.realloc (bytes I32) Ptr<I16>

        this.capacity += 32
        this.chars = this.chars.realloc(this.capacity * 2)
    }
    unsafe {
        this.chars[this.length] = i16
        this.length += 1
    }
    this
}

val StringBuilder.append (string String) StringBuilder -> {
    use string*
    var i = 0
    val length = string.length
    loop {
        i >= length ? break
        this.appendChar(string[i])
        i += 1
    }
    this
}

//[inline]
//val StringBuilder.append (i32 I32) StringBuilder -> this.append(i32.toString())

val StringBuilder.toString String -> String {
    length: this.length
    chars: this.chars
}

val StringBuilder.invert -> {
    var i = 0
    var j = this.length - 1
    loop {
        i >= j ? break
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
        i += 1
        j -= 1
    }
}

[inline]
[operator]
val StringBuilder.set(i Int, i16 I16) -> unsafe { this.chars[i] = i16 }