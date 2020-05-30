package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.interpreter.symbols.FunctionSymbol

class BoundProgram(
    val previous: BoundProgram?,
    val diagnostics: DiagnosticList,
    val functionBodies: HashMap<FunctionSymbol, BoundStatement>,
    val statement: BoundBlockStatement
)
