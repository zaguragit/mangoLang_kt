
[inline]
fn Bool.toString String -> if this return "true" : return "false"

[inline]
fn Bool.toInt Int -> if this return 1 : return 0

[inline]
fn Int.toBool Bool -> this != 0

fn Int.toString (radix Int) String -> {
    use text.builder*

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