package mango.console

object Console {

    const val ESCAPE = '\u001b'

    const val RESET = "$ESCAPE[0m"

    const val RED = "$ESCAPE[0;31m"
    const val GREEN = "$ESCAPE[0;32m"
    const val YELLOW = "$ESCAPE[0;33m"
    const val BLUE = "$ESCAPE[0;34m"
    const val PURPLE = "$ESCAPE[0;35m"
    const val CYAN = "$ESCAPE[0;36m"
    const val RED_BRIGHT = "$ESCAPE[0;91m"
    const val GREEN_BRIGHT = "$ESCAPE[0;92m"
    const val YELLOW_BRIGHT = "$ESCAPE[0;93m"
    const val BLUE_BRIGHT = "$ESCAPE[0;94m"
    const val PURPLE_BRIGHT = "$ESCAPE[0;95m"
    const val CYAN_BRIGHT = "$ESCAPE[0;96m"

    const val RED_BOLD = "$ESCAPE[1;31m"
    const val GREEN_BOLD = "$ESCAPE[1;32m"
    const val YELLOW_BOLD = "$ESCAPE[1;33m"
    const val BLUE_BOLD = "$ESCAPE[1;34m"
    const val PURPLE_BOLD = "$ESCAPE[1;35m"
    const val CYAN_BOLD = "$ESCAPE[1;36m"
    const val RED_BOLD_BRIGHT = "$ESCAPE[1;91m"
    const val GREEN_BOLD_BRIGHT = "$ESCAPE[1;92m"
    const val YELLOW_BOLD_BRIGHT = "$ESCAPE[1;93m"
    const val BLUE_BOLD_BRIGHT = "$ESCAPE[1;94m"
    const val PURPLE_BOLD_BRIGHT = "$ESCAPE[1;95m"
    const val CYAN_BOLD_BRIGHT = "$ESCAPE[1;96m"

    //print("$ESCAPE[H$ESCAPE[2J")
    inline fun clear() = print("${ESCAPE}c")

    inline fun setTitle(title: String) = print("$ESCAPE]0;$title\u0007")

    inline fun setCursorPos(x: Int, y: Int) = print("$ESCAPE[$y;${x}H")

    inline fun hideCursor() = print("$ESCAPE[?25l")
    inline fun showCursor() = print("$ESCAPE[?25h")
}