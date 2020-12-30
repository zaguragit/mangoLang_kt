
type String {
	val length I32
	val chars Ptr<I16>
}

[inline]
[operator]
val String.get(i Int) I16 -> unsafe { this.chars[i] }

[cname: "stringToInt"]
val String.toInt (radix Int) Int -> {
	var n = 0
	var p = 1
	val length = this.length
	var i = length - 1
	loop {
	    i < 0 ? break
		n += {this[i] - '0'} * p
		p *= radix
		i -= 1
	}
	n
}


[inline]
val String.toInt Int -> this.toInt(10)

[operator]
val String.equals (other String) Bool -> {
	val size = this.length
	size != other.length ? return false
    var i = 0
    loop {
        i >= size ? break
        this[i] != other[i] ? return false
        i += 1
    }
	true
}