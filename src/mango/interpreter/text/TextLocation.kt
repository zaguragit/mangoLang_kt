package mango.interpreter.text

class TextLocation(
    val text: SourceText,
    val span: TextSpan
) {
    inline val startLineI get() = text.getLineI(span.start)
    inline val startCharI get() = span.start - text.lines[startLineI].start

    inline val endLineI get() = text.getLineI(span.end)
    inline val endCharI get() = span.end - text.lines[endLineI].end
}