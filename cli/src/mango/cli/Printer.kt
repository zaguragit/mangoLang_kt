package mango.cli

import mango.cli.console.Console
import mango.cli.console.Highlighter
import mango.compiler.binding.nodes.expressions.BlockExpression
import mango.compiler.binding.structureString
import mango.compiler.ir.ControlFlowGraph
import mango.compiler.symbols.CallableSymbol
import mango.compiler.symbols.Symbol
import mango.compiler.symbols.TypeSymbol
import mango.compiler.symbols.VariableSymbol
import mango.parser.SyntaxTree
import mango.parser.Token
import mango.parser.nodes.Node
import shared.Diagnostic

fun Symbol.printStructure() = when (kind) {
    Symbol.Kind.Variable, Symbol.Kind.VisibleVariable, Symbol.Kind.Field -> printVariable()
    Symbol.Kind.Parameter -> printParameter()
    Symbol.Kind.Function -> printFunction()
    Symbol.Kind.Type -> print(this)
    Symbol.Kind.StructType -> printStruct()
    Symbol.Kind.FunctionType -> printFnType()
}

private fun Symbol.printVariable() {
    this as VariableSymbol
    print(Highlighter.keyword(if (isReadOnly) { if (constant == null) "val " else "const " } else "var "))
    print(name)
    print(' ')
    print(Highlighter.type(type.toString()))
}

private fun Symbol.printParameter() {
    this as VariableSymbol
    print(name)
    print(' ')
    print(Highlighter.type(type.toString()))
}

private fun Symbol.printFunction() {
    this as CallableSymbol
    print(Highlighter.keyword("val "))
    print(name)
    print(" (")
    for (i in parameters.indices) {
        if (i != 0) {
            print(',')
            print(' ')
        }
        parameters[i].printStructure()
    }
    print(')')
    print(' ')
    print(Highlighter.type(returnType.toString()))
}

private fun Symbol.printStruct() {
    this as TypeSymbol.StructTypeSymbol
    print(Highlighter.keyword("type "))
    print(name)
    println(" {")
    for (field in fields) {
        print('\t')
        print(Highlighter.keyword("val"))
        print(field.name)
        print(' ')
        print(Highlighter.type(field.type.toString()))
        println()
    }
    print('}')
}

private fun Symbol.printFnType() {
    this as TypeSymbol.Fn
    print("$returnType(")
    args.joinToString(", ")
    print(")")
}

fun ControlFlowGraph.print() {
    println("ControlFlowGraph {")
    for (b in blocks) {
        println("    Block $b {")
        println("        incoming: ${b.incoming.joinToString(", ")}")
        println("        outgoing: ${b.outgoing.joinToString(", ")}")
        if (b.statements.any()) {
            print("        statements ")
            print(BlockExpression(b.statements, TypeSymbol.Void).structureString(indent = 2, sameLine = true))
        }
        println("    }")
    }
    for (b in branches) {
        print("    Branch $b: ")
        print(b.from)
        print(" --${b.condition ?: ""}--> ")
        println(b.to)
    }
    println('}')
}

fun printError(diagnostic: Diagnostic) {
    val location = diagnostic.location
    val message = diagnostic.message
    if (location == null) {
        print(Console.RED + "error: $message" + Console.RESET)
        return
    }
    val span = location.span
    val charI = location.startCharI
    val startLine = location.text.lines[location.startLineI]
    val endLine = location.text.lines[location.endLineI]

    val spanStart = span.start
    val spanEnd = span.end

    print(Console.RED + "${location.text.filePackage}[" + Console.BLUE_BRIGHT + "${location.startLineI}, $charI" + Console.RED + "]: $message" + Console.RESET + " {\n\t")
    print(location.text.getTextRange(startLine.start, spanStart))
    print(Console.RED_BOLD_BRIGHT)
    print(location.text.getTextRange(spanStart, spanEnd).replace("\n", "\n\t"))
    print(Console.RESET)
    print(location.text.getTextRange(spanEnd, endLine.end))
    println()
    println('}')
}

fun printSuggestion(diagnostic: Diagnostic) {
    val location = diagnostic.location
    val message = diagnostic.message
    val span = location!!.span
    val charI = location.startCharI
    val startLine = location.text.lines[location.startLineI]
    val endLine = location.text.lines[location.endLineI]

    val spanStart = span.start
    val spanEnd = span.end

    val color = when (diagnostic.diagnosticType) {
        Diagnostic.Type.Warning -> Console.YELLOW_BRIGHT
        else -> Console.CYAN
    }

    print(color + "${location.text.filePackage}[" + Console.BLUE_BRIGHT + "${location.startLineI}, $charI" + color + "]: $message" + Console.RESET + " {\n\t")
    print(location.text.getTextRange(startLine.start, spanStart))
    when (diagnostic.diagnosticType) {
        Diagnostic.Type.Warning -> print(Console.YELLOW_BOLD_BRIGHT)
        else -> print(Console.CYAN_BOLD_BRIGHT)
    }
    print(location.text.getTextRange(spanStart, spanEnd).replace("\n", "\n\t"))
    print(Console.RESET)
    print(location.text.getTextRange(spanEnd, endLine.end))
    println()
    println('}')
}

fun SyntaxTree.printTree(indent: String = "", isLast: Boolean = true) {
    print(Console.GRAY)
    print(indent)
    print(if (isLast) "└──" else "├──")

    print(Console.RESET)
    print(projectPath)

    println()

    val newIndent = indent + if (isLast) "    " else "│   "

    val lastChild = members.lastOrNull()

    for (child in members) {
        child.printTree(newIndent, child === lastChild)
    }

    print(Console.RESET)
}

fun Node.printTree(indent: String = "", isLast: Boolean = true) {
    print(Console.GRAY)
    print(indent)
    print(if (isLast) "└──" else "├──")

    if (this is Token) {
        print(Console.CYAN_BRIGHT)
        print(kind.name)
        if (string != null) {
            print(" ")
            print(Console.GREEN_BOLD_BRIGHT)
            print(string)
        }
    } else {
        print(Console.RESET)
        print(kind.name)
    }

    println()

    val newIndent = indent + if (isLast) "    " else "│   "

    val lastChild = children.lastOrNull()

    for (child in children) {
        child.printTree(newIndent, child === lastChild)
    }

    print(Console.RESET)
}