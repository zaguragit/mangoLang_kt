package shared.text

class SourceText(
    private val text: String,
    val filePackage: String
) {

    val lines = ArrayList<Line>()

    fun getLineI(position: Int): Int {
        var lower = 0
        var upper = lines.size - 1
        while (lower <= upper) {
            val i = lower + (upper - lower) / 2
            val start = lines[i].start
            if (position == start) return i
            if (start > position) upper = i - 1
            else lower = i + 1
        }
        return lower - 1
    }

    init {

        var i = 0
        var lineStart = 0

        fun addLine(lbWidth: Int) {
            val length = i - lineStart
            val lengthIncludingLineBreak = length + lbWidth
            lines.add(Line(this, lineStart, length, lengthIncludingLineBreak))
        }

        while (i < text.length) {
            val lbWidth = getLineBreakWidth(i)
            if (lbWidth == 0) {
                i++
            }
            else {
                addLine(lbWidth)
                i += lbWidth
                lineStart = i
            }
        }

        if (i >= lineStart) {
            addLine(0)
        }
    }

    private fun getLineBreakWidth(i: Int): Int {
        val c = text[i]
        val l = if (i + 1 >= text.length) '\u0000' else text[i + 1]
        return when {
            c == '\r' && l == '\n' -> 2
            c == '\r' || c == '\n' -> 1
            else -> 0
        }
    }

    class Line(
        val sourceText: SourceText,
        val start: Int,
        val length: Int,
        val lengthIncludingLineBreak: Int
    ) {
        val span get() = TextSpan(start, length)

        fun getTextRange(start: Int, end: Int) = getText().substring(start, end)
        fun getText(start: Int, length: Int) = getText().substring(start, start + length)
        fun getText(span: TextSpan) = getText().substring(span.start, span.end)
        fun getText() = sourceText.getText(start, length)

        val end get() = start + length
    }

    fun getTextRange(start: Int, end: Int) = text.substring(start, end)
    fun getText(start: Int, length: Int) = text.substring(start, start + length)
    fun getText(span: TextSpan) = text.substring(span.start, span.end)
    fun getText() = text

    operator fun get(i: Int) = text[i]
    val length get() = text.length
    val lastIndex get() = text.lastIndex
}