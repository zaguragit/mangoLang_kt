package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.interpreter.binding.nodes.expressions.BlockExpression
import mango.interpreter.binding.nodes.statements.Statement
import mango.interpreter.symbols.CallableSymbol

class Program(
        val previous: Program?,
        val diagnostics: DiagnosticList,
        val functions: HashMap<CallableSymbol, BlockExpression?>,
        val statement: BlockExpression,
        val functionBodies: HashMap<CallableSymbol, Statement?>?
)