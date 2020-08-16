package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.interpreter.binding.nodes.*
import mango.interpreter.binding.nodes.expressions.*
import mango.interpreter.binding.nodes.statements.*
import mango.interpreter.text.TextSpan
import mango.interpreter.symbols.*
import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.Translator
import mango.interpreter.syntax.nodes.*
import mango.interpreter.text.TextLocation
import mango.isRepl
import mango.isSharedLib
import mango.util.BinderError
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class Binder(
    var scope: BoundScope,
    val function: FunctionSymbol?,
    val functions: HashMap<FunctionSymbol, BoundBlockStatement?>,
    val functionBodies: HashMap<FunctionSymbol, BoundBlockStatement?>?
) {

    var isUnsafe = false
    val diagnostics = DiagnosticList()
    private val loopStack = Stack<Pair<BoundLabel, BoundLabel>>()
    private var labelCount = 0

    init {
        if (function != null) {
            for (p in function.parameters) {
                scope.tryDeclare(p)
            }
        }
    }

    private fun bindStatement(node: Node): BoundStatement {
        return when (node.kind) {
            SyntaxType.ExpressionStatement -> bindExpressionStatement(node as ExpressionStatementNode)
            SyntaxType.VariableDeclaration -> bindVariableDeclaration(node as VariableDeclarationNode)
            SyntaxType.IfStatement -> bindIfStatement(node as IfStatementNode)
            SyntaxType.WhileStatement -> bindWhileStatement(node as WhileStatementNode)
            SyntaxType.ForStatement -> bindForStatement(node as ForStatementNode)
            SyntaxType.BreakStatement -> bindBreakStatement(node as BreakStatementNode)
            SyntaxType.ContinueStatement -> bindContinueStatement(node as ContinueStatementNode)
            SyntaxType.ReturnStatement -> bindReturnStatement(node as ReturnStatementNode)
            else -> throw BinderError("Unexpected node: ${node.kind}")
        }
    }

    private fun bindErrorStatement() = BoundExpressionStatement(BoundErrorExpression())

    private fun bindIfStatement(node: IfStatementNode): BoundIfStatement {
        val condition = bindExpression(node.condition, TypeSymbol.Bool)
        val statement = bindBlock(node.thenStatement, false) as BoundBlockStatement
        val elseStatement: BoundStatement?
        if (node.elseClause != null) {
            elseStatement = bindStatement(node.elseClause.statement)
            if (elseStatement is BoundExpressionStatement) {
                val block = elseStatement.expression
                if (block is BoundBlockExpression &&
                    block.statements.size == 1 &&
                    block.statements.elementAt(0) is BoundIfStatement) {
                    diagnostics.styleElseIfStatement(TextLocation(
                        node.location.text,
                        TextSpan.fromBounds(
                            node.elseClause.keyword.span.start,
                            node.elseClause.statement.children.elementAt(1)
                                .children.elementAt(0).span.end)))
                }
            }
        } else {
            elseStatement = null
        }
        return BoundIfStatement(condition, statement, elseStatement)
    }

    private fun bindWhileStatement(node: WhileStatementNode): BoundWhileStatement {
        val condition = bindExpression(node.condition, TypeSymbol.Bool)
        val (body, breakLabel, continueLabel) = bindLoopBody(node.body)
        return BoundWhileStatement(condition, body, breakLabel, continueLabel)
    }

    private fun bindForStatement(node: ForStatementNode): BoundForStatement {
        val lowerBound = bindExpression(node.lowerBound, TypeSymbol.Int)
        val upperBound = bindExpression(node.upperBound, TypeSymbol.Int)

        val previous = scope
        scope = BoundScope(scope)
        val variable = bindVariable(node.identifier, TypeSymbol.Int, true, null)
        val (body, breakLabel, continueLabel) = bindLoopBody(node.body)
        scope = previous

        return BoundForStatement(variable, lowerBound, upperBound, body, breakLabel, continueLabel)
    }

    private fun bindLoopBody(node: BlockNode): Triple<BoundBlockStatement, BoundLabel, BoundLabel> {
        val numStr = labelCount.toString(16)
        val breakLabel = BoundLabel("B$numStr")
        val continueLabel = BoundLabel("C$numStr")
        labelCount++
        loopStack.push(breakLabel to continueLabel)
        val body = bindBlock(node, false) as BoundBlockStatement
        loopStack.pop()
        return Triple(body, breakLabel, continueLabel)
    }

    private fun bindBreakStatement(node: BreakStatementNode): BoundStatement {
        if (loopStack.count() == 0) {
            diagnostics.reportBreakContinueOutsideLoop(node.keyword.location, node.keyword.string!!)
            return bindErrorStatement()
        }
        val breakLabel = loopStack.peek().first
        return BoundGotoStatement(breakLabel)
    }

    private fun bindContinueStatement(node: ContinueStatementNode): BoundStatement {
        if (loopStack.count() == 0) {
            diagnostics.reportBreakContinueOutsideLoop(node.keyword.location, node.keyword.string!!)
            return bindErrorStatement()
        }
        val continueLabel = loopStack.peek().second
        return BoundGotoStatement(continueLabel)
    }

    private fun bindReturnStatement(node: ReturnStatementNode): BoundStatement {
        val expression = node.expression?.let { bindExpression(it) }
        if (function == null) {
            diagnostics.reportReturnOutsideFunction(node.keyword.location)
        }
        else if (function.returnType == TypeSymbol.Unit) {
            if (expression != null) {
                diagnostics.reportCantReturnInUnitFunction(node.expression.location)
            }
        }
        else {
            if (expression == null) {
                diagnostics.reportCantReturnWithoutValue(node.keyword.location)
            }
            else if (expression.type != TypeSymbol.err && !expression.type.isOfType(function.returnType)) {
                diagnostics.reportWrongType(node.expression.location, expression.type, function.returnType)
            }
        }
        return BoundReturnStatement(expression)
    }

    private fun bindExpressionStatement(node: ExpressionStatementNode): BoundStatement {
        when (node.expression.kind) {
            SyntaxType.Block -> {
                return bindBlock(
                        node.expression as BlockNode,
                        isExpression = false,
                        isUnsafe = false
                ) as BoundStatement
            }
            SyntaxType.UnsafeBlock -> {
                return bindBlock(
                        (node.expression as UnsafeBlockNode).block,
                        isExpression = false,
                        isUnsafe = true
                ) as BoundStatement
            }
            else -> {
                val expression = bindExpression(node.expression, canBeUnit = true)
                if (function == null && !isRepl || function != null && function.declarationNode?.lambdaArrow == null) {
                    val kind = expression.kind
                    val isAllowedExpression =
                            kind == BoundNodeType.AssignmentExpression ||
                                    kind == BoundNodeType.CallExpression ||
                                    kind == BoundNodeType.ErrorExpression
                    if (!isAllowedExpression) {
                        diagnostics.reportInvalidExpressionStatement(node.location)
                        return bindErrorStatement()
                    }
                }
                return BoundExpressionStatement(expression)
            }
        }
    }

    private fun bindVariableDeclaration(node: VariableDeclarationNode): BoundVariableDeclaration {
        val isReadOnly = node.keyword.kind == SyntaxType.Val
        val type = bindTypeClause(node.typeClauseNode)
        val initializer = bindExpression(node.initializer)
        val actualType = type ?: initializer.type
        if (!initializer.type.isOfType(actualType) && initializer.type != TypeSymbol.err) {
            diagnostics.reportWrongType(node.initializer.location, initializer.type, type!!)
        }
        val variable = bindVariable(node.identifier, actualType, isReadOnly, initializer.constantValue)
        return BoundVariableDeclaration(variable, initializer)
    }

    private fun bindTypeClause(node: TypeClauseNode?): TypeSymbol? {
        if (node == null) {
            return null
        }
        val type = TypeSymbol[node.identifier.string!!]
        if (type == null) {
            diagnostics.reportUndefinedType(node.identifier.location, node.identifier.string)
            return TypeSymbol.err
        }
        return type
    }

    private fun bindVariable(identifier: Token, type: TypeSymbol, isReadOnly: Boolean, constant: BoundConstant?): VariableSymbol {
        val name = identifier.string ?: "?"
        val variable: VariableSymbol
        variable = if (function == null && scope is BoundNamespace) {
            VariableSymbol.visible(name, type, isReadOnly, constant, (scope as BoundNamespace).path + '.' + name)
        } else {
            VariableSymbol.local(name, type, isReadOnly, constant)
        }
        if (!identifier.isMissing && !scope.tryDeclare(variable)) {
            diagnostics.reportSymbolAlreadyDeclared(identifier.location, name)
        }
        return variable
    }

    fun bindUseStatement(node: UseStatementNode) {
        val path = node.directories.joinToString(separator = ".") { it.string!! }
        if (BoundNamespace.namespaces[path] == null) {
            diagnostics.reportIncorrectUseStatement(node.location)
        } else {
            scope.use(BoundUse(path, node.isInclude))
        }
    }

    private fun bindExpression(node: Node, canBeUnit: Boolean = false, type: TypeSymbol? = null): BoundExpression {
        val result = bindExpressionInternal(node, type)
        if (!canBeUnit && result.type == TypeSymbol.Unit) {
            diagnostics.reportExpressionMustHaveValue(node.location)
            return BoundErrorExpression()
        }
        return result
    }

    private fun bindExpressionInternal(node: Node, type: TypeSymbol?): BoundExpression {
        return when (node.kind) {
            SyntaxType.ParenthesizedExpression -> bindParenthesizedExpression(node as ParenthesizedExpressionNode)
            SyntaxType.LiteralExpression -> bindLiteralExpression(node as LiteralExpressionNode, type)
            SyntaxType.UnaryExpression -> bindUnaryExpression(node as UnaryExpressionNode)
            SyntaxType.BinaryExpression -> {
                node as BinaryExpressionNode
                if (node.operator.kind == SyntaxType.Dot) {
                    bindDotAccessExpression(node)
                } else {
                    bindBinaryExpression(node)
                }
            }
            SyntaxType.NameExpression -> bindNameExpression(node as NameExpressionNode)
            SyntaxType.AssignmentExpression -> bindAssignmentExpression(node as AssignmentExpressionNode)
            SyntaxType.CallExpression -> bindCallExpression(node as CallExpressionNode)
            SyntaxType.IndexExpression -> bindIndexExpression(node as IndexExpressionNode)
            SyntaxType.UnsafeBlock -> bindBlock(
                (node as UnsafeBlockNode).block,
                isExpression = true,
                isUnsafe = true
            ) as BoundExpression
            SyntaxType.Block -> bindBlock(
                node as BlockNode,
                isExpression = true,
                isUnsafe = false
            ) as BoundExpression
            else -> throw BinderError("Unexpected node: ${node.kind}")
        }
    }

    private fun bindExpression(node: Node, type: TypeSymbol): BoundExpression {
        val result = bindExpression(node)
        if (type != TypeSymbol.err &&
            result.type != TypeSymbol.err &&
            !result.type.isOfType(type)) {
            diagnostics.reportWrongType(node.location, result.type, type)
        }
        return result
    }

    private fun bindCallExpression(node: CallExpressionNode): BoundExpression {

        val type = TypeSymbol[node.identifier.string!!]
        if (node.arguments.nodeCount == 1 && type != null) {
            return bindCast(node.arguments[0], type)
        }

        val arguments = ArrayList<BoundExpression>()

        for (a in node.arguments) {
            val arg = bindExpression(a, canBeUnit = true)
            if (arg.kind == BoundNodeType.ErrorExpression) {
                return BoundErrorExpression()
            }
            arguments.add(arg)
        }

        val types = arguments.map { it.type }

        val function = scope.tryLookup(
            listOf(node.identifier.string),
            CallableSymbol.generateSuffix(types, false))

        if (function == null) {
            diagnostics.reportUndefinedFunction(node.identifier.location, node.identifier.string, types, false)
            return BoundErrorExpression()
        }

        if (function !is CallableSymbol) {
            diagnostics.reportNotCallable(node.identifier.location, function)
            return BoundErrorExpression()
        }

        if (node.arguments.nodeCount > function.parameters.size) {
            val firstExceedingNode = if (function.parameters.isNotEmpty()) {
                node.arguments.getSeparator(function.parameters.size - 1)
            } else {
                node.arguments[0]
            }
            val span = TextSpan.fromBounds(firstExceedingNode.span.start, node.arguments.last().span.end)
            val location = TextLocation(firstExceedingNode.syntaxTree.sourceText, span)
            diagnostics.reportWrongArgumentCount(location, function.name, node.arguments.nodeCount, function.parameters.size)
            return BoundErrorExpression()
        } else if (node.arguments.nodeCount < function.parameters.size) {
            diagnostics.reportWrongArgumentCount(node.rightBracket.location, function.name, node.arguments.nodeCount, function.parameters.size)
            return BoundErrorExpression()
        }

        for (i in arguments.indices) {
            val arg = arguments[i]
            val param = function.parameters[i]

            if (!arg.type.isOfType(param.type)) {
                if (arg.type != TypeSymbol.err) {
                    diagnostics.reportWrongArgumentType(node.arguments[i].location, param.name, arg.type, param.type)
                }
                return BoundErrorExpression()
            }
        }

        return BoundCallExpression(function, arguments)
    }

    private fun bindIndexExpression(node: IndexExpressionNode): BoundExpression {
        val expression = bindExpression(node.expression)

        if (expression.kind == BoundNodeType.ErrorExpression) {
            return BoundErrorExpression()
        }

        val list = ArrayList<BoundExpression>()
        list.add(expression)
        for (it in node.arguments) {
            list.add(bindExpression(it))
        }
        if (expression.type.isOfType(TypeSymbol.Ptr) && list.size == 2 && list[1].type.isOfType(TypeSymbol.Integer)) {
            if (!isUnsafe) {
                diagnostics.reportPointerOperationsAreUnsafe(node.location)
                return BoundErrorExpression()
            }
            return BoundPointerAccess(expression, list[1])
        }
        val operatorFunction = scope.tryLookup(
            listOf("get"),
            CallableSymbol.generateSuffix(list.map { it.type }, true)
        )
        if (operatorFunction != null && operatorFunction.meta.isOperator) {
            operatorFunction as CallableSymbol
            return BoundCallExpression(operatorFunction, list)
        }

        diagnostics.reportUndefinedFunction(node.location, "get", list.map { it.type }, true)
        return BoundErrorExpression()
    }

    private fun bindBlock(node: BlockNode, isExpression: Boolean, isUnsafe: Boolean = false): BoundNode {
        val statements = ArrayList<BoundStatement>()
        val previous = scope
        scope = BoundScope(scope)
        var type = TypeSymbol.Unit
        val isLastUnsafe = this.isUnsafe
        this.isUnsafe = this.isUnsafe || isUnsafe
        for (i in node.statements.indices) {
            val s = node.statements.elementAt(i)
            when (s.kind) {
                SyntaxType.FunctionDeclaration -> {
                    val symbol = bindFunctionDeclaration(s as FunctionDeclarationNode)
                    if (function != null) {
                        bindFunction(scope, functions, symbol, diagnostics, functionBodies)
                    }
                }
                SyntaxType.UseStatement -> bindUseStatement(s as UseStatementNode)
                else -> {
                    if (isExpression && i == node.statements.size - 1 && s.kind == SyntaxType.ExpressionStatement) {
                        s as ExpressionStatementNode
                        val expression = bindExpression(s.expression)
                        type = expression.type
                        statements.add(BoundExpressionStatement(expression))
                    } else {
                        val statement = bindStatement(s)
                        statements.add(statement)
                    }
                }
            }
        }
        this.isUnsafe = isLastUnsafe
        scope = previous
        return if (isExpression) {
            BoundBlockExpression(statements, type)
        } else {
            BoundBlockStatement(statements)
        }
    }
/*
    private fun bindBlockStatement(node: BlockNode): BoundBlockStatement {
        val statements = ArrayList<BoundStatement>()
        val previous = scope
        scope = BoundScope(scope)
        for (s in node.statements) {
            when (s.kind) {
                SyntaxType.FunctionDeclaration -> {
                    val symbol = bindFunctionDeclaration(s as FunctionDeclarationNode)
                    if (function != null) {
                        bindFunction(scope, functions, symbol, diagnostics, functionBodies)
                    }
                }
                SyntaxType.UseStatement -> bindUseStatement(s as UseStatementNode)
                else -> {
                    val statement = bindStatement(s)
                    statements.add(statement)
                }
            }
        }
        scope = previous
        return BoundBlockStatement(statements)
    }
*/

    private fun bindCast(node: Node, type: TypeSymbol): BoundExpression {
        val expression = bindExpression(node)
        if (expression.type == TypeSymbol.err) {
            return BoundErrorExpression()
        }
        val conversion = Conversion.classify(expression.type, type)
        if (!conversion.exists) {
            diagnostics.reportCantCast(node.location, expression.type, type)
            return BoundErrorExpression()
        }
        if (conversion.isIdentity) {
            return expression
        }
        return BoundCastExpression(type, expression)
    }

    private fun bindAssignmentExpression(node: AssignmentExpressionNode): BoundExpression {
        val name = node.identifierToken.string ?: return BoundErrorExpression()
        val boundExpression = bindExpression(node.expression)
        val variable = scope.tryLookup(listOf(name))
        if (variable == null) {
            diagnostics.reportUndefinedName(node.identifierToken.location, name)
            return boundExpression
        }
        if (variable !is VariableSymbol) {
            diagnostics.reportVarIsConstant(node.equalsToken.location, name)
            return boundExpression
        }
        if (variable.isReadOnly) {
            diagnostics.reportVarIsImmutable(node.equalsToken.location, name)
            return boundExpression
        }
        if (!boundExpression.type.isOfType(variable.type)) {
            diagnostics.reportWrongType(node.identifierToken.location, variable.type, boundExpression.type)
            return boundExpression
        }
        return BoundAssignmentExpression(variable, boundExpression)
    }

    private fun bindNameExpression(node: NameExpressionNode): BoundExpression {
        val name = node.identifier.string ?: return BoundErrorExpression()
        val variable = scope.tryLookup(listOf(name))
        if (variable == null) {
            diagnostics.reportUndefinedName(node.identifier.location, name)
            return BoundErrorExpression()
        }
        if (variable !is VariableSymbol) {
            diagnostics.reportUndefinedName(node.identifier.location, name)
            return BoundErrorExpression()
        }
        return BoundVariableExpression(variable)
    }

    private fun bindParenthesizedExpression(
        node: ParenthesizedExpressionNode
    ) = bindExpression(node.expression)

    private fun bindLiteralExpression(
        node: LiteralExpressionNode,
        expectedType: TypeSymbol?
    ): BoundLiteralExpression {
        val type = when (node.value) {
            is Byte -> when (expectedType) {
                TypeSymbol.I8 -> TypeSymbol.I8
                TypeSymbol.I16 -> TypeSymbol.I16
                TypeSymbol.I64 -> TypeSymbol.I64
                else -> TypeSymbol.I32
            }
            is Short -> when (expectedType) {
                TypeSymbol.I16 -> TypeSymbol.I16
                TypeSymbol.I64 -> TypeSymbol.I64
                else -> TypeSymbol.I32
            }
            is Int -> when (expectedType) {
                TypeSymbol.I64 -> TypeSymbol.I64
                else -> TypeSymbol.I32
            }
            is Long -> TypeSymbol.I64
            is Float -> TypeSymbol.Float
            is Double -> TypeSymbol.Double
            is Boolean -> TypeSymbol.Bool
            is String -> TypeSymbol.String
            else -> throw BinderError("Unexpected literal of type ${node.value?.javaClass}")
        }
        return BoundLiteralExpression(when (type) {
            TypeSymbol.I8 -> (node.value as Number).toByte()
            TypeSymbol.I16 -> (node.value as Number).toShort()
            TypeSymbol.I32 -> (node.value as Number).toInt()
            TypeSymbol.I64 -> (node.value as Number).toLong()
            TypeSymbol.Float -> (node.value as Number).toFloat()
            else -> node.value
        }, type)
    }

    private fun bindUnaryExpression(node: UnaryExpressionNode): BoundExpression {
        val operand = bindExpression(node.operand)
        if (operand.type == TypeSymbol.err) {
            return BoundErrorExpression()
        }
        if (node.operator.kind == SyntaxType.BitAnd) {
            if (!isUnsafe) {
                diagnostics.reportPointerOperationsAreUnsafe(node.operator.location)
                return BoundErrorExpression()
            }
            if (operand.kind != BoundNodeType.VariableExpression) {
                diagnostics.reportReferenceRequiresMutableVar(node.operator.location)
                return BoundErrorExpression()
            }
            operand as BoundVariableExpression
            if (operand.symbol.isReadOnly) {
                diagnostics.reportReferenceRequiresMutableVar(node.operator.location)
                return BoundErrorExpression()
            }
            return BoundReference(operand)
        }
        val operator = BoundUnOperator.bind(node.operator.kind, operand.type)
        if (operator == null) {
            val operatorFunction = scope.tryLookup(
                listOf(Translator.unaryOperatorToString(node.operator.kind)),
                CallableSymbol.generateSuffix(listOf(operand.type), true))
            if (operatorFunction != null && operatorFunction.meta.isOperator) {
                operatorFunction as CallableSymbol
                return BoundCallExpression(operatorFunction, listOf(operand))
            }
            diagnostics.reportUnaryOperator(node.operator.location, node.operator.kind, operand.type)
            return BoundErrorExpression()
        }
        return BoundUnaryExpression(operator, operand)
    }

    private fun bindBinaryExpression(node: BinaryExpressionNode): BoundExpression {

        val left = bindExpression(node.left)
        val right = bindExpression(node.right)

        if (left.type == TypeSymbol.err || right.type == TypeSymbol.err) {
            return BoundErrorExpression()
        }

        val operator = BoundBiOperator.bind(node.operator.kind, left.type, right.type)
        if (operator != null) {
            return BoundBinaryExpression(left, operator, right)
        }

        if (node.operator.kind == SyntaxType.IsNotEqual) {
            val operatorFunction = scope.tryLookup(
                listOf(Translator.binaryOperatorToString(SyntaxType.IsEqual)),
                CallableSymbol.generateSuffix(listOf(left.type, right.type), true))
            if (operatorFunction != null && operatorFunction.meta.isOperator) {
                operatorFunction as CallableSymbol
                return BoundUnaryExpression(
                    BoundUnOperator(SyntaxType.Bang, BoundUnOperator.Type.Not, TypeSymbol.Bool),
                    BoundCallExpression(operatorFunction, listOf(left, right)))
            }
        } else {
            val operatorFunction = scope.tryLookup(
                listOf(Translator.binaryOperatorToString(node.operator.kind)),
                CallableSymbol.generateSuffix(listOf(left.type, right.type), true))
            if (operatorFunction != null && operatorFunction.meta.isOperator) {
                operatorFunction as CallableSymbol
                return BoundCallExpression(operatorFunction, listOf(left, right))
            }
        }
        diagnostics.reportBinaryOperator(node.operator.location, left.type, node.operator.kind, right.type)
        return BoundErrorExpression()
    }

    private fun bindDotAccessExpression(node: BinaryExpressionNode): BoundExpression {

        fun bindNamespaceAccess(leftName: String, rightName: String): BoundExpression {

            val symbol = if (node.right.kind == SyntaxType.CallExpression) {
                node.right as CallExpressionNode
                scope.tryLookup(leftName.split('.').toMutableList().apply { add(rightName) }, CallableSymbol.generateSuffix(node.right.arguments.map { bindExpression(it).type }, false))
            } else {
                scope.tryLookup(leftName.split('.').toMutableList().apply { add(rightName) })
            }

            if (node.right.kind == SyntaxType.CallExpression) {
                if (symbol != null) {
                    node.right as CallExpressionNode
                    if (symbol !is CallableSymbol) {
                        diagnostics.reportNotCallable(node.right.location, symbol)
                        return BoundErrorExpression()
                    }
                    return BoundCallExpression(symbol as FunctionSymbol, node.right.arguments.map { bindExpression(it) })
                }
                node.right as CallExpressionNode
                diagnostics.reportUndefinedFunction(node.right.location, rightName, node.right.arguments.map { bindExpression(it).type }, false)
                return BoundErrorExpression()
            } else {
                if (symbol != null) {
                    symbol as VariableSymbol
                    return BoundVariableExpression(symbol)
                }
            }
            val namespace = BoundNamespace["$leftName.$rightName"]
            if (namespace == null) {
                diagnostics.reportUndefinedName(node.right.location, rightName)
                return BoundErrorExpression()
            }
            return BoundNamespaceFieldAccess(namespace)
        }

        if (node.right.kind != SyntaxType.NameExpression && node.right.kind != SyntaxType.CallExpression) {
            diagnostics.reportCantBeAfterDot(node.right.location)
            return BoundErrorExpression()
        }

        node.right as NameExpressionNode

        if (node.left.kind == SyntaxType.NameExpression) {
            node.left as NameExpressionNode
            val leftName = node.left.identifier.string ?: return BoundErrorExpression()
            val rightName = node.right.identifier.string ?: return BoundErrorExpression()
            scope.tryLookup(listOf(leftName)) ?: return bindNamespaceAccess(leftName, rightName)
        }

        val left = bindExpression(node.left)
        val name = node.right.identifier.string ?: return BoundErrorExpression()

        if (left.kind == BoundNodeType.NamespaceFieldAccess) {
            left as BoundNamespaceFieldAccess
            val leftName = left.namespace.path
            return bindNamespaceAccess(leftName, name)
        }

        if (left.type.kind == Symbol.Kind.Struct) {
            val i = (left.type as TypeSymbol.StructTypeSymbol).fields.indexOfFirst { it.name == name }
            if (node.right.kind == SyntaxType.NameExpression) {
                if (i == -1) {
                    diagnostics.reportUndefinedName(node.right.location, name)
                    return BoundErrorExpression()
                }
                return BoundStructFieldAccess(left, i)
            }
        }
        if (node.right.kind == SyntaxType.CallExpression) {
            node.right as CallExpressionNode
            val arguments = arrayListOf(left).apply { for (a in node.right.arguments) add(bindExpression(a)) }
            val types = arguments.map { it.type }
            val function = scope.tryLookup(listOf(name), CallableSymbol.generateSuffix(types, true))
            if (function != null) {
                return BoundCallExpression(function as CallableSymbol, arguments)
            }
            diagnostics.reportUndefinedFunction(node.right.location, name, types, true)
            return BoundErrorExpression()
        }
        diagnostics.reportUndefinedName(node.right.location, name)
        return BoundErrorExpression()
    }

    fun bindGlobalStatement(
        statement: Node,
        syntaxTree: SyntaxTree,
        statementBuilder: ArrayList<BoundStatement>,
        symbols: ArrayList<Symbol>
    ) {
        when (statement.kind) {
            SyntaxType.FunctionDeclaration -> {
                statement as FunctionDeclarationNode
                symbols.add(bindFunctionDeclaration(statement))
            }
            SyntaxType.VariableDeclaration -> {
                statement as VariableDeclarationNode
                val statement = bindVariableDeclaration(statement)
                statementBuilder.add(statement)
                symbols.add(statement.variable)
            }
            SyntaxType.UseStatement -> {
                statement as UseStatementNode
                bindUseStatement(statement)
            }
            SyntaxType.ReplStatement -> {
                statement as ReplStatementNode
                val statement = bindStatement(statement.statementNode)
                statementBuilder.add(statement)
            }
            SyntaxType.NamespaceStatement -> {
                statement as NamespaceStatementNode
                val prev = scope
                val namespace = BoundNamespace(syntaxTree.projectPath + '.' + statement.identifier.string, scope)
                scope = namespace
                for (s in statement.members) {
                    bindGlobalStatement(s, syntaxTree, statementBuilder, symbols)
                }
                scope = prev
            }
            else -> throw BinderError("Incorrect statement got to be global (${statement.kind})")
        }
    }

    companion object {

        fun bindGlobalScope(
            previous: BoundGlobalScope?,
            syntaxTrees: Collection<SyntaxTree>
        ): BoundGlobalScope {

            val parentScope = createParentScopes(previous)
            val binder = Binder(parentScope, null, HashMap(), null)

            val symbols = ArrayList<Symbol>()
            val statementBuilder = ArrayList<BoundStatement>()

            for (syntaxTree in syntaxTrees) {
                val strBuilder = StringBuilder()
                var parent = parentScope
                syntaxTree.projectPath.split('.').forEachIndexed { i, s ->
                    val path = if (i == 0) { strBuilder.append(s).toString() }
                        else { strBuilder.append('.').append(s).toString() }
                    parent = BoundNamespace.getOr(path) { BoundNamespace(path, parent) }
                }
            }
            for (syntaxTree in syntaxTrees) {
                binder.diagnostics.append(syntaxTree.diagnostics)
                val prev = binder.scope
                val namespace = BoundNamespace[syntaxTree.projectPath]!!
                binder.scope = namespace
                for (s in syntaxTree.root.members) {
                    binder.bindGlobalStatement(s, syntaxTree, statementBuilder, symbols)
                }
                binder.scope = prev
            }

            var entryFn = symbols.find {
                it.kind == Symbol.Kind.Function &&
                (it as FunctionSymbol).meta.isEntry &&
                it.type == TypeSymbol.Fn.entry &&
                it.parameters.isEmpty()
            } as FunctionSymbol?

            val diagnostics = binder.diagnostics

            if (entryFn == null) {
                entryFn = FunctionSymbol(
                    "main",
                    arrayOf(),
                    if (isRepl) TypeSymbol.Any else TypeSymbol.Unit,
                        "main",
                    null,
                        Symbol.MetaData()
                )
                if (!isRepl && !isSharedLib) {
                    diagnostics.reportNoMainFn()
                }
            }

            if (previous != null) {
                diagnostics.append(previous.diagnostics)
            }

            return BoundGlobalScope(
                previous,
                diagnostics,
                symbols,
                statementBuilder,
                entryFn)
        }

        private fun createParentScopes(
            previous: BoundGlobalScope?
        ): BoundScope {

            val stack = Stack<BoundGlobalScope>()
            var prev = previous

            while (prev != null) {
                stack.push(prev)
                prev = prev.previous
            }

            var parent: BoundScope = createRootScope()

            while (stack.count() > 0) {
                val previous = stack.pop()
                val scope = BoundScope(parent)
                for (v in previous.symbols) {
                    v as VisibleSymbol
                    if (v.meta.isEntry) { //path.substringBeforeLast('.') == "main"
                        scope.tryDeclare(v)
                    }
                }
                parent = scope
            }

            return parent
        }

        private fun createRootScope(): BoundScope {
            val result = BoundScope(null)
            if (isRepl) {
                for (fn in BuiltinFunctions.getAll()) {
                    result.tryDeclare(fn)
                }
            }
            return result
        }

        fun bindFunction(
            parentScope: BoundScope,
            functions: HashMap<FunctionSymbol, BoundBlockStatement?>,
            symbol: FunctionSymbol,
            diagnostics: DiagnosticList,
            functionBodies: HashMap<FunctionSymbol, BoundBlockStatement?>?
        ) {
            val binder = Binder(parentScope, symbol, functions, functionBodies)
            when {
                symbol.meta.isExtern -> functions[symbol] = null
                symbol.declarationNode!!.lambdaArrow == null -> {
                    val body = binder.bindBlock(symbol.declarationNode.body as BlockNode, false) as BoundBlockStatement
                    val loweredBody = Lowerer.lower(body)
                    if (symbol.returnType != TypeSymbol.Unit && !ControlFlowGraph.allPathsReturn(loweredBody)) {
                        diagnostics.reportAllPathsMustReturn(symbol.declarationNode.identifier.location)
                    }
                    functions[symbol] = loweredBody
                    functionBodies?.put(symbol, body)
                }
                else -> {
                    val body = symbol.declarationNode.body as ExpressionStatementNode
                    val expression = binder.bindExpression(body.expression, canBeUnit = true)
                    val loweredBody = Lowerer.lower(expression)
                    functions[symbol] = loweredBody
                    functionBodies?.put(symbol, loweredBody)
                }
            }
            diagnostics.append(binder.diagnostics)
        }

        fun bindProgram(
            previous: BoundProgram?,
            globalScope: BoundGlobalScope
        ): BoundProgram {

            val parentScope = createParentScopes(globalScope)
            val functions = HashMap<FunctionSymbol, BoundBlockStatement?>()
            val diagnostics = DiagnosticList()

            val functionBodies = if (isSharedLib) {
                val tmp = HashMap<FunctionSymbol, BoundBlockStatement?>()
                for (symbol in globalScope.symbols) {
                    if (symbol is FunctionSymbol) {
                        bindFunction(BoundScope(symbol.namespace!!), functions, symbol, diagnostics, tmp)
                    }
                }
                tmp
            } else {
                for (symbol in globalScope.symbols) {
                    if (symbol is FunctionSymbol) {
                        bindFunction(BoundScope(symbol.namespace!!), functions, symbol, diagnostics, null)
                    }
                }
                null
            }

            val statement: BoundBlockStatement
            if (isRepl) {
                val statements = globalScope.statements
                if (statements.size == 1) {
                    val s = statements[0]
                    if (s is BoundExpressionStatement &&
                        s.expression.type != TypeSymbol.Unit) {
                        statements[0] = BoundReturnStatement(s.expression)
                    }
                }
                else if (statements.size != 0){
                    val nullValue = BoundLiteralExpression("", TypeSymbol.String)
                    statements[0] = BoundReturnStatement(nullValue)
                }
                val body = Lowerer.lower(BoundBlockStatement(
                    globalScope.statements
                ))
                functions[globalScope.mainFn] = body
                statement = BoundBlockStatement(listOf())
            }
            else {
                statement = Lowerer.lower(BoundBlockStatement(
                    globalScope.statements
                ))
            }

            return BoundProgram(
                previous,
                diagnostics,
                globalScope.mainFn,
                functions,
                statement,
                functionBodies)
        }
    }

    private fun bindFunctionDeclaration(node: FunctionDeclarationNode): FunctionSymbol {

        val meta = Symbol.MetaData()
        val params = ArrayList<VariableSymbol>()
        val seenParameterNames = HashSet<String>()

        if (node.extensionType != null) {
            seenParameterNames.add("this")
            params.add(VariableSymbol.param("this", bindTypeClause(node.extensionType)!!))
            meta.isExtension = true
        }

        if (node.params != null) {
            for (paramNode in node.params.iterator()) {
                val name = paramNode.identifier.string!!
                if (!seenParameterNames.add(name)) {
                    diagnostics.reportParamAlreadyExists(paramNode.identifier.location, name)
                } else {
                    val type = bindTypeClause(paramNode.typeClause)!!
                    val parameter = VariableSymbol.param(name, type)
                    params.add(parameter)
                }
            }
        }

        val type: TypeSymbol = if (node.typeClause == null) {
            TypeSymbol.Unit
        } else {
            bindTypeClause(node.typeClause)!!
        }

        val path = if (scope is BoundNamespace) {
            (scope as BoundNamespace).path + '.' + node.identifier.string!!
        } else {
            function!!.mangledName().substringBeforeLast('.') + '.' + Symbol.genFnUID()
        }
        val function = FunctionSymbol(node.identifier.string!!, params.toTypedArray(), type, path, node, meta)
        for (annotation in node.annotations) {
            when (annotation.identifier.string) {
                "inline" -> meta.isInline = true
                "internal" -> meta.isInternal = true
                "extern" -> meta.isExtern = true
                "operator" -> {
                    if (function.meta.isExtension) {
                        function.meta.isOperator = true
                        when (function.name) {
                            "equals" -> {
                                if (function.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "plus" -> {
                                if (function.parameters.size != 2 && function.parameters.size != 1) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "minus" -> {
                                if (function.parameters.size != 2 && function.parameters.size != 1) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "times" -> {
                                if (function.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "divide" -> {
                                if (function.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "rem" -> {
                                if (function.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "and" -> {
                                if (function.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "or" -> {
                                if (function.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "not" -> {
                                if (function.parameters.size != 1) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "get" -> {}
                            else -> {
                                diagnostics.reportInvalidAnnotation(annotation.location)
                            }
                        }
                    } else {
                        diagnostics.reportInvalidAnnotation(annotation.location)
                    }
                }
                "entry" -> {
                    if (Symbol.MetaData.entryExists) {
                        diagnostics.reportMultipleEntryFuncs(annotation.location)
                    }
                    meta.isEntry = true
                    Symbol.MetaData.entryExists = true
                }
                "cname" -> {
                    val expression = bindLiteralExpression(annotation.value as LiteralExpressionNode, TypeSymbol.String)
                    meta.cname = expression.value as String
                }
                else -> {
                    diagnostics.reportInvalidAnnotation(annotation.location)
                }
            }
        }

        if (!scope.tryDeclare(function)) {
            diagnostics.reportSymbolAlreadyDeclared(node.identifier.location, function.name)
        }

        return function
    }
}