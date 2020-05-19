package mango.compilation

import mango.binding.*
import mango.symbols.BuiltinFunctions
import mango.symbols.TypeSymbol
import mango.symbols.VariableSymbol
import kotlin.Exception
import kotlin.random.Random

class Evaluator(
    val root: BoundBlockStatement,
    val variables: HashMap<VariableSymbol, Any?>
) {

    var lastValue: Any? = null

    fun evaluate(): Any? {
        val labelToIndex = HashMap<BoundLabel, Int>()
        for (i in root.statements.indices) {
            val s = root.statements.elementAt(i)
            if (s is BoundLabelStatement) {
                labelToIndex[s.symbol] = i + 1
            }
        }
        var i = 0
        while (i < root.statements.size) {
            val s = root.statements.elementAt(i)
            when (s.boundType) {
                BoundNodeType.ExpressionStatement -> {
                    evaluateExpressionStatement(s as BoundExpressionStatement)
                    i++
                }
                BoundNodeType.VariableDeclaration -> {
                    evaluateVariableDeclaration(s as BoundVariableDeclaration)
                    i++
                }
                BoundNodeType.GotoStatement -> {
                    s as BoundGotoStatement
                    i = labelToIndex[s.label]!!
                }
                BoundNodeType.ConditionalGotoStatement -> {
                    s as BoundConditionalGotoStatement
                    val condition = evaluateExpression(s.condition) as Boolean
                    if (condition == s.jumpIfTrue) {
                        i = labelToIndex[s.label]!!
                    }
                    else { i++ }
                }
                BoundNodeType.LabelStatement -> { i++ }
                else -> throw Exception("Unexpected node: ${s.boundType.name}")
            }
        }
        return lastValue
    }

    private fun evaluateExpression(node: BoundExpression) = when (node) {
        is BoundLiteralExpression -> node.value
        is BoundVariableExpression -> evaluateVariableExpression(node)
        is BoundAssignmentExpression -> evaluateAssignmentExpression(node)
        is BoundUnaryExpression -> evaluateUnaryExpression(node)
        is BoundBinaryExpression -> evaluateBinaryExpression(node)
        is BoundCallExpression -> evaluateCallExpression(node)
        is BoundCastExpression -> evaluateCastExpression(node)
        else -> throw Exception("Unexpected node: ${node.boundType.name}")
    }

    private fun evaluateExpressionStatement(node: BoundExpressionStatement) {
        lastValue = evaluateExpression(node.expression)
    }

    private fun evaluateVariableDeclaration(node: BoundVariableDeclaration) {
        val value = evaluateExpression(node.initializer)
        variables[node.variable] = value
        lastValue = value
    }

    private fun evaluateVariableExpression(node: BoundVariableExpression): Any? {
        return variables[node.variable]
    }

    private fun evaluateAssignmentExpression(node: BoundAssignmentExpression): Any? {
        val value = evaluateExpression(node.expression)
        variables[node.variable] = value
        return value
    }

    private fun evaluateUnaryExpression(node: BoundUnaryExpression): Any? {
        val operand = evaluateExpression(node.operand)
        return when (node.operator.type) {
            BoundUnaryOperatorType.Identity -> operand as Int
            BoundUnaryOperatorType.Negation -> -(operand as Int)
            BoundUnaryOperatorType.Not -> !(operand as Boolean)
        }
    }

    private fun evaluateBinaryExpression(node: BoundBinaryExpression): Any? {
        val left = evaluateExpression(node.left)
        val right = evaluateExpression(node.right)
        if (right == null || left == null) return null
        return when (node.operator.type) {
            BoundBinaryOperatorType.Add -> {
                if (node.type == TypeSymbol.string) {
                    left as String + right as String
                }
                else {
                    left as Int + right as Int
                }
            }
            BoundBinaryOperatorType.Sub -> left as Int - right as Int
            BoundBinaryOperatorType.Mul -> left as Int * right as Int
            BoundBinaryOperatorType.Div -> left as Int / right as Int
            BoundBinaryOperatorType.Rem -> left as Int % right as Int
            BoundBinaryOperatorType.BitAnd -> {
                if (node.type == TypeSymbol.bool) {
                    left as Boolean and right as Boolean
                }
                else {
                    left as Int and right as Int
                }
            }
            BoundBinaryOperatorType.BitOr -> {
                if (node.type == TypeSymbol.bool) {
                    left as Boolean or right as Boolean
                }
                else {
                    left as Int or right as Int
                }
            }
            BoundBinaryOperatorType.LogicAnd -> left as Boolean && right as Boolean
            BoundBinaryOperatorType.LogicOr -> left as Boolean || right as Boolean
            BoundBinaryOperatorType.LessThan -> (left as Int) < right as Int
            BoundBinaryOperatorType.MoreThan -> left as Int > right as Int
            BoundBinaryOperatorType.IsEqual -> left == right
            BoundBinaryOperatorType.IsEqualOrMore -> left as Int >= right as Int
            BoundBinaryOperatorType.IsEqualOrLess -> left as Int <= right as Int
            BoundBinaryOperatorType.IsNotEqual -> left != right
            BoundBinaryOperatorType.IsIdentityEqual -> left === right
            BoundBinaryOperatorType.IsNotIdentityEqual -> left !== right
        }
    }

    private fun evaluateCallExpression(node: BoundCallExpression): Any? = when (node.function) {
        BuiltinFunctions.print -> {
            print(evaluateExpression(node.arguments.elementAt(0)) as String)
            null
        }
        BuiltinFunctions.println -> {
            val args = node.arguments
            println(evaluateExpression(args.elementAt(0)) as String)
            null
        }
        BuiltinFunctions.readln -> readLine()
        BuiltinFunctions.typeOf -> {
            node.arguments.elementAt(0).type.name
        }
        BuiltinFunctions.random -> {
            Random.nextInt(evaluateExpression(node.arguments.elementAt(0)) as Int)
        }
        else -> throw Exception("Unexpected function ${node.function}")
    }

    private fun evaluateCastExpression(node: BoundCastExpression): Any? {
        val value = evaluateExpression(node.expression)
        return when (node.type) {
            TypeSymbol.bool -> {
                value.toString().toBoolean()
            }
            TypeSymbol.int -> {
                value.toString().toInt()
            }
            TypeSymbol.string -> {
                value.toString()
            }
            else -> value
        }
    }
}