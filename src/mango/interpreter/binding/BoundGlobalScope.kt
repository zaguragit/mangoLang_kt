package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.interpreter.binding.nodes.statements.BoundStatement
import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.symbols.Symbol

class BoundGlobalScope(
    val previous: BoundGlobalScope?,
    val diagnostics: DiagnosticList,
    val symbols: Collection<Symbol>,
    val statements: MutableList<BoundStatement>,
    val mainFn: FunctionSymbol)