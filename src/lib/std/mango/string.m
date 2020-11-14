
struct String {
	val length I32
	val chars Ptr<I16>
}

fn String.toInt (radix Int) Int -> {
	var n = 0
	var p = 1
	val length = this.length
	var i = length - 1
	while i >= 0 {
		n += unsafe { this.chars[i] - '0' } * p
		p *= radix
		i -= 1
	}
	n
}

[inline]
fn String.toInt Int -> this.toInt(10)

fn Int.toString (radix Int) String -> {
    use std.text.builder*

    if this == 0 return "0"

    val builder = StringBuilder {
        length: 0
        chars: Ptr<I16> { length: 10 }
        capacity: 10
    }

    var isNegative = false
    var num = this

    if num < 0 && radix == 10 {
        isNegative = true
        num = -num
    }

    while num != 0 {
        val rem = (num % radix) as I16
        if rem > 9 builder.appendChar((rem - 10) + 'a')
        : builder.appendChar(rem + '0')
        num /= radix
    }

    if isNegative builder.appendChar('-')

    builder.invert()

    builder.toString()
}

[inline]
fn Int.toString String -> this.toString(10)

[inline]
fn Bool.toString String -> if this return "true" : return "false"

[operator]
fn String.equals (other String) Bool -> {
	val size = this.length
	if size != other.length return false
    var i = 0
    while i < size {
        if this[i] != other[i] return false
        i += 1
    }
	true
}

[inline]
[operator]
fn String.get(i I32) I16 -> unsafe { this.chars[i] }