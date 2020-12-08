
[inline]
fn Bool.toString String -> return this ? "true" : "false"

[inline]
fn Bool.toInt Int -> return this ? 1 : 0

[inline]
fn Int.toBool Bool -> this != 0

fn Int.toString (radix Int) String -> {
    use text.builder*

    this == 0 ? return "0"

    val builder = StringBuilder {
        length: 0
        chars: Ptr<I16> { length: 10 }
        capacity: 10
    }

    var isNegative = false
    var num = this

    num < 0 && radix == 10 ? {
        isNegative = true
        num = -num
    }

    loop {
        num == 0 ? break
        val rem = {num % radix} as I16
        rem > 9 ? builder.appendChar({rem - 10} + 'a')
        : builder.appendChar(rem + '0')
        num /= radix
    }

    isNegative ? builder.appendChar('-')

    builder.invert()

    builder.toString()
}

[inline]
fn Int.toString String -> this.toString(10)