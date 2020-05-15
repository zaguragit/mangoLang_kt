package mango.compilation

class TextSpan(val start: Int, val length: Int) {
    inline val end get() = start + length
}