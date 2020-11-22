package mango.interpreter.binding

import mango.interpreter.binding.nodes.BiOperator
import mango.interpreter.binding.nodes.BoundNode
import mango.interpreter.binding.nodes.expressions.*
import mango.interpreter.binding.nodes.statements.*
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.symbols.VariableSymbol
import mango.interpreter.syntax.SyntaxType

class Lowerer : TreeRewriter() {

    companion object {
        fun lower(statement: Statement): BlockExpression {
            val lowerer = Lowerer()
            val block = if (statement is ExpressionStatement && statement.expression.type != TypeSymbol.Unit) {
                ExpressionStatement(BlockExpression(listOf(ReturnStatement(statement.expression)), TypeSymbol.Unit))
            } else statement
            val result = lowerer.rewriteStatement(block)
            return removeDeadCode(flatten(result))
        }

        private fun flatten(statement: Statement): BlockExpression {
            val arrayList = ArrayList<Statement>()
            val variableNames = HashSet<String>()
            flattenStatement(statement, arrayList, variableNames)
            return BlockExpression(arrayList, TypeSymbol.Unit)
        }

        private fun flattenStatement(statement: Statement, arrayList: ArrayList<Statement>, variableNames: HashSet<String>) {
            when (statement.kind) {
                BoundNode.Kind.ExpressionStatement -> {
                    statement as ExpressionStatement
                    val a = flattenExpression(statement.expression, arrayList, variableNames)
                    if (a !is BlockExpression) {
                        arrayList.add(ExpressionStatement(a))
                    }
                }
                BoundNode.Kind.VariableDeclaration -> {
                    statement as VariableDeclaration
                    if (!variableNames.add(statement.variable.realName)) {
                        var string = statement.variable.realName
                        while (!variableNames.add(string)) {
                            string = ".l_$string"
                        }
                        statement.variable.realName = string
                    }
                    arrayList.add(VariableDeclaration(
                        statement.variable,
                        flattenExpression(statement.initializer, arrayList, variableNames)))
                }
                BoundNode.Kind.ConditionalGotoStatement -> {
                    statement as ConditionalGotoStatement
                    arrayList.add(ConditionalGotoStatement(
                        statement.label,
                        flattenExpression(statement.condition, arrayList, variableNames),
                        statement.jumpIfTrue))
                }
                BoundNode.Kind.ReturnStatement -> {
                    statement as ReturnStatement
                    arrayList.add(ReturnStatement(statement.expression?.let {
                        flattenExpression(it, arrayList, variableNames)
                    }))
                }
                BoundNode.Kind.AssignmentStatement -> {
                    statement as Assignment
                    arrayList.add(Assignment(
                        flattenExpression(statement.assignee, arrayList, variableNames),
                        flattenExpression(statement.expression, arrayList, variableNames)))
                }
                BoundNode.Kind.PointerAccessAssignment -> {
                    statement as PointerAccessAssignment
                    arrayList.add(PointerAccessAssignment(
                        flattenExpression(statement.expression, arrayList, variableNames),
                        flattenExpression(statement.i, arrayList, variableNames),
                        flattenExpression(statement.value, arrayList, variableNames)))
                }
                else -> arrayList.add(statement)
            }
        }

        private fun flattenExpression(expression: Expression, arrayList: ArrayList<Statement>, variableNames: HashSet<String>): Expression {
            return when (expression.kind) {
                BoundNode.Kind.UnaryExpression -> {
                    expression as UnaryExpression
                    UnaryExpression(expression.operator, flattenExpression(expression.operand, arrayList, variableNames))
                }
                BoundNode.Kind.BinaryExpression -> {
                    expression as BinaryExpression
                    BinaryExpression(flattenExpression(expression.left, arrayList, variableNames), expression.operator, flattenExpression(expression.right, arrayList, variableNames))
                }
                BoundNode.Kind.CallExpression -> {
                    expression as CallExpression
                    CallExpression(expression.expression, expression.arguments.map { flattenExpression(it, arrayList, variableNames) })
                }
                BoundNode.Kind.CastExpression -> {
                    expression as CastExpression
                    CastExpression(expression.type, flattenExpression(expression.expression, arrayList, variableNames))
                }
                BoundNode.Kind.StructInitialization -> {
                    expression as StructInitialization
                    StructInitialization(expression.type, expression.fields.mapValues { flattenExpression(it.value, arrayList, variableNames) })
                }
                BoundNode.Kind.BlockExpression -> {
                    expression as BlockExpression
                    var result: Expression? = null
                    loop@ for (i in expression.statements.indices) {
                        val s = expression.statements.elementAt(i)
                        if (i == expression.statements.size - 1 && s is ExpressionStatement) {
                            result = flattenExpression(s.expression, arrayList, variableNames)
                            continue@loop
                        }
                        flattenStatement(s, arrayList, variableNames)
                    }
                    result ?: expression
                }
                else -> expression
            }
        }

        private fun removeDeadCode(block: BlockExpression): BlockExpression {
            val controlFlow = ControlFlowGraph.create(block)
            val reachableStatements = controlFlow.blocks.flatMap { it.statements }.toHashSet()
            val builder = block.statements.toMutableList()
            for (i in builder.lastIndex downTo 0) {
                if (!reachableStatements.contains(builder[i])) {
                    builder.removeAt(i)
                }
            }
            return BlockExpression(builder, TypeSymbol.Unit)
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
        val upperBoundSymbol = VariableSymbol.local(".upperBound", TypeSymbol.Int, true, node.upperBound.constantValue)
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
        val body = ExpressionStatement(BlockExpression(listOf(
            ConditionalGotoStatement(node.breakLabel, condition, false),
            node.body,
            continueLabelStatement,
            increment
        ), TypeSymbol.Unit))
        val whileStatement = LoopStatement(body, node.breakLabel, generateLabel())
        val result = ExpressionStatement(BlockExpression(listOf(variableDeclaration, upperBoundDeclaration, whileStatement), TypeSymbol.Unit))
        return rewriteStatement(result)
    }

    override fun rewriteIfStatement(node: IfStatement): Statement {

        if (node.elseStatement == null) {
            val endLabel = generateLabel()
            val gotoFalse = ConditionalGotoStatement(endLabel, node.condition, false)
            val endLabelStatement = LabelStatement(endLabel)
            val result = ExpressionStatement(BlockExpression(listOf(gotoFalse, node.statement, endLabelStatement), TypeSymbol.Unit))
            return rewriteStatement(result)
        }

        val elseLabel = generateLabel()
        val endLabel = generateLabel()
        val gotoFalse = ConditionalGotoStatement(elseLabel, node.condition, false)
        val gotoEnd = GotoStatement(endLabel)
        val elseLabelStatement = LabelStatement(elseLabel)
        val endLabelStatement = LabelStatement(endLabel)
        val result = ExpressionStatement(BlockExpression(listOf(
            gotoFalse,
            node.statement,
            gotoEnd,
            elseLabelStatement,
            node.elseStatement,
            endLabelStatement
        ), TypeSymbol.Unit))
        return rewriteStatement(result)
    }

    override fun rewriteWhileStatement(node: LoopStatement): Statement {
        val continueLabelStatement = LabelStatement(node.continueLabel)
        val gotoTrue = GotoStatement(node.continueLabel)
        val breakLabelStatement = LabelStatement(node.breakLabel)
        return ExpressionStatement(BlockExpression(listOf(
            continueLabelStatement,
            rewriteStatement(node.body),
            gotoTrue,
            breakLabelStatement
        ), TypeSymbol.Unit))
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