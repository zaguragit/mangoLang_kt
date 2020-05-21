package mango.binding

import mango.compilation.DiagnosticList
import mango.symbols.FunctionSymbol

class BoundProgram(
    val globalScope: BoundGlobalScope,
    val diagnostics: DiagnosticList,
    val functionBodies: HashMap<FunctionSymbol, BoundStatement>
)
