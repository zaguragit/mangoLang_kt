package mango.compiler.ir

import mango.compiler.binding.TreeRewriter
import mango.compiler.binding.nodes.BiOperator
import mango.compiler.binding.nodes.expressions.*
import mango.compiler.binding.nodes.statements.*
import mango.compiler.symbols.TypeSymbol
import mango.compiler.symbols.VariableSymbol
import mango.parser.SyntaxType

class Lowerer private constructor() : TreeRewriter {

    companion object {
        fun lower(statement: Statement): BlockExpression {
            return BlockExpression(Lowerer().lower(statement), TypeSymbol.Void)
        }

        private fun removeDeadCode(block: Collection<Statement>): List<Statement> {
            val controlFlow = ControlFlowGraph.create(block)
            val reachableStatements = controlFlow.blocks.flatMap { it.statements }.toHashSet()
            val builder = block.toMutableList()
            for (i in builder.lastIndex downTo 0) {
                if (!reachableStatements.contains(builder[i])) {
                    builder.removeAt(i)
                }
            }
            return builder
        }
    }

    private var labelCount = 0
    private fun generateLabel(): Label {
        val name = "L${labelCount++.toString(16)}"
        return Label(name)
    }

    private val statements = ArrayList<Statement>()
    private val variableNames = HashSet<String>()

    fun lower(statement: Statement): List<Statement> {
        val block = if (statement is ExpressionStatement && statement.expression.type != TypeSymbol.Void) {
            ExpressionStatement(BlockExpression(listOf(ReturnStatement(statement.expression)), TypeSymbol.Void))
        } else statement
        val result = rewriteStatement(block)
        statements.add(result)
        return removeDeadCode(statements)
    }

    override fun rewriteForStatement(node: ForStatement): Statement {
        val variableDeclaration = ValVarDeclaration(node.variable, node.lowerBound)
        val variableExpression = NameExpression(node.variable)
        val upperBoundSymbol = VariableSymbol.local(".loop_max", TypeSymbol.Int, true, node.upperBound.constantValue)
        val upperBoundDeclaration = ValVarDeclaration(upperBoundSymbol, node.upperBound)
        val condition = BinaryExpression(
            variableExpression,
            BiOperator.bind(SyntaxType.IsEqualOrLess, TypeSymbol.Int, TypeSymbol.Int)!!,
            NameExpression(upperBoundSymbol)
        )
        val continueLabelStatement = LabelStatement(node.continueLabel)
        val breakLabelStatement = LabelStatement(node.breakLabel)
        val increment = Assignment(
            NameExpression(node.variable),
            BinaryExpression(
                variableExpression,
                BiOperator.bind(SyntaxType.Plus, TypeSymbol.Int, TypeSymbol.Int)!!,
                LiteralExpression(1, TypeSymbol.I32)
            )
        )
        val body = ExpressionStatement(BlockExpression(listOf(
            continueLabelStatement,
            ConditionalGoto(node.breakLabel, condition, false),
            node.body,
            increment,
            Goto(node.continueLabel),
            breakLabelStatement
        ), TypeSymbol.Void))
        val result = ExpressionStatement(BlockExpression(listOf(variableDeclaration, upperBoundDeclaration, body), TypeSymbol.Void))
        return rewriteStatement(result)
    }

    override fun rewriteLoopStatement(node: LoopStatement): Statement {
        val continueLabelStatement = LabelStatement(node.continueLabel)
        val goto = Goto(node.continueLabel)
        val breakLabelStatement = LabelStatement(node.breakLabel)
        return rewriteStatement(ExpressionStatement(BlockExpression(listOf(
            continueLabelStatement,
            node.body,
            goto,
            breakLabelStatement
        ), TypeSymbol.Void)))
    }

    override fun rewriteIfExpression(node: IfExpression): Expression {

        val endLabel = generateLabel()
        val endLabelStatement = LabelStatement(endLabel)

        if (node.elseExpression == null) {
            val gotoFalse = ConditionalGoto(endLabel, node.condition, false)
            val thenStatement = ExpressionStatement(node.thenExpression)
            val result = BlockExpression(listOf(gotoFalse, thenStatement, endLabelStatement), TypeSymbol.Void)
            return rewriteExpression(result)
        }

        val elseLabel = generateLabel()
        val elseLabelStatement = LabelStatement(elseLabel)

        val gotoFalse = ConditionalGoto(elseLabel, node.condition, false)
        val gotoEnd = Goto(endLabel)

        val isActuallyExpression = node.type != TypeSymbol.Void

        val varDeclaration = if (isActuallyExpression) ValVarDeclaration(VariableSymbol.local(".tmp", node.type, false, null), run {
            LiteralExpression.nullEquivalent(node.type)
        }) else null

        val t = node.thenExpression
        val e = node.elseExpression
        val thenStatement: Statement
        val elseStatement: Statement
        if (isActuallyExpression) {
            thenStatement = Assignment(NameExpression(varDeclaration!!.variable), t)
            elseStatement = Assignment(NameExpression(varDeclaration.variable), e)
        } else {
            thenStatement = ExpressionStatement(t)
            elseStatement = ExpressionStatement(e)
        }

        val statements = arrayListOf(
            gotoFalse,
            thenStatement,
            gotoEnd,
            elseLabelStatement,
            elseStatement,
            endLabelStatement
        )

        if (isActuallyExpression) {
            statements.add(0, varDeclaration!!)
            statements.add(ExpressionStatement(NameExpression(varDeclaration.variable)))
        }

        return rewriteExpression(BlockExpression(statements, TypeSymbol.Void))
    }

    override fun rewriteConditionalGotoStatement(node: ConditionalGoto): Statement {
        val constant = node.condition.constantValue
        if (constant != null) {
            return if (constant.value as Boolean == node.jumpIfTrue) {
                rewriteGotoStatement(Goto(node.label))
            } else NopStatement
        }
        val condition = rewriteExpression(node.condition)
        if (condition == node.condition) {
            return node
        }
        return ConditionalGoto(node.label, condition, node.jumpIfTrue)
    }

    override fun rewriteLambdaExpression(node: Lambda): Expression {
        return NameExpression(node.symbol)
    }

    override fun rewriteExpressionStatement(node: ExpressionStatement): Statement {
        val expression = rewriteExpression(node.expression)
        if (expression is BlockExpression) {
            return NopStatement
        }
        if (expression == node.expression) {
            return node
        }
        return ExpressionStatement(expression)
    }

    override fun rewriteValVarDeclaration(node: ValVarDeclaration): Statement {
        if (!variableNames.add(node.variable.realName)) {
            val r = node.variable.realName
            var string = r
            var i = 1
            while (!variableNames.add(string)) {
                string = ".${i++}_$string"
            }
            node.variable.realName = string
        }
        val initializer = rewriteExpression(node.initializer)
        if (initializer == node.initializer) {
            return node
        }
        return ValVarDeclaration(node.variable, initializer)
    }

    override fun rewriteExpression(node: Expression): Expression {
        val c = node.constantValue
        if (c != null) {
            return LiteralExpression(c.value, node.type)
        }
        return super.rewriteExpression(node)
    }

    override fun rewriteCastExpression(node: CastExpression): Expression {
        val expression = rewriteExpression(node.expression)
        if (expression == node.expression) {
            return node
        }
        return CastExpression(node.type, expression)
    }

    override fun rewriteBlockExpression(node: BlockExpression): Expression {
        var result: Expression? = null
        println()
        for (i in node.statements.indices) {
            val s = node.statements.elementAt(i)
            val n = rewriteStatement(s)
            if (i == node.statements.size - 1 && n is ExpressionStatement) {
                result = n.expression
                continue
            }
            statements.add(n)
        }

        return result ?: node
    }
}