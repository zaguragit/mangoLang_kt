use text.string*

@inline
val (b Bool) toString () String -> b ? "true" : "false"

/*
@inline
val (b Bool) toI64 () I64 -> b ? 1 : 0
*/
@inline
val (b Bool) toI32 () I32 -> b ? 1 : 0
/*
@inline
val (b Bool) toI16 () I16 -> b ? 1 : 0

@inline
val (b Bool) toI8 () I8 -> b ? 1 : 0
*/

@inline
val (i I32) toBool () Bool -> i != 0

val (i I32) toString (radix I32) String -> {
    use text.builder*

    i == 0 ? ret "0"

    var isNegative = false
    var num = i

    num < 0 && radix == 10 ? {
        isNegative = true
        num = -num
    }

    val translation = "0123456789abcdefghijklmnopqrstuwxyzαβγδεζηθικλμνξοπσςτυφχψω" // risky, radix can be too big

    val builder = StringBuilder(10)

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

@inline
val (i I32) toString () String -> i.toString(10)

@inline
val iterate (start I32, end I32, step I32, fn Void(I32)) -> {
    var i = start
    loop {
        fn(i)
        i += step
        i == end ? break
    }
}