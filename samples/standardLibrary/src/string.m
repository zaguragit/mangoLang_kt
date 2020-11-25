
struct String {
	val length I32
	val chars Ptr<I16>
}

[inline]
[operator]
fn String.get(i Int) I16 -> unsafe { this.chars[i] }

[cname: "stringToInt"]
fn String.toInt (radix Int) Int -> {
	var n = 0
	var p = 1
	val length = this.length
	var i = length - 1
	loop {
	    i < 0 ? break
		n += (this[i] - '0') * p
		p *= radix
		i -= 1
	}
	n
}


[inline]
fn String.toInt Int -> this.toInt(10)

[operator]
fn String.equals (other String) Bool -> {
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