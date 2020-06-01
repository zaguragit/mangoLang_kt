package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.interpreter.symbols.FunctionSymbol

class BoundProgram(
    val previous: BoundProgram?,
    val diagnostics: DiagnosticList,
    val mainFn: FunctionSymbol,
    val functionBodies: HashMap<FunctionSymbol, BoundBlockStatement>,
    val statement: BoundBlockStatement
)
