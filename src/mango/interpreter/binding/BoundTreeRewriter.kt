package mango.interpreter.binding

open class BoundTreeRewriter {

    fun rewriteStatement(node: BoundStatement): BoundStatement {
        return when (node.boundType) {
            BoundNodeType.BlockStatement -> rewriteBlockStatement(node as BoundBlockStatement)
            BoundNodeType.ExpressionStatement -> rewriteExpressionStatement(node as BoundExpressionStatement)
            BoundNodeType.VariableDeclaration -> rewriteVariableDeclaration(node as BoundVariableDeclaration)
            BoundNodeType.IfStatement -> rewriteIfStatement(node as BoundIfStatement)
            BoundNodeType.WhileStatement -> rewriteWhileStatement(node as BoundWhileStatement)
            BoundNodeType.ForStatement -> rewriteForStatement(node as BoundForStatement)
            BoundNodeType.LabelStatement -> rewriteLabelStatement(node as BoundLabelStatement)
            BoundNodeType.GotoStatement -> rewriteGotoStatement(node as BoundGotoStatement)
            BoundNodeType.ConditionalGotoStatement -> rewriteConditionalGotoStatement(node as BoundConditionalGotoStatement)
            BoundNodeType.ReturnStatement -> rewriteReturnStatement(node as BoundReturnStatement)
            else -> throw Exception("Unexpected node: ${node.boundType}")
        }
    }

    protected open fun rewriteBlockStatement(node: BoundBlockStatement): BoundBlockStatement {
        var statements: ArrayList<BoundStatement>? = null
        for (i in node.statements.indices) {
            val oldStatement = node.statements.elementAt(i)
            val newStatement = rewriteStatement(oldStatement)
            if (newStatement != oldStatement) {
                if (statements == null) {
                    statements = ArrayList()
                    for (j in 0 until i) {
                        statements.add(node.statements.elementAt(j))
                    }
                }
            }
            statements?.add(newStatement)
        }

        if (statements == null) {
            return node
        }
        return BoundBlockStatement(statements)
    }

    protected open fun rewriteExpressionStatement(node: BoundExpressionStatement): BoundStatement {
        val expression = rewriteExpression(node.expression)
        if (expression == node.expression) {
            return node
        }
        return BoundExpressionStatement(expression)
    }

    protected open fun rewriteVariableDeclaration(node: BoundVariableDeclaration): BoundStatement {
        val initializer = rewriteExpression(node.initializer)
        if (initializer == node.initializer) {
            return node
        }
        return BoundVariableDeclaration(node.variable, initializer)
    }

    protected open fun rewriteIfStatement(node: BoundIfStatement): BoundStatement {
        val condition = rewriteExpression(node.condition)
        val body = rewriteBlockStatement(node.statement)
        val elseStatement = if (node.elseStatement == null) { null } else {
            rewriteStatement(node.elseStatement)
        }
        if (condition == node.condition && body == node.statement && elseStatement == node.elseStatement) {
            return node
        }
        return BoundIfStatement(condition, body, elseStatement)
    }

    protected open fun rewriteWhileStatement(node: BoundWhileStatement): BoundStatement {
        val condition = rewriteExpression(node.condition)
        val body = rewriteBlockStatement(node.body)
        if (condition == node.condition && body == node.body) {
            return node
        }
        return BoundWhileStatement(condition, body, node.breakLabel, node.continueLabel)
    }

    protected open fun rewriteForStatement(node: BoundForStatement): BoundStatement {
        val lowerBound = rewriteExpression(node.lowerBound)
        val upperBound = rewriteExpression(node.upperBound)
        val body = rewriteBlockStatement(node.body)
        if (lowerBound == node.lowerBound && upperBound == node.upperBound && body == node.body) {
            return node
        }
        return BoundForStatement(node.variable, lowerBound, upperBound, body, node.breakLabel, node.continueLabel)
    }

    protected fun rewriteLabelStatement(node: BoundLabelStatement) = node
    protected fun rewriteGotoStatement(node: BoundGotoStatement) = node

    protected fun rewriteConditionalGotoStatement(node: BoundConditionalGotoStatement): BoundStatement {
        val condition = rewriteExpression(node.condition)
        if (condition == node.condition) {
            return node
        }
        return BoundConditionalGotoStatement(node.label, condition, node.jumpIfTrue)
    }

    protected fun rewriteReturnStatement(node: BoundReturnStatement): BoundStatement {
        return BoundReturnStatement(node.expression?.let { rewriteExpression(it) })
    }

    protected fun rewriteExpression(node: BoundExpression): BoundExpression {
        return when (node.boundType) {
            BoundNodeType.UnaryExpression -> rewriteUnaryExpression(node as BoundUnaryExpression)
            BoundNodeType.BinaryExpression -> rewriteBinaryExpression(node as BoundBinaryExpression)
            BoundNodeType.LiteralExpression -> rewriteLiteralExpression(node as BoundLiteralExpression)
            BoundNodeType.VariableExpression -> rewriteVariableExpression(node as BoundVariableExpression)
            BoundNodeType.AssignmentExpression -> rewriteAssignmentExpression(node as BoundAssignmentExpression)
            BoundNodeType.CallExpression -> rewriteCallExpression(node as BoundCallExpression)
            BoundNodeType.CastExpression -> rewriteCastExpression(node as BoundCastExpression)
            BoundNodeType.ErrorExpression -> node
            else -> throw Exception("Unexpected node: ${node.boundType}")
        }
    }

    protected open fun rewriteLiteralExpression(node: BoundLiteralExpression) = node
    protected open fun rewriteVariableExpression(node: BoundVariableExpression) = node

    protected open fun rewriteUnaryExpression(node: BoundUnaryExpression): BoundExpression {
        val operand = rewriteExpression(node.operand)
        if (operand == node.operand) {
            return node
        }
        return BoundUnaryExpression(node.operator, operand)
    }

    protected open fun rewriteBinaryExpression(node: BoundBinaryExpression): BoundExpression {
        val left = rewriteExpression(node.left)
        val right = rewriteExpression(node.right)
        if (left == node.left && right == node.right) {
            return node
        }
        return BoundBinaryExpression(left, node.operator, right)
    }

    protected open fun rewriteAssignmentExpression(node: BoundAssignmentExpression): BoundExpression {
        val expression = rewriteExpression(node.expression)
        if (expression == node.expression) {
            return node
        }
        return BoundAssignmentExpression(node.variable, expression)
    }

    protected fun rewriteCallExpression(node: BoundCallExpression): BoundExpression {
        var args: ArrayList<BoundExpression>? = null
        for (i in node.arguments.indices) {
            val oldArgument = node.arguments.elementAt(i)
            val newArgument = rewriteExpression(oldArgument)
            if (newArgument != oldArgument) {
                if (args == null) {
                    args = ArrayList()
                    for (j in 0 until i) {
                        args.add(node.arguments.elementAt(j))
                    }
                }
            }
            args?.add(newArgument)
        }

        if (args == null) {
            return node
        }
        return BoundCallExpression(node.function, args)
    }

    protected fun rewriteCastExpression(node: BoundCastExpression): BoundExpression {
        val expression = rewriteExpression(node.expression)
        if (expression == node.expression) {
            return node
        }
        return BoundCastExpression(node.type, expression)
    }
}