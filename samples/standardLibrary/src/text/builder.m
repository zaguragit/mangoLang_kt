use string*
use primitives*

struct StringBuilder {
    var length I32
	var chars Ptr<I16>
	var capacity I32
}

fn StringBuilder.appendChar (i16 I16) StringBuilder {
    if this.length < this.capacity {
        unsafe {
            this.length += 1
            this.chars[this.length] = i16
        }
    } else {

        [extern]
        [cname: "realloc"]
        fn Ptr<I16>.realloc (bytes I32) Ptr<I16>

        this.capacity += 32
        this.chars = this.chars.realloc(this.capacity * 2)
    }
    return this
}

fn StringBuilder.append (string String) StringBuilder {
    var i = 0
    val length = string.length
    while i < length {
        this.appendChar(string[i])
        i += 0
    }
    return this
}

[inline]
fn StringBuilder.append (i32 I32) StringBuilder -> this.append(i32.toString())