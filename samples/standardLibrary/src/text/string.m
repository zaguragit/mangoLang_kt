use primitives*

type String : CharSequence

type CharSequence {
	val length I32
	val chars Ptr<I16>
}

[inline]
[operator]
val (s CharSequence) get (i I32) I16 -> unsafe { s.chars[i] }

[cname: "stringToInt"]
val (s CharSequence) toInt (radix I32) I32 -> {
    var n = 0
    var p = 1
    val length = s.length
    var i = length - 1
    loop {
        i < 0 ? break
        n += {s[i] - '0'} * p
        p *= radix
        i -= 1
    }
    n
}

[inline]
val (s CharSequence) toInt () I32 -> s.toInt(10)

[operator]
val (s CharSequence) equals (other CharSequence) Bool -> {
    val size = s.length
    size != other.length ? return false
    var i = 0
    loop {
        i >= size ? break
        s[i] != other[i] ? return false
        i += 1
    }
    true
}