package mango.compiler.binding

import mango.compiler.binding.nodes.BoundNode
import mango.compiler.binding.nodes.expressions.*
import mango.compiler.binding.nodes.statements.*
import mango.compiler.symbols.TypeSymbol

interface TreeRewriter {

    fun rewriteStatement(node: Statement): Statement {
        return when (node.kind) {
            BoundNode.Kind.ExpressionStatement -> rewriteExpressionStatement(node as ExpressionStatement)
            BoundNode.Kind.ValVarDeclaration -> rewriteValVarDeclaration(node as ValVarDeclaration)
            BoundNode.Kind.LoopStatement -> rewriteLoopStatement(node as LoopStatement)
            BoundNode.Kind.ForLoopStatement -> rewriteForStatement(node as ForStatement)
            BoundNode.Kind.LabelStatement -> rewriteLabelStatement(node as LabelStatement)
            BoundNode.Kind.GotoStatement -> rewriteGotoStatement(node as Goto)
            BoundNode.Kind.ConditionalGotoStatement -> rewriteConditionalGotoStatement(node as ConditionalGoto)
            BoundNode.Kind.ReturnStatement -> rewriteReturnStatement(node as ReturnStatement)
            BoundNode.Kind.AssignmentStatement -> rewriteAssignmentStatement(node as Assignment)
            BoundNode.Kind.PointerAccessAssignment -> rewritePointerAccessAssignment(node as PointerAccessAssignment)
            BoundNode.Kind.NopStatement -> NopStatement
            else -> throw Binder.BinderError("Unexpected node: ${node.kind}")
        }
    }

    fun rewriteExpressionStatement(node: ExpressionStatement): Statement {
        val expression = rewriteExpression(node.expression)
        if (expression == node.expression) {
            return node
        }
        return ExpressionStatement(expression)
    }

    fun rewriteValVarDeclaration(node: ValVarDeclaration): Statement {
        val initializer = rewriteExpression(node.initializer)
        if (initializer == node.initializer) {
            return node
        }
        return ValVarDeclaration(node.variable, initializer)
    }

    fun rewriteLoopStatement(node: LoopStatement): Statement {
        val body = rewriteStatement(node.body)
        if (body == node.body) {
            return node
        }
        return LoopStatement(body, node.breakLabel, node.continueLabel)
    }

    fun rewriteForStatement(node: ForStatement): Statement {
        val lowerBound = rewriteExpression(node.lowerBound)
        val upperBound = rewriteExpression(node.upperBound)
        val body = rewriteStatement(node.body)
        if (lowerBound == node.lowerBound && upperBound == node.upperBound && body == node.body) {
            return node
        }
        return ForStatement(node.variable, lowerBound, upperBound, body, node.breakLabel, node.continueLabel)
    }

    fun rewriteLabelStatement(node: LabelStatement) = node
    fun rewriteGotoStatement(node: Goto) = node

    fun rewriteConditionalGotoStatement(node: ConditionalGoto): Statement {
        val condition = rewriteExpression(node.condition)
        if (condition == node.condition) {
            return node
        }
        return ConditionalGoto(node.label, condition, node.jumpIfTrue)
    }

    fun rewriteReturnStatement(node: ReturnStatement): Statement {
        return ReturnStatement(node.expression?.let { rewriteExpression(it) })
    }

    fun rewriteExpression(node: Expression) = when (node.kind) {
        BoundNode.Kind.UnaryExpression -> rewriteUnaryExpression(node as UnaryExpression)
        BoundNode.Kind.BinaryExpression -> rewriteBinaryExpression(node as BinaryExpression)
        BoundNode.Kind.LiteralExpression -> rewriteLiteralExpression(node as LiteralExpression)
        BoundNode.Kind.NameExpression -> rewriteNameExpression(node as NameExpression)
        BoundNode.Kind.CallExpression -> rewriteCallExpression(node as CallExpression)
        BoundNode.Kind.CastExpression -> rewriteCastExpression(node as CastExpression)
        BoundNode.Kind.ErrorExpression -> node
        BoundNode.Kind.StructFieldAccess -> rewriteStructFieldAccess(node as StructFieldAccess)
        BoundNode.Kind.BlockExpression -> rewriteBlockExpression(node as BlockExpression)
        BoundNode.Kind.ReferenceExpression -> rewriteReferenceExpression(node as Reference)
        BoundNode.Kind.PointerAccessExpression -> rewritePointerAccessExpression(node as PointerAccess)
        BoundNode.Kind.StructInitialization -> rewriteStructInitialization(node as StructInitialization)
        BoundNode.Kind.PointerArrayInitialization -> rewritePointerArrayInitialization(node as PointerArrayInitialization)
        BoundNode.Kind.IfExpression -> rewriteIfExpression(node as IfExpression)
        BoundNode.Kind.Lambda -> rewriteLambdaExpression(node as Lambda)
        else -> throw Binder.BinderError("Unexpected node: ${node.kind}")
    }

    fun rewriteLiteralExpression(node: LiteralExpression) = node
    fun rewriteNameExpression(node: NameExpression) = node

    fun rewriteUnaryExpression(node: UnaryExpression): Expression {
        val operand = rewriteExpression(node.operand)
        if (operand == node.operand) {
            return node
        }
        return UnaryExpression(node.operator, operand)
    }

    fun rewriteBinaryExpression(node: BinaryExpression): Expression {
        val left = rewriteExpression(node.left)
        val right = rewriteExpression(node.right)
        if (left == node.left && right == node.right) {
            return node
        }
        return BinaryExpression(left, node.operator, right)
    }

    fun rewriteAssignmentStatement(node: Assignment): Statement {
        val expression = rewriteExpression(node.expression)
        val assignee = rewriteExpression(node.assignee)
        if (expression == node.expression && assignee == node.assignee) {
            return node
        }
        return Assignment(assignee, expression)
    }

    fun rewritePointerAccessAssignment(node: PointerAccessAssignment): Statement {
        val expression = rewriteExpression(node.expression)
        val i = rewriteExpression(node.i)
        val value = rewriteExpression(node.value)
        if (expression == node.expression && i == node.i && value == node.value) {
            return node
        }
        return PointerAccessAssignment(expression, i, value)
    }

    fun rewriteCallExpression(node: CallExpression): Expression {
        var args: ArrayList<Expression>? = null
        for (i in node.arguments.indices) {
            val oldArgument = node.arguments.elementAt(i)
            val newArgument = rewriteExpression(oldArgument)
            if (args == null) {
                if (newArgument != oldArgument) {
                    args = ArrayList()
                    for (j in 0..i) {
                        args.add(node.arguments.elementAt(j))
                    }
                }
            } else {
                args.add(newArgument)
            }
        }
        val expression = rewriteExpression(node.expression)
        if (args == null && expression == node.expression) {
            return node
        }
        return CallExpression(node.expression, args ?: node.arguments)
    }

    fun rewriteCastExpression(node: CastExpression): Expression {
        val expression = rewriteExpression(node.expression)
        if (expression == node.expression) {
            return node
        }
        return CastExpression(node.type, expression)
    }

    fun rewriteStructFieldAccess(node: StructFieldAccess): Expression {
        val struct = rewriteExpression(node.struct)
        if (struct == node.struct) {
            return node
        }
        return StructFieldAccess(struct, node.i)
    }

    fun rewriteBlockExpression(node: BlockExpression): Expression {
        var statements: ArrayList<Statement>? = null
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
        return BlockExpression(statements, node.type)
    }

    fun rewriteReferenceExpression(node: Reference): Expression {
        val expression = rewriteNameExpression(node.expression)
        if (expression == node.expression) {
            return node
        }
        return Reference(expression)
    }

    fun rewritePointerAccessExpression(node: PointerAccess): Expression {
        val expression = rewriteExpression(node.expression)
        val i = rewriteExpression(node.i)
        if (expression == node.expression && i == node.i) {
            return node
        }
        return PointerAccess(expression, i)
    }

    fun rewriteStructInitialization(node: StructInitialization): Expression {
        val map = HashMap<TypeSymbol.StructTypeSymbol.Field, Expression>()
        var wasChanged = false
        for (entry in node.fields) {
            val e = rewriteExpression(entry.value)
            map[entry.key] = e
            if (e != entry.value) {
                wasChanged = true
            }
        }
        return if (wasChanged) StructInitialization(node.type, map) else node
    }

    fun rewritePointerArrayInitialization(node: PointerArrayInitialization): Expression {
        var list: ArrayList<Expression>? = null
        var wasChanged = false
        if (node.expressions != null) {
            list = ArrayList()
            for (e in node.expressions) {
                val expression = rewriteExpression(e)
                list.add(expression)
                if (e != expression) {
                    wasChanged = true
                }
            }
        }
        var length: Expression? = null
        if (node.length != null) {
            length = rewriteExpression(node.length)
            if (length != node.length) {
                wasChanged = true
            }
        }
        return if (wasChanged) return PointerArrayInitialization(node.type, length, list) else node
    }

    fun rewriteIfExpression(node: IfExpression): Expression {
        val condition = rewriteExpression(node.condition)
        val body = rewriteExpression(node.thenExpression)
        val elseStatement = if (node.elseExpression == null) { null } else {
            rewriteExpression(node.elseExpression)
        }
        if (condition == node.condition && body == node.thenExpression && elseStatement == node.elseExpression) {
            return node
        }
        return IfExpression(condition, body, elseStatement)
    }

    fun rewriteLambdaExpression(node: Lambda): Expression {
        return node
    }
}