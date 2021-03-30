use text.string*

[inline]
val (b Bool) toString () String -> return b ? "true" : "false"

/*
[inline]
val (b Bool) toI64 () I64 -> return b ? 1 : 0
*/
[inline]
val (b Bool) toI32 () I32 -> return b ? 1 : 0
/*
[inline]
val (b Bool) toI16 () I16 -> return b ? 1 : 0

[inline]
val (b Bool) toI8 () I8 -> return b ? 1 : 0
*/

[inline]
val (i I32) toBool () Bool -> i != 0

val (i I32) toString (radix I32) String -> {
    use text.builder*

    i == 0 ? return "0"

    val builder = StringBuilder(10)

    var isNegative = false
    var num = i

    num < 0 && radix == 10 ? {
        isNegative = true
        num = -num
    }

    val translation = "0123456789abcdefghijklmnopqrstuwxyz" // risky, radix can be too big

    loop {
        num == 0 ? break
        val rem = num % radix
        builder.appendChar(translation[rem])
        num /= radix
    }

    isNegative ? builder.appendChar('-')

    builder.invert()

    builder.toString()
}

[inline]
val (i I32) toString () String -> i.toString(10)