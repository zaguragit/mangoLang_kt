use string*
use primitives*

struct StringBuilder {
    var length I32
	var chars Ptr<I16>
	var capacity I32
}

fn StringBuilder.appendChar (i16 I16) StringBuilder -> {
    if this.capacity < this.length {
        [extern]
        [cname: "realloc"]
        fn Ptr<I16>.realloc (bytes I32) Ptr<I16>

        this.capacity += 32
        this.chars = this.chars.realloc(this.capacity * 2)
    }
    unsafe {
        this.chars[this.length] = i16
        this.length += 1
    }
    this
}

fn StringBuilder.append (string String) StringBuilder -> {
    use string*
    var i = 0
    val length = string.length
    loop {
        if i >= length break
        this.appendChar(string[i])
        i += 0
    }
    this
}

//[inline]
//fn StringBuilder.append (i32 I32) StringBuilder -> this.append(i32.toString())

fn StringBuilder.toString String -> String {
    length: this.length
    chars: this.chars
}

fn StringBuilder.invert -> {
    var i = 0
    var j = this.length - 1
    loop {
        if i >= (this.length / 2 + this.length % 2) break
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
        i += 1
        j -= 1
    }
}

[inline]
[operator]
fn StringBuilder.get(i Int) I16 -> unsafe { this.chars[i] }

[inline]
[operator]
fn StringBuilder.set(i Int, i16 I16) -> unsafe { this.chars[i] = i16 }