/*
struct String {
	val length Int
	val chars I8*
}
*/

[extern]
[cname: "stringToInt"]
fn String.toInt (radix Int) Int

[inline]
fn String.toInt Int -> this.toInt(10)

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
fn String.get(i Int) I8 -> unsafe { this.chars[i] }