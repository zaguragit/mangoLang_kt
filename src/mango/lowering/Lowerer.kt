package mango.lowering

import mango.binding.*
import mango.symbols.LocalVariableSymbol
import mango.symbols.TypeSymbol
import mango.symbols.VariableSymbol
import mango.syntax.SyntaxType
import java.util.*
import kotlin.collections.ArrayList

class Lowerer private constructor() : BoundTreeRewriter() {

    private var labelCount = 0

    private fun generateLabel(): BoundLabel {
        val name = "label_${(++labelCount).toString(16)}"
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
        val increment = BoundExpressionStatement(BoundAssignmentExpression(
            node.variable,
            BoundBinaryExpression(
                variableExpression,
                BoundBinaryOperator.bind(SyntaxType.Plus, TypeSymbol.int, TypeSymbol.int)!!,
                BoundLiteralExpression(1)
            )
        ))
        val body = BoundBlockStatement(listOf(node.body, increment))
        val whileStatement = BoundWhileStatement(condition, body)
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
        val continueLabel = generateLabel()
        val checkLabel = generateLabel()
        val endLabel = generateLabel()
        val gotoCheck = BoundGotoStatement(checkLabel)
        val continueLabelStatement = BoundLabelStatement(continueLabel)
        val checkLabelStatement = BoundLabelStatement(checkLabel)
        val gotoTrue = BoundConditionalGotoStatement(continueLabel, node.condition, true)
        val endLabelStatement = BoundLabelStatement(endLabel)
        return BoundBlockStatement(listOf(
                gotoCheck,
                continueLabelStatement,
                node.body,
                checkLabelStatement,
                gotoTrue,
                endLabelStatement
        ))
    }

    companion object {
        fun lower(statement: BoundStatement): BoundBlockStatement {
            val lowerer = Lowerer()
            val result = lowerer.rewriteStatement(statement)
            return lowerer.flatten(result)
        }
    }
}