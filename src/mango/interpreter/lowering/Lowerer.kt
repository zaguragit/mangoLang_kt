package mango.interpreter.lowering

import mango.interpreter.binding.*
import mango.interpreter.symbols.LocalVariableSymbol
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.syntax.SyntaxType
import java.util.*
import kotlin.collections.ArrayList

class Lowerer private constructor() : BoundTreeRewriter() {

    private var labelCount = 0

    private fun generateLabel(): BoundLabel {
        val name = "L${(++labelCount).toString(16)}"
        return BoundLabel(name)
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

    override fun rewriteForStatement(node: BoundForStatement): BoundStatement {
        val variableDeclaration = BoundVariableDeclaration(node.variable, node.lowerBound)
        val variableExpression = BoundVariableExpression(node.variable)
        val upperBoundSymbol = LocalVariableSymbol("0upperBound", TypeSymbol.int, true)
        val upperBoundDeclaration = BoundVariableDeclaration(upperBoundSymbol, node.upperBound)
        val condition = BoundBinaryExpression(
            variableExpression,
            BoundBinaryOperator.bind(SyntaxType.IsEqualOrLess, TypeSymbol.int, TypeSymbol.int)!!,
            BoundVariableExpression(upperBoundSymbol)
        )
        val continueLabelStatement = BoundLabelStatement(node.continueLabel)
        val increment = BoundExpressionStatement(BoundAssignmentExpression(
            node.variable,
            BoundBinaryExpression(
                variableExpression,
                BoundBinaryOperator.bind(SyntaxType.Plus, TypeSymbol.int, TypeSymbol.int)!!,
                BoundLiteralExpression(1)
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
                gotoTrue,
                breakLabelStatement
        ))
    }

    companion object {
        fun lower(expression: BoundExpression): BoundBlockStatement {
            val lowerer = Lowerer()
            val block = BoundBlockStatement(listOf(BoundReturnStatement(expression)))
            val result = lowerer.rewriteStatement(block)
            return lowerer.flatten(result)
        }
        fun lower(block: BoundBlockStatement): BoundBlockStatement {
            val lowerer = Lowerer()
            val result = lowerer.rewriteBlockStatement(block)
            return lowerer.flatten(result)
        }
    }
}