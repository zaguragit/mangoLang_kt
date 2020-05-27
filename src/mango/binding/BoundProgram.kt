package mango.binding

import mango.compilation.DiagnosticList
import mango.symbols.FunctionSymbol

class BoundProgram(
    val diagnostics: DiagnosticList,
    val functionBodies: HashMap<FunctionSymbol, BoundStatement>,
    val statement: BoundBlockStatement
)
