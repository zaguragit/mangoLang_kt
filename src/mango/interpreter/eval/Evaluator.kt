package mango.interpreter.eval

import mango.interpreter.binding.*
import mango.interpreter.symbols.*
import java.util.*
import kotlin.Exception
import kotlin.collections.HashMap
import kotlin.random.Random

class Evaluator(
    val program: BoundProgram,
    val globals: HashMap<VariableSymbol, Any?>
) {

    val functionBodies = HashMap<FunctionSymbol, BoundBlockStatement?>()

    val stack = Stack<HashMap<VariableSymbol, Any?>>().apply { push(HashMap()) }

    init {
        var current: BoundProgram? = program
        while (current != null) {
            for (f in current.functions) {
                functionBodies.putIfAbsent(f.key, f.value)
            }
            current = current.previous
        }
    }

    fun evaluate(): Any? {
        val function = program.mainFn
        val body = functionBodies[function]!!
        evaluateStatement(program.statement)
        return evaluateStatement(body)
    }

    private fun evaluateStatement(body: BoundBlockStatement): Any? {
        val labelToIndex = HashMap<BoundLabel, Int>()
        for (i in body.statements.indices) {
            val s = body.statements.elementAt(i)
            if (s is BoundLabelStatement) {
                labelToIndex[s.symbol] = i + 1
            }
        }
        if (body.statements.size == 1 &&
            body.statements.elementAt(0).boundType == BoundNodeType.ExpressionStatement) {
            return evaluateExpressionStatement(body.statements.elementAt(0) as BoundExpressionStatement)
        }
        var i = 0
        while (i < body.statements.size) {
            val s = body.statements.elementAt(i)
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
                BoundNodeType.ReturnStatement -> {
                    s as BoundReturnStatement
                    return s.expression?.let { evaluateExpression(it) }
                }
                BoundNodeType.NopStatement -> { i++ }
                else -> throw Exception("Unexpected node: ${s.boundType.name}")
            }
        }
        return null
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

    private fun evaluateExpressionStatement(node: BoundExpressionStatement) =
        evaluateExpression(node.expression)

    private fun evaluateVariableDeclaration(node: BoundVariableDeclaration) {
        val value = evaluateExpression(node.initializer)
        assign(node.variable, value)
    }

    private fun evaluateAssignmentExpression(node: BoundAssignmentExpression): Any? {
        val value = evaluateExpression(node.expression)
        assign(node.variable, value)
        return value
    }

    private fun evaluateVariableExpression(node: BoundVariableExpression) =
        if (node.variable.kind == Symbol.Kind.GlobalVariable) {
            globals[node.variable]
        } else {
            val locals = stack.peek()
            locals[node.variable]
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
            print(evaluateExpression(node.arguments.elementAt(0)))
            null
        }
        BuiltinFunctions.println -> {
            val args = node.arguments
            println(evaluateExpression(args.elementAt(0)))
            null
        }
        BuiltinFunctions.readln -> readLine()
        BuiltinFunctions.typeOf -> {
            node.arguments.elementAt(0).type.name
        }
        BuiltinFunctions.random -> {
            Random.nextInt(evaluateExpression(node.arguments.elementAt(0)) as Int)
        }
        else -> {
            val locals = HashMap<VariableSymbol, Any?>()
            for (i in node.arguments.indices) {
                val parameter = node.function.parameters[i]
                val value = evaluateExpression(node.arguments.elementAt(i))
                locals[parameter] = value
            }
            stack.push(locals)
            val statement = functionBodies[node.function]!!
            val result = evaluateStatement(statement)
            stack.pop()
            result
        }
    }

    private fun evaluateCastExpression(node: BoundCastExpression): Any? {
        val value = evaluateExpression(node.expression)
        return when (node.type) {
            TypeSymbol.bool -> value.toString().toBoolean()
            TypeSymbol.int -> value.toString().toInt()
            TypeSymbol.string -> value.toString()
            else -> value
        }
    }

    private fun assign(variable: VariableSymbol, value: Any?) {
        if (variable.kind == Symbol.Kind.GlobalVariable) {
            globals[variable] = value
        } else {
            val locals = stack.peek()
            locals[variable] = value
        }
    }
}