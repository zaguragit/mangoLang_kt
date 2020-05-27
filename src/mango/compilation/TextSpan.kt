package mango.compilation

class TextSpan {
    val start: Int
    val length: Int
    val end: Int

    constructor(start: Int, length: Int) {
        this.start = start
        this.length = length
        this.end = start + length
    }

    private constructor(start: Int, length: Int, end: Int) {
        this.start = start
        this.length = length
        this.end = end
    }

    companion object {
        fun fromBounds(start: Int, end: Int) = TextSpan(start, end - start, end)
    }
}