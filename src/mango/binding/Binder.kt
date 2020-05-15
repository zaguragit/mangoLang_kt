package mango.binding

import mango.compilation.DiagnosticList
import mango.syntax.SyntaxType
import mango.syntax.parser.*
import java.util.*
import kotlin.Exception
import kotlin.collections.ArrayList

class Binder(
    var scope: BoundScope
) {

    val diagnostics = DiagnosticList()

    private fun bindStatement(node: StatementNode): BoundStatement {
        return when (node.kind) {
            SyntaxType.BlockStatement -> bindBlockStatement(node as BlockStatementNode)
            SyntaxType.ExpressionStatement -> bindExpressionStatement(node as ExpressionStatementNode)
            SyntaxType.VariableDeclaration -> bindVariableDeclaration(node as VariableDeclarationNode)
            SyntaxType.IfStatement -> bindIfStatement(node as IfStatementNode)
            SyntaxType.WhileStatement -> bindWhileStatement(node as WhileStatementNode)
            SyntaxType.ForStatement -> bindForStatement(node as ForStatementNode)
            else -> throw Exception("Unexpected node: ${node.kind}")
        }
    }

    private fun bindIfStatement(node: IfStatementNode): BoundIfStatement {
        val condition = bindExpression(node.condition, Type.Bool)
        val statement = bindBlockStatement(node.thenStatement)
        val elseStatement = node.elseClause?.let { bindStatement(it.statement) }
        return BoundIfStatement(condition, statement, elseStatement)
    }

    private fun bindWhileStatement(node: WhileStatementNode): BoundWhileStatement {
        val condition = bindExpression(node.condition, Type.Bool)
        val body = bindBlockStatement(node.body)
        return BoundWhileStatement(condition, body)
    }

    private fun bindForStatement(node: ForStatementNode): BoundForStatement {
        val lowerBound = bindExpression(node.lowerBound, Type.Int)
        val upperBound = bindExpression(node.upperBound, Type.Int)

        val previous = scope
        scope = BoundScope(scope)
        val name = node.identifier.string ?: ""
        val variable = VariableSymbol(name, Type.Int, true)
        scope.tryDeclare(variable)
        val body = bindBlockStatement(node.body)
        scope = previous

        return BoundForStatement(variable, lowerBound, upperBound, body)
    }

    private fun bindBlockStatement(node: BlockStatementNode): BoundBlockStatement {
        val statements = ArrayList<BoundStatement>()
        val previous = scope
        scope = BoundScope(scope)
        for (s in node.statements) {
            val statement = bindStatement(s)
            statements.add(statement)
        }
        scope = previous
        return BoundBlockStatement(statements)
    }

    private fun bindExpressionStatement(node: ExpressionStatementNode): BoundExpressionStatement {
        val expression = bindExpression(node.expression)
        return BoundExpressionStatement(expression)
    }

    private fun bindVariableDeclaration(node: VariableDeclarationNode): BoundStatement {
        val name = node.identifier.string ?: ""
        val isReadOnly = node.keyword.kind == SyntaxType.Val
        val initializer = bindExpression(node.initializer)
        val variable = VariableSymbol(name, initializer.type, isReadOnly)

        val result = scope.tryDeclare(variable)
        if (!result) {
            diagnostics.reportVarAlreadyDeclared(node.identifier.span, name)
        }
        return BoundVariableDeclaration(variable, initializer)
    }

    private fun bindExpression(node: ExpressionNode): BoundExpression {
        return when (node.kind) {
            SyntaxType.ParenthesizedExpression -> bindParenthesizedExpression(node as ParenthesizedExpressionNode)
            SyntaxType.LiteralExpression -> bindLiteralExpression(node as LiteralExpressionNode)
            SyntaxType.UnaryExpression -> bindUnaryExpression(node as UnaryExpressionNode)
            SyntaxType.BinaryExpression -> bindBinaryExpression(node as BinaryExpressionNode)
            SyntaxType.NameExpression -> bindNameExpression(node as NameExpressionNode)
            SyntaxType.AssignmentExpression -> bindAssignmentExpression(node as AssignmentExpressionNode)
            else -> throw Exception("Unexpected node: ${node.kind}")
        }
    }

    private fun bindExpression(node: ExpressionNode, type: Type): BoundExpression {
        val result = bindExpression(node)
        if (result.type != type) {
            diagnostics.reportWrongType(node.span, result, type)
        }
        return result
    }

    private fun bindAssignmentExpression(node: AssignmentExpressionNode): BoundExpression {
        val name = node.identifierToken.string ?: return BoundLiteralExpression(0, Type.Int)
        val boundExpression = bindExpression(node.expression)
        val result = scope.tryLookup(name)
        if (!result.second) {
            diagnostics.reportUndefinedName(node.identifierToken.span, name)
            return boundExpression
        }
        val variable = result.first!!
        if (variable.isReadOnly) {
            diagnostics.reportVarIsImmutable(node.equalsToken.span, name)
            return boundExpression
        }
        if (variable.type != boundExpression.type) {
            diagnostics.reportWrongType(node.identifierToken.span, name, boundExpression.type)
            return boundExpression
        }
        return BoundAssignmentExpression(variable, boundExpression)
    }

    private fun bindNameExpression(node: NameExpressionNode): BoundExpression {
        val name = node.identifierToken.string ?: return BoundLiteralExpression(0, Type.Int)
        val result = scope.tryLookup(name)
        if (!result.second) {
            diagnostics.reportUndefinedName(node.identifierToken.span, name)
            return BoundLiteralExpression(0, Type.Int)
        }
        return BoundVariableExpression(result.first!!)
    }

    private fun bindParenthesizedExpression(
            node: ParenthesizedExpressionNode
    ) = bindExpression(node.expression)

    private fun bindLiteralExpression(node: LiteralExpressionNode): BoundExpression {
        return when (val value = node.value) {
            is Int -> BoundLiteralExpression(value, Type.Int)
            is Boolean -> BoundLiteralExpression(value, Type.Bool)
            else -> BoundLiteralExpression(value, Type.Int)
        }
    }

    private fun bindUnaryExpression(node: UnaryExpressionNode): BoundExpression {
        val operand = bindExpression(node.operand)
        val operator = BoundUnaryOperator.bind(node.operator.kind, operand.type)
        if (operator == null) {
            diagnostics.reportUnaryOperator(node.operator.span, node.operator.kind, operand.type)
            return operand
        }
        return BoundUnaryExpression(operator, operand)
    }

    private fun bindBinaryExpression(node: BinaryExpressionNode): BoundExpression {
        val left = bindExpression(node.left)
        val right = bindExpression(node.right)
        val operator = BoundBinaryOperator.bind(node.operator.kind, left.type, right.type)
        if (operator == null) {
            diagnostics.reportBinaryOperator(node.operator.span, left.type, node.operator.kind, right.type)
            return left
        }
        return BoundBinaryExpression(left, operator, right)
    }

    companion object {
        fun bindGlobalScope(fileUnit: FileUnit, previous: BoundGlobalScope?): BoundGlobalScope {
            val parentScope = createParentScopes(previous)
            val binder = Binder(BoundScope(parentScope))
            val expression = binder.bindStatement(fileUnit.statementNode)
            val variables = binder.scope.variables
            val diagnostics = binder.diagnostics.list.toMutableList()
            if (previous != null) {
                diagnostics.addAll(previous.diagnostics)
            }
            return BoundGlobalScope(previous, diagnostics, variables, expression)
        }

        fun createParentScopes(previous: BoundGlobalScope?): BoundScope? {
            val stack = Stack<BoundGlobalScope>()
            var prev = previous
            while (prev != null) {
                stack.push(prev)
                prev = prev.previous
            }

            var parent: BoundScope? = null

            while (stack.count() > 0) {
                val previous = stack.pop()
                val scope = BoundScope(parent)
                for (v in previous.variables) {
                    scope.tryDeclare(v)
                }
                parent = scope
            }

            return parent
        }
    }
}