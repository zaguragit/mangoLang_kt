package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.interpreter.binding.nodes.statements.BoundBlockStatement
import mango.interpreter.symbols.FunctionSymbol

class BoundProgram(
        val previous: BoundProgram?,
        val diagnostics: DiagnosticList,
        val mainFn: FunctionSymbol,
        val functions: HashMap<FunctionSymbol, BoundBlockStatement?>,
        val statement: BoundBlockStatement,
        val functionBodies: HashMap<FunctionSymbol, BoundBlockStatement?>?
)