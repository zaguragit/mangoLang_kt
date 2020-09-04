package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.interpreter.binding.nodes.statements.BlockStatement
import mango.interpreter.symbols.FunctionSymbol

class Program(
    val previous: Program?,
    val diagnostics: DiagnosticList,
    val functions: HashMap<FunctionSymbol, BlockStatement?>,
    val statement: BlockStatement,
    val functionBodies: HashMap<FunctionSymbol, BlockStatement?>?
)