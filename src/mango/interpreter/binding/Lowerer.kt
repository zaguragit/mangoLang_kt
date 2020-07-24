package mango.interpreter.binding

import mango.interpreter.binding.nodes.BoundBinaryOperator
import mango.interpreter.binding.nodes.expressions.*
import mango.interpreter.binding.nodes.statements.*
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.symbols.VariableSymbol
import mango.interpreter.syntax.SyntaxType
import java.util.*
import kotlin.collections.ArrayList

class Lowerer : BoundTreeRewriter() {

    companion object {
        fun lower(expression: BoundExpression): BoundBlockStatement {
            val lowerer = Lowerer()
            val block = if (expression.type == TypeSymbol.Unit) {
                BoundBlockStatement(listOf(BoundExpressionStatement(expression)))
            } else {
                BoundBlockStatement(listOf(BoundReturnStatement(expression)))
            }
            val result = lowerer.rewriteStatement(block)
            return removeDeadCode(flatten(result))
        }

        fun lower(block: BoundBlockStatement): BoundBlockStatement {
            val lowerer = Lowerer()
            val result = lowerer.rewriteBlockStatement(block)
            return removeDeadCode(flatten(result))
        }

        private fun flatten(statement: BoundStatement): BoundBlockStatement {
            val arrayList = ArrayList<BoundStatement>()
            val stack = Stack<BoundStatement>()
            stack.push(statement)
            while (stack.count() > 0) {
                val current = stack.pop()
                if (current is BoundBlockStatement) {
                    for (s in current.statements.reversed()) {
                        stack.push(s)
                    }
                } else {
                    arrayList.add(current)
                }
            }
            return BoundBlockStatement(arrayList)
        }

        private fun removeDeadCode(block: BoundBlockStatement): BoundBlockStatement {
            val controlFlow = ControlFlowGraph.create(block)
            val reachableStatements = controlFlow.blocks.flatMap { it.statements }.toHashSet()
            val builder = block.statements.toMutableList()
            for (i in builder.lastIndex downTo 0) {
                if (!reachableStatements.contains(builder[i])) {
                    builder.removeAt(i)
                }
            }
            return BoundBlockStatement(builder)
        }
    }

    private var labelCount = 0
    private fun generateLabel(): BoundLabel {
        val name = "L${(++labelCount).toString(16)}"
        return BoundLabel(name)
    }

    override fun rewriteForStatement(node: BoundForStatement): BoundStatement {
        val variableDeclaration = BoundVariableDeclaration(node.variable, node.lowerBound)
        val variableExpression = BoundVariableExpression(node.variable)
        val upperBoundSymbol = VariableSymbol.local("0upperBound", TypeSymbol.Int, true, node.upperBound.constantValue)
        val upperBoundDeclaration = BoundVariableDeclaration(upperBoundSymbol, node.upperBound)
        val condition = BoundBinaryExpression(
                variableExpression,
                BoundBinaryOperator.bind(SyntaxType.IsEqualOrLess, TypeSymbol.Int, TypeSymbol.Int)!!,
                BoundVariableExpression(upperBoundSymbol)
        )
        val continueLabelStatement = BoundLabelStatement(node.continueLabel)
        val increment = BoundExpressionStatement(BoundAssignmentExpression(
                node.variable,
                BoundBinaryExpression(
                    variableExpression,
                    BoundBinaryOperator.bind(SyntaxType.Plus, TypeSymbol.Int, TypeSymbol.Int)!!,
                    BoundLiteralExpression(1, TypeSymbol.I32)
                )
        ))
        val body = BoundBlockStatement(listOf(
                node.body,
                continueLabelStatement,
                increment
        ))
        val whileStatement = BoundWhileStatement(condition, body, node.breakLabel, generateLabel())
        val result = BoundBlockStatement(listOf(variableDeclaration, upperBoundDeclaration, whileStatement))
        return rewriteStatement(result)
    }

    override fun rewriteIfStatement(node: BoundIfStatement): BoundStatement {

        if (node.elseStatement == null) {
            val endLabel = generateLabel()
            val gotoFalse = BoundConditionalGotoStatement(endLabel, node.condition, false)
            val endLabelStatement = BoundLabelStatement(endLabel)
            val result = BoundBlockStatement(listOf(gotoFalse, node.statement, endLabelStatement))
            return rewriteStatement(result)
        }

        val elseLabel = generateLabel()
        val endLabel = generateLabel()
        val gotoFalse = BoundConditionalGotoStatement(elseLabel, node.condition, false)
        val gotoEnd = BoundGotoStatement(endLabel)
        val elseLabelStatement = BoundLabelStatement(elseLabel)
        val endLabelStatement = BoundLabelStatement(endLabel)
        val result = BoundBlockStatement(listOf(
                gotoFalse,
                node.statement,
                gotoEnd,
                elseLabelStatement,
                node.elseStatement,
                endLabelStatement
        ))
        return rewriteStatement(result)
    }

    override fun rewriteWhileStatement(node: BoundWhileStatement): BoundStatement {
        val checkLabel = generateLabel()
        val gotoCheck = BoundGotoStatement(checkLabel)
        val continueLabelStatement = BoundLabelStatement(node.continueLabel)
        val checkLabelStatement = BoundLabelStatement(checkLabel)
        val gotoTrue = BoundConditionalGotoStatement(node.continueLabel, node.condition, true)
        val breakLabelStatement = BoundLabelStatement(node.breakLabel)
        return BoundBlockStatement(listOf(
                gotoCheck,
                continueLabelStatement,
                rewriteBlockStatement(node.body),
                checkLabelStatement,
                rewriteConditionalGotoStatement(gotoTrue),
                breakLabelStatement
        ))
    }

    override fun rewriteConditionalGotoStatement(node: BoundConditionalGotoStatement): BoundStatement {
        val constant = node.condition.constantValue
        if (constant != null) {
            return if (constant.value as Boolean == node.jumpIfTrue) {
                rewriteGotoStatement(BoundGotoStatement(node.label))
            } else {
                BoundNopStatement()
            }
        }
        return super.rewriteConditionalGotoStatement(node)
    }
}