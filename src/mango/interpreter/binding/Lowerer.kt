package mango.interpreter.binding

import mango.interpreter.binding.nodes.BiOperator
import mango.interpreter.binding.nodes.BoundNode
import mango.interpreter.binding.nodes.expressions.*
import mango.interpreter.binding.nodes.statements.*
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.symbols.VariableSymbol
import mango.interpreter.syntax.SyntaxType
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class Lowerer : TreeRewriter() {

    companion object {
        fun lower(expression: Expression): BlockStatement {
            val lowerer = Lowerer()
            val block = if (expression.type == TypeSymbol.Unit) {
                BlockStatement(listOf(ExpressionStatement(expression)))
            } else {
                BlockStatement(listOf(ReturnStatement(expression)))
            }
            val result = lowerer.rewriteStatement(block)
            return removeDeadCode(flatten(result))
        }

        fun lower(block: BlockStatement): BlockStatement {
            val lowerer = Lowerer()
            val result = lowerer.rewriteBlockStatement(block)
            return removeDeadCode(flatten(result))
        }

        private fun flatten(statement: Statement): BlockStatement {
            val arrayList = ArrayList<Statement>()
            val stack = Stack<Statement>()
            stack.push(statement)
            val variableNames = HashSet<String>()

            while (stack.count() > 0) {
                val current = stack.pop()
                if (current is BlockStatement) {
                    for (s in current.statements.reversed()) {
                        var s = s
                        when (s.kind) {
                            BoundNode.Kind.VariableDeclaration -> {
                                s as VariableDeclaration
                                if (!variableNames.add(s.variable.realName)) {
                                    var string = s.variable.realName
                                    while (!variableNames.add(string)) {
                                        string = ".l_$string"
                                    }
                                    s.variable.realName = string
                                }
                                val tmpStack = Stack<Statement>()
                                s = VariableDeclaration(s.variable, flattenExpression(s.initializer, tmpStack, variableNames))
                                stack.push(s)
                                tmpStack.forEach { stack.push(it) }
                            }
                            BoundNode.Kind.ExpressionStatement -> {
                                s as ExpressionStatement
                                val tmpStack = Stack<Statement>()
                                s = ExpressionStatement(flattenExpression(s.expression, tmpStack, variableNames))
                                stack.push(s)
                                tmpStack.forEach { stack.push(it) }
                            }
                            BoundNode.Kind.ConditionalGotoStatement -> {
                                s as ConditionalGotoStatement
                                val tmpStack = Stack<Statement>()
                                s = ConditionalGotoStatement(s.label, flattenExpression(s.condition, tmpStack, variableNames), s.jumpIfTrue)
                                stack.push(s)
                                tmpStack.forEach { stack.push(it) }
                            }
                            BoundNode.Kind.ReturnStatement -> {
                                s as ReturnStatement
                                val tmpStack = Stack<Statement>()
                                s = ReturnStatement(s.expression?.let { flattenExpression(it, tmpStack, variableNames) })
                                stack.push(s)
                                tmpStack.forEach { stack.push(it) }
                            }
                            BoundNode.Kind.AssignmentStatement -> {
                                s as Assignment
                                val tmpStack = Stack<Statement>()
                                s = Assignment(flattenExpression(s.assignee, tmpStack, variableNames), flattenExpression(s.expression, tmpStack, variableNames))
                                stack.push(s)
                                tmpStack.forEach { stack.push(it) }
                            }
                            else -> stack.push(s)
                        }
                    }
                } else {
                    arrayList.add(current)
                }
            }
            return BlockStatement(arrayList)
        }

        private fun flattenExpression(expression: Expression, stack: Stack<Statement>, variableNames: HashSet<String>): Expression {
            return when (expression.kind) {
                BoundNode.Kind.UnaryExpression -> {
                    expression as UnaryExpression
                    UnaryExpression(expression.operator, flattenExpression(expression.operand, stack, variableNames))
                }
                BoundNode.Kind.BinaryExpression -> {
                    expression as BinaryExpression
                    BinaryExpression(flattenExpression(expression.left, stack, variableNames), expression.operator, flattenExpression(expression.right, stack, variableNames))
                }
                BoundNode.Kind.CallExpression -> {
                    expression as CallExpression
                    CallExpression(expression.expression, expression.arguments.map { flattenExpression(it, stack, variableNames) })
                }
                BoundNode.Kind.CastExpression -> {
                    expression as CastExpression
                    CastExpression(expression.type, flattenExpression(expression.expression, stack, variableNames))
                }
                BoundNode.Kind.StructInitialization -> {
                    expression as StructInitialization
                    StructInitialization(expression.type, expression.fields.mapValues { flattenExpression(it.value, stack, variableNames) })
                }
                BoundNode.Kind.BlockExpression -> {
                    expression as BlockExpression
                    var result: Expression? = null
                    loop@ for (i in expression.statements.indices.reversed()) {
                        var s = expression.statements.elementAt(i)
                        when (s.kind) {
                            BoundNode.Kind.VariableDeclaration -> {
                                s as VariableDeclaration
                                if (!variableNames.add(s.variable.realName)) {
                                    var string = s.variable.realName
                                    while (!variableNames.add(string)) {
                                        string = ".l_$string"
                                    }
                                    s.variable.realName = string
                                }
                                s = VariableDeclaration(s.variable, flattenExpression(s.initializer, stack, variableNames))
                            }
                            BoundNode.Kind.ExpressionStatement -> {
                                s as ExpressionStatement
                                if (i == expression.statements.size - 1) {
                                    result = flattenExpression(s.expression, stack, variableNames)
                                    continue@loop
                                }
                                s = ExpressionStatement(flattenExpression(s.expression, stack, variableNames))
                            }
                            BoundNode.Kind.ConditionalGotoStatement -> {
                                s as ConditionalGotoStatement
                                s = ConditionalGotoStatement(s.label, flattenExpression(s.condition, stack, variableNames), s.jumpIfTrue)
                            }
                            BoundNode.Kind.ReturnStatement -> {
                                s as ReturnStatement
                                s = ReturnStatement(s.expression?.let { flattenExpression(it, stack, variableNames) })
                            }
                        }
                        stack.push(s)
                    }
                    result ?: expression
                }
                else -> expression
            }
        }

        private fun removeDeadCode(block: BlockStatement): BlockStatement {
            val controlFlow = ControlFlowGraph.create(block)
            val reachableStatements = controlFlow.blocks.flatMap { it.statements }.toHashSet()
            val builder = block.statements.toMutableList()
            for (i in builder.lastIndex downTo 0) {
                if (!reachableStatements.contains(builder[i])) {
                    builder.removeAt(i)
                }
            }
            return BlockStatement(builder)
        }
    }

    private var labelCount = 0
    private fun generateLabel(): Label {
        val name = "L${(++labelCount).toString(16)}"
        return Label(name)
    }

    override fun rewriteForStatement(node: ForStatement): Statement {
        val variableDeclaration = VariableDeclaration(node.variable, node.lowerBound)
        val variableExpression = NameExpression(node.variable)
        val upperBoundSymbol = VariableSymbol.local("0upperBound", TypeSymbol.Int, true, node.upperBound.constantValue)
        val upperBoundDeclaration = VariableDeclaration(upperBoundSymbol, node.upperBound)
        val condition = BinaryExpression(
            variableExpression,
            BiOperator.bind(SyntaxType.IsEqualOrLess, TypeSymbol.Int, TypeSymbol.Int)!!,
            NameExpression(upperBoundSymbol)
        )
        val continueLabelStatement = LabelStatement(node.continueLabel)
        val increment = Assignment(
            NameExpression(node.variable),
            BinaryExpression(
                variableExpression,
                BiOperator.bind(SyntaxType.Plus, TypeSymbol.Int, TypeSymbol.Int)!!,
                LiteralExpression(1, TypeSymbol.I32)
            )
        )
        val body = BlockStatement(listOf(
            node.body,
            continueLabelStatement,
            increment
        ))
        val whileStatement = WhileStatement(condition, body, node.breakLabel, generateLabel())
        val result = BlockStatement(listOf(variableDeclaration, upperBoundDeclaration, whileStatement))
        return rewriteStatement(result)
    }

    override fun rewriteIfStatement(node: IfStatement): Statement {

        if (node.elseStatement == null) {
            val endLabel = generateLabel()
            val gotoFalse = ConditionalGotoStatement(endLabel, node.condition, false)
            val endLabelStatement = LabelStatement(endLabel)
            val result = BlockStatement(listOf(gotoFalse, node.statement, endLabelStatement))
            return rewriteStatement(result)
        }

        val elseLabel = generateLabel()
        val endLabel = generateLabel()
        val gotoFalse = ConditionalGotoStatement(elseLabel, node.condition, false)
        val gotoEnd = GotoStatement(endLabel)
        val elseLabelStatement = LabelStatement(elseLabel)
        val endLabelStatement = LabelStatement(endLabel)
        val result = BlockStatement(listOf(
            gotoFalse,
            node.statement,
            gotoEnd,
            elseLabelStatement,
            node.elseStatement,
            endLabelStatement
        ))
        return rewriteStatement(result)
    }

    override fun rewriteWhileStatement(node: WhileStatement): Statement {
        val checkLabel = generateLabel()
        val gotoCheck = GotoStatement(checkLabel)
        val continueLabelStatement = LabelStatement(node.continueLabel)
        val checkLabelStatement = LabelStatement(checkLabel)
        val gotoTrue = ConditionalGotoStatement(node.continueLabel, node.condition, true)
        val breakLabelStatement = LabelStatement(node.breakLabel)
        return BlockStatement(listOf(
            gotoCheck,
            continueLabelStatement,
            rewriteBlockStatement(node.body),
            checkLabelStatement,
            rewriteConditionalGotoStatement(gotoTrue),
            breakLabelStatement
        ))
    }

    override fun rewriteConditionalGotoStatement(node: ConditionalGotoStatement): Statement {
        val constant = node.condition.constantValue
        if (constant != null) {
            return if (constant.value as Boolean == node.jumpIfTrue) {
                rewriteGotoStatement(GotoStatement(node.label))
            } else {
                NopStatement()
            }
        }
        return super.rewriteConditionalGotoStatement(node)
    }
}