package mango.compiler.binding

import mango.compiler.binding.nodes.statements.Statement
import mango.compiler.symbols.Symbol
import shared.DiagnosticList

class GlobalScope(
    val diagnostics: DiagnosticList,
    val symbols: Collection<Symbol>,
    val statements: MutableList<Statement>)