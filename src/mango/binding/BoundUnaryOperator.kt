package mango.binding

import mango.syntax.SyntaxType

class BoundUnaryOperator(
    val syntaxType: SyntaxType,
    val type: BoundUnaryOperatorType,
    val operandType: Type,
    val resultType: Type = operandType
) {
    companion object {

        private val operators = arrayOf(
            BoundUnaryOperator(SyntaxType.Not, BoundUnaryOperatorType.Not, Type.Bool),
            BoundUnaryOperator(SyntaxType.Plus, BoundUnaryOperatorType.Identity, Type.Int),
            BoundUnaryOperator(SyntaxType.Minus, BoundUnaryOperatorType.Negation, Type.Int))

        fun bind(syntaxType: SyntaxType, operandType: Type): BoundUnaryOperator? {
            for (op in operators) {
                if (op.syntaxType == syntaxType && op.operandType == operandType) {
                    return op
                }
            }
            return null
        }
    }
}