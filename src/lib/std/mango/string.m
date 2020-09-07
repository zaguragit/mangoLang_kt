
struct String {
	val length I32
	val chars Ptr<I16>
}

[cname: "stringToInt"]
fn String.toInt (radix Int) Int {
	var n = 0
	var p = 1
	val length = this.length
	var i = length - 1
	while i >= 0 {
		n = n + unsafe { this.chars[i] - '0' } * p
		p = p * radix
		i = i - 1
	}
	return n
}

[inline]
fn String.toInt Int -> this.toInt(10)

[extern]
[cname: "intToString"]
fn Int.toString (radix Int) String

[inline]
fn Int.toString String -> this.toString(10)

[inline]
fn Bool.toString String {
    if this { return "true" }
    else { return "false" }
}

[operator]
fn String.equals (other String) Bool {
	val size = this.length
	if size != other.length { return false }
    var i = 0
    while i < size {
        if this[i] != other[i] {
            return false
        }
        i = i + 1
    }
	return true
}

[inline]
[operator]
fn String.get(i Int) I16 -> unsafe { this.chars[i] }