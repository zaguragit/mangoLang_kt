package mango.interpreter.binding.nodes

import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.syntax.SyntaxType

class UnOperator(
    val syntaxType: SyntaxType,
    val type: Type,
    val operandType: TypeSymbol,
    val resultType: TypeSymbol = operandType
) {
    companion object {

        private val operators = arrayOf(
            UnOperator(SyntaxType.Bang, Type.Not, TypeSymbol.Bool),
            UnOperator(SyntaxType.Plus, Type.Identity, TypeSymbol.I8),
            UnOperator(SyntaxType.Plus, Type.Identity, TypeSymbol.I16),
            UnOperator(SyntaxType.Plus, Type.Identity, TypeSymbol.I32),
            UnOperator(SyntaxType.Plus, Type.Identity, TypeSymbol.I64),
            UnOperator(SyntaxType.Plus, Type.Identity, TypeSymbol.Float),
            UnOperator(SyntaxType.Plus, Type.Identity, TypeSymbol.Double),
            UnOperator(SyntaxType.Minus, Type.Negation, TypeSymbol.I8),
            UnOperator(SyntaxType.Minus, Type.Negation, TypeSymbol.I16),
            UnOperator(SyntaxType.Minus, Type.Negation, TypeSymbol.I32),
            UnOperator(SyntaxType.Minus, Type.Negation, TypeSymbol.I64),
            UnOperator(SyntaxType.Minus, Type.Negation, TypeSymbol.Float),
            UnOperator(SyntaxType.Minus, Type.Negation, TypeSymbol.Double)
        )

        fun bind(syntaxType: SyntaxType, operandType: TypeSymbol): UnOperator? {
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