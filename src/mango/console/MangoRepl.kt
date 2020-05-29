package mango.console

import mango.interpreter.symbols.VariableSymbol
import mango.compilation.Compilation
import mango.interpreter.syntax.parser.SyntaxTree
import kotlin.math.min

class MangoRepl : Repl() {

    private var showParseTree = false
    private var showBindTree = false

    private var previous: Compilation? = null

    private val variables = HashMap<VariableSymbol, Any?>()

    override fun isCompleteSubmission(string: String): Boolean {
        if (string.isEmpty()) {
            return true
        }

        if (lastWasEmpty && thisIsEmpty) {
            return true
        }

        val syntaxTree = SyntaxTree.parse(string)
        if (syntaxTree.root.members.lastOrNull()?.getLastToken()?.isMissing == true) {
            return false
        }
        return true
    }

    override fun evaluateSubmission(text: String) {

        val syntaxTree = SyntaxTree.parse(text)

        val compilation = Compilation(syntaxTree, previous)

        if (showParseTree) {
            syntaxTree.root.printTree()
        }

        if (showBindTree) {
            compilation.printTree()
        }

        val result = compilation.evaluate(variables)
        val errors = result.errors
        val nonErrors = result.nonErrors

        if (errors.isEmpty()) {
            previous = compilation
        } else {
            println()
            for (error in errors) {
                error.print(syntaxTree)
                println()
            }
        }
/*
        for (suggestion in nonErrors) {
            val lineNumber = syntaxTree.sourceText.getLineI(suggestion.span.start)
            val charNumber = suggestion.span.start - syntaxTree.sourceText.lines[lineNumber].start
            val locationStr = Console.BLUE_BRIGHT + "$lineNumber, $charNumber"
            val prefix = when (suggestion.diagnosticType) {
                Diagnostic.Type.Warning -> Console.YELLOW_BRIGHT + "warning(" + locationStr + Console.YELLOW_BRIGHT + "): "
                else -> Console.CYAN + "style(" + locationStr + Console.CYAN + "): "
            }
            val textLine = syntaxTree.sourceText.lines[lineNumber]
            val spanStart = suggestion.span.start
            val spanEnd = min(suggestion.span.end, textLine.end)
            print(prefix + suggestion + Console.RESET + " {\n\t")
            print(text.substring(textLine.start, spanStart))
            when (suggestion.diagnosticType) {
                Diagnostic.Type.Warning -> print(Console.YELLOW_BOLD_BRIGHT)
                else -> print(Console.CYAN_BOLD_BRIGHT)
            }
            print(text.substring(spanStart, spanEnd))
            print(Console.RESET)
            print(text.substring(spanEnd, textLine.end))
            println()
            println('}')
            println()
        }*/
    }

    override fun evaluateMetaCommand(cmd: String) = when (cmd) {
        "#showTree" -> {
            if (showParseTree) {
                showParseTree = false
                println("Parse tree is now invisible")
            } else {
                showParseTree = true
                println("Parse tree is now visible")
            }
        }
        "#showProgram" -> {
            if (showBindTree) {
                showBindTree = false
                println("Bind tree is now invisible")
            } else {
                showBindTree = true
                println("Bind tree is now visible")
            }
        }
        "#reset" -> previous = null
        else -> super.evaluateMetaCommand(cmd)
    }
}