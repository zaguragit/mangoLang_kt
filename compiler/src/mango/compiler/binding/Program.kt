package mango.compiler.binding

import mango.compiler.binding.nodes.expressions.BlockExpression
import mango.compiler.binding.nodes.statements.Statement
import mango.compiler.symbols.CallableSymbol
import shared.DiagnosticList

class Program(
    val diagnostics: DiagnosticList,
    val functions: HashMap<CallableSymbol, BlockExpression?>,
    val statement: BlockExpression,
    val functionBodies: HashMap<CallableSymbol, Statement?>
)