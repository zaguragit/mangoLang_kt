package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.interpreter.binding.nodes.statements.BlockStatement
import mango.interpreter.symbols.CallableSymbol

class Program(
        val previous: Program?,
        val diagnostics: DiagnosticList,
        val functions: HashMap<CallableSymbol, BlockStatement?>,
        val statement: BlockStatement,
        val functionBodies: HashMap<CallableSymbol, BlockStatement?>?
)