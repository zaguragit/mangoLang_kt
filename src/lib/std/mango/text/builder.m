use std.string*

struct StringBuilder {
    var length I32
	val chars Ptr<I16>
	var capacity I32
}

[extern]
[cname: "stringBuilder_appendChar"]
fn StringBuilder.appendChar (i16 I16) StringBuilder

/*fn StringBuilder.appendChar (i16 I16) StringBuilder {
    if this.length < this.capacity {
        unsafe {
            this.length += 1
            this.chars[this.length] = i16
        }
    } else {
        this.capacity += 32
        this.chars = ptrArray<I16>(capacity)
    }
    return this
}*/

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