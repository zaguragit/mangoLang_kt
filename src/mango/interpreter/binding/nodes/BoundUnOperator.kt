package mango.interpreter.binding.nodes

import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.syntax.SyntaxType

class BoundUnOperator(
    val syntaxType: SyntaxType,
    val type: Type,
    val operandType: TypeSymbol,
    val resultType: TypeSymbol = operandType
) {
    companion object {

        private val operators = arrayOf(
            BoundUnOperator(SyntaxType.Bang, Type.Not, TypeSymbol.Bool),
            BoundUnOperator(SyntaxType.Plus, Type.Identity, TypeSymbol.I8),
            BoundUnOperator(SyntaxType.Plus, Type.Identity, TypeSymbol.I16),
            BoundUnOperator(SyntaxType.Plus, Type.Identity, TypeSymbol.I32),
            BoundUnOperator(SyntaxType.Plus, Type.Identity, TypeSymbol.I64),
            BoundUnOperator(SyntaxType.Plus, Type.Identity, TypeSymbol.Float),
            BoundUnOperator(SyntaxType.Plus, Type.Identity, TypeSymbol.Double),
            BoundUnOperator(SyntaxType.Minus, Type.Negation, TypeSymbol.I8),
            BoundUnOperator(SyntaxType.Minus, Type.Negation, TypeSymbol.I16),
            BoundUnOperator(SyntaxType.Minus, Type.Negation, TypeSymbol.I32),
            BoundUnOperator(SyntaxType.Minus, Type.Negation, TypeSymbol.I64),
            BoundUnOperator(SyntaxType.Minus, Type.Negation, TypeSymbol.Float),
            BoundUnOperator(SyntaxType.Minus, Type.Negation, TypeSymbol.Double)
        )

        fun bind(syntaxType: SyntaxType, operandType: TypeSymbol): BoundUnOperator? {
            for (op in operators) {
                if (op.syntaxType == syntaxType && op.operandType == operandType) {
                    return op
                }
            }
            return null
        }

        fun getString(type: Type): String = when (type) {
            Type.Identity -> "+"
            Type.Negation -> "-"
            Type.Not -> "!"
        }

        fun getString(type: SyntaxType): String = when (type) {
            SyntaxType.Plus -> "+"
            SyntaxType.Minus -> "-"
            SyntaxType.Bang -> "!"
            else -> type.toString()
        }
    }

    enum class Type {
        Identity,
        Negation,
        Not
    }
}