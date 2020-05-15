package mango.console

abstract class Repl {

    private val stringBuilder = StringBuilder()
    private var lineI = 0
    protected var lastWasEmpty = false
    protected var thisIsEmpty = false

    fun run() {
        Console.setTitle("mango")
        while (true) {

            val text = editSubmission() ?: return

            evaluateSubmission(text)

            stringBuilder.clear()
        }
    }

    fun editSubmission(): String? {
        val stringBuilder = StringBuilder()
        while (true) {
            if (stringBuilder.isEmpty()) {
                print(Console.BLUE + "> " + Console.RESET)
            } else {
                print(Console.BLUE + "· " + Console.RESET)
            }

            val line = readLine()

            if (stringBuilder.isEmpty()) {
                if (line.isNullOrBlank()) { break }
                if (line.startsWith('#')) {
                    evaluateMetaCommand(line)
                    continue
                }
            }

            stringBuilder.append(line)
            val text = stringBuilder.toString()

            lastWasEmpty = thisIsEmpty
            thisIsEmpty = line.isNullOrEmpty()

            if (!isCompleteSubmission(text)) {
                stringBuilder.append('\n')
                continue
            }

            return text
        }
        return stringBuilder.toString()
    }

    protected abstract fun isCompleteSubmission(string: String): Boolean
    protected abstract fun evaluateSubmission(text: String)

    protected open fun evaluateMetaCommand(cmd: String) {
        when (cmd) {
            "#clear" -> {
                lineI = 0
                Console.clear()
            }
            else -> {
                print(Console.RED_BOLD_BRIGHT)
                print(cmd)
                print(Console.RED)
                println(" isn't a valid command!")
                print(Console.RESET)
            }
        }
    }

    /*private class SubmissionView(val startLineI: Int) {

        val document = ArrayList<String>()

        private var y = 0
        private var x = 0

        var curY
            get() = y
            set(value) {
                if (y != value) {
                    y = value
                    updateCursorPos()
                }
            }

        var curX
            get() = x
            set(value) {
                if (x != value) {
                    x = value
                    updateCursorPos()
                }
            }

        fun updateCursorPos() = Console.setCursorPos(x + 2, y + startLineI)

        private fun submissionDocumentChanged() {
            render()
        }

        private fun render() {
            x = 0

            Console.hideCursor()

            for ((lineCount, line) in document.withIndex()) {
                print(Console.BLUE)
                if (lineCount == 0) {
                    print("> ")
                }
                else { print("· ") }
                print(Console.RESET)
                println(line)
            }

            Console.showCursor()
        }
    }*/
}