package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.interpreter.binding.nodes.statements.Statement
import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.symbols.Symbol

class GlobalScope(
    val previous: GlobalScope?,
    val diagnostics: DiagnosticList,
    val symbols: Collection<Symbol>,
    val statements: MutableList<Statement>,
    val mainFn: FunctionSymbol)