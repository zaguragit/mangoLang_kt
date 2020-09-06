package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.interpreter.binding.nodes.BiOperator
import mango.interpreter.binding.nodes.BoundNode
import mango.interpreter.binding.nodes.UnOperator
import mango.interpreter.binding.nodes.expressions.*
import mango.interpreter.binding.nodes.statements.*
import mango.interpreter.symbols.*
import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.Translator
import mango.interpreter.syntax.nodes.*
import mango.interpreter.text.TextLocation
import mango.interpreter.text.TextSpan
import mango.isRepl
import mango.isSharedLib
import mango.util.BinderError
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class Binder(
    var scope: Scope,
    val function: CallableSymbol?,
    val functions: HashMap<CallableSymbol, BlockStatement?>,
    val functionBodies: HashMap<CallableSymbol, BlockStatement?>?
) {

    var isUnsafe = false
    val diagnostics = DiagnosticList()
    private val loopStack = Stack<Pair<Label, Label>>()
    private var labelCount = 0

    init {
        if (function != null) {
            for (p in function.parameters) {
                scope.tryDeclare(p)
            }
        }
    }

    private fun bindStatement(node: Node): Statement {
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

    private fun bindErrorStatement() = ExpressionStatement(ErrorExpression())

    private fun bindIfStatement(node: IfStatementNode): IfStatement {
        val condition = bindExpression(node.condition, TypeSymbol.Bool)
        val statement = bindBlock(node.thenStatement, false) as BlockStatement
        val elseStatement: Statement?
        if (node.elseClause != null) {
            elseStatement = bindStatement(node.elseClause.statement)
            if (elseStatement is ExpressionStatement) {
                val block = elseStatement.expression
                if (block is BlockExpression &&
                    block.statements.size == 1 &&
                    block.statements.elementAt(0) is IfStatement) {
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
        return IfStatement(condition, statement, elseStatement)
    }

    private fun bindWhileStatement(node: WhileStatementNode): WhileStatement {
        val condition = bindExpression(node.condition, TypeSymbol.Bool)
        val (body, breakLabel, continueLabel) = bindLoopBody(node.body)
        return WhileStatement(condition, body, breakLabel, continueLabel)
    }

    private fun bindForStatement(node: ForStatementNode): ForStatement {
        val lowerBound = bindExpression(node.lowerBound, TypeSymbol.Int)
        val upperBound = bindExpression(node.upperBound, TypeSymbol.Int)

        val previous = scope
        scope = Scope(scope)
        val variable = bindVariable(node.identifier, TypeSymbol.Int, true, null)
        val (body, breakLabel, continueLabel) = bindLoopBody(node.body)
        scope = previous

        return ForStatement(variable, lowerBound, upperBound, body, breakLabel, continueLabel)
    }

    private fun bindLoopBody(node: BlockNode): Triple<BlockStatement, Label, Label> {
        val numStr = labelCount.toString(16)
        val breakLabel = Label("B$numStr")
        val continueLabel = Label("C$numStr")
        labelCount++
        loopStack.push(breakLabel to continueLabel)
        val body = bindBlock(node, false) as BlockStatement
        loopStack.pop()
        return Triple(body, breakLabel, continueLabel)
    }

    private fun bindBreakStatement(node: BreakStatementNode): Statement {
        if (loopStack.count() == 0) {
            diagnostics.reportBreakContinueOutsideLoop(node.keyword.location, node.keyword.string!!)
            return bindErrorStatement()
        }
        val breakLabel = loopStack.peek().first
        return GotoStatement(breakLabel)
    }

    private fun bindContinueStatement(node: ContinueStatementNode): Statement {
        if (loopStack.count() == 0) {
            diagnostics.reportBreakContinueOutsideLoop(node.keyword.location, node.keyword.string!!)
            return bindErrorStatement()
        }
        val continueLabel = loopStack.peek().second
        return GotoStatement(continueLabel)
    }

    private fun bindReturnStatement(node: ReturnStatementNode): Statement {
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
        return ReturnStatement(expression)
    }

    private fun bindExpressionStatement(node: ExpressionStatementNode): Statement {
        when (node.expression.kind) {
            SyntaxType.Block -> {
                return bindBlock(
                    node.expression as BlockNode,
                    isExpression = false,
                    isUnsafe = false
                ) as Statement
            }
            SyntaxType.UnsafeBlock -> {
                return bindBlock(
                    (node.expression as UnsafeBlockNode).block,
                    isExpression = false,
                    isUnsafe = true
                ) as Statement
            }
            else -> {
                val expression = bindExpression(node.expression, canBeUnit = true)
                if (function != null && function.declarationNode?.lambdaArrow == null) {
                    val kind = expression.kind
                    val isAllowedExpression =
                            kind == BoundNode.Kind.AssignmentExpression ||
                            kind == BoundNode.Kind.CallExpression ||
                            kind == BoundNode.Kind.ErrorExpression
                    if (!isAllowedExpression) {
                        diagnostics.reportInvalidExpressionStatement(node.location)
                        return bindErrorStatement()
                    }
                }
                return ExpressionStatement(expression)
            }
        }
    }

    private fun bindVariableDeclaration(node: VariableDeclarationNode): VariableDeclaration {
        val isReadOnly = node.keyword.kind == SyntaxType.Val
        val type = node.typeClauseNode?.let { bindTypeClause(it) }
        if (node.initializer == null) {
            val variable = bindVariable(node.identifier, type ?: TypeSymbol.Any, isReadOnly, null)
            diagnostics.reportHasToBeInitialized(node.identifier.location)
            return VariableDeclaration(variable, ErrorExpression())
        }
        val initializer = bindExpression(node.initializer)
        val actualType = type ?: initializer.type
        if (!initializer.type.isOfType(actualType) && initializer.type != TypeSymbol.err) {
            diagnostics.reportWrongType(node.initializer.location, initializer.type, type!!)
        }
        val variable = bindVariable(node.identifier, actualType, isReadOnly, initializer.constantValue)
        return VariableDeclaration(variable, initializer)
    }

    private fun bindTypeClause(node: TypeClauseNode): TypeSymbol {
        val type = TypeSymbol[node.identifier.string!!]
        if (type == null) {
            diagnostics.reportUndefinedType(node.identifier.location, node.identifier.string)
            return TypeSymbol.err
        }
        val types = node.types
        if (types != null) {
            if (type.paramCount != types.nodeCount) {
                diagnostics.reportWrongArgumentCount(node.identifier.location, types.nodeCount, type.paramCount)
            }
            return type(Array(type.paramCount) {
                val t = bindTypeClause(types[it])
                if (!t.isOfType(type.params[it])) {
                    diagnostics.reportWrongType(types[it].location, t, type.params[it])
                }
                t
            })
        }
        return type
    }

    private fun bindVariable(identifier: Token, type: TypeSymbol, isReadOnly: Boolean, constant: BoundConstant?): VariableSymbol {
        val name = identifier.string ?: "?"
        val variable: VariableSymbol
        variable = if (function == null && scope is Namespace) {
            VariableSymbol.visible(name, type, isReadOnly, constant, (scope as Namespace).path + '.' + name)
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
        if (Namespace.namespaces[path] == null) {
            diagnostics.reportIncorrectUseStatement(node.location)
        } else {
            scope.use(UseStatement(path, node.isInclude))
        }
    }

    private fun bindExpression(node: Node, canBeUnit: Boolean = false, type: TypeSymbol? = null): BoundExpression {
        val result = bindExpressionInternal(node, type)
        if (!canBeUnit && result.type == TypeSymbol.Unit) {
            diagnostics.reportExpressionMustHaveValue(node.location)
            return ErrorExpression()
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
                    bindDotAccessExpression(node).first
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
            else -> throw BinderError("Unexpected node: ${node.kind}, ${node.location}")
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

        val arguments = ArrayList<BoundExpression>()

        for (a in node.arguments) {
            val arg = bindExpression(a, canBeUnit = true)
            if (arg.kind == BoundNode.Kind.ErrorExpression) {
                return ErrorExpression()
            }
            arguments.add(arg)
        }

        val function = if (node.function.kind == SyntaxType.NameExpression) {
            val types = arguments.map { it.type }
            val name = node.function as NameExpressionNode
            val symbol = scope.tryLookup(listOf(name.identifier.string!!), CallableSymbol.generateSuffix(types, false))
            if (symbol != null) {
                symbol as VariableSymbol
                NameExpression(symbol)
            } else {
                val symbol2 = scope.tryLookup(listOf(name.identifier.string))
                if (symbol2 != null) {
                    symbol2 as VariableSymbol
                    NameExpression(symbol2)
                } else {
                    diagnostics.reportUndefinedFunction(name.location, name.identifier.string, types, false)
                    return ErrorExpression()
                }
            }
        } else if (node.function.kind == SyntaxType.BinaryExpression && (node.function as BinaryExpressionNode).operator.kind == SyntaxType.Dot) {
            val types = arguments.map { it.type }
            val (f, firstArg) = bindDotAccessExpression(node.function, types)
            firstArg?.let { arguments.add(0, it) }
            f
        } else {
            bindExpression(node.function)
        }
        if (function.type == TypeSymbol.err) {
            return ErrorExpression()
        }
        if (function.type.kind != Symbol.Kind.FunctionType) {
            diagnostics.reportNotCallable(node.function.location, function.type)
            return ErrorExpression()
        }
        val functionType = function.type as TypeSymbol.Fn

        if (arguments.size > functionType.args.size) {
            val firstExceedingNode = if (functionType.args.isNotEmpty()) {
                node.arguments.getSeparator(functionType.args.size - 1)
            } else {
                node.arguments[0]
            }
            val span = TextSpan.fromBounds(firstExceedingNode.span.start, node.arguments.last().span.end)
            val location = TextLocation(firstExceedingNode.syntaxTree.sourceText, span)
            diagnostics.reportWrongArgumentCount(location, node.arguments.nodeCount, functionType.args.size)
            return ErrorExpression()
        } else if (arguments.size < functionType.args.size) {
            diagnostics.reportWrongArgumentCount(node.rightBracket.location, node.arguments.nodeCount, functionType.args.size)
            return ErrorExpression()
        }

        for (i in arguments.indices) {
            val arg = arguments[i]
            val param = functionType.args.elementAt(i)

            if (!arg.type.isOfType(param)) {
                if (arg.type != TypeSymbol.err) {
                    diagnostics.reportWrongArgumentType(node.arguments[i].location, param.name, arg.type, param)
                }
                return ErrorExpression()
            }
        }

        return CallExpression(function, arguments)
    }

    private fun bindIndexExpression(node: IndexExpressionNode): BoundExpression {
        val expression = bindExpression(node.expression)

        if (expression.kind == BoundNode.Kind.ErrorExpression) {
            return ErrorExpression()
        }

        val list = ArrayList<BoundExpression>()
        list.add(expression)
        for (it in node.arguments) {
            list.add(bindExpression(it))
        }
        if (expression.type.isOfType(TypeSymbol.Ptr) && list.size == 2 && list[1].type.isOfType(TypeSymbol.Integer)) {
            if (!isUnsafe) {
                diagnostics.reportPointerOperationsAreUnsafe(node.location)
                return ErrorExpression()
            }
            return PointerAccess(expression, list[1])
        }
        val operatorFunction = scope.tryLookup(
            listOf("get"),
            CallableSymbol.generateSuffix(list.map { it.type }, true)
        )
        if (operatorFunction != null && operatorFunction.meta.isOperator) {
            operatorFunction as CallableSymbol
            return CallExpression(NameExpression(operatorFunction), list)
        }

        diagnostics.reportUndefinedFunction(node.location, "get", list.map { it.type }, true)
        return ErrorExpression()
    }

    private fun bindBlock(node: BlockNode, isExpression: Boolean, isUnsafe: Boolean = false): BoundNode {
        val statements = ArrayList<Statement>()
        val previous = scope
        scope = Scope(scope)
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
                        statements.add(ExpressionStatement(expression))
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
            BlockExpression(statements, type)
        } else {
            BlockStatement(statements)
        }
    }

    private fun bindAssignmentExpression(node: AssignmentExpressionNode): BoundExpression {
        val name = node.identifierToken.string ?: return ErrorExpression()
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
        return AssignmentExpression(variable, boundExpression)
    }

    private fun bindNameExpression(node: NameExpressionNode): BoundExpression {
        val name = node.identifier.string ?: return ErrorExpression()
        val variable = scope.tryLookup(listOf(name))
        if (variable == null) {
            diagnostics.reportUndefinedName(node.identifier.location, name)
            return ErrorExpression()
        }
        if (variable !is VariableSymbol) {
            diagnostics.reportUndefinedName(node.identifier.location, name)
            return ErrorExpression()
        }
        return NameExpression(variable)
    }

    private fun bindParenthesizedExpression(
        node: ParenthesizedExpressionNode
    ) = bindExpression(node.expression)

    private fun bindLiteralExpression(
        node: LiteralExpressionNode,
        expectedType: TypeSymbol?
    ): LiteralExpression {
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
            is String -> TypeSymbol["String"]!!//TypeSymbol.String
            else -> throw BinderError("Unexpected literal of type ${node.value?.javaClass}")
        }
        return LiteralExpression(when (type) {
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
            return ErrorExpression()
        }
        if (node.operator.kind == SyntaxType.BitAnd) {
            if (!isUnsafe) {
                diagnostics.reportPointerOperationsAreUnsafe(node.operator.location)
                return ErrorExpression()
            }
            if (operand.kind != BoundNode.Kind.VariableExpression) {
                diagnostics.reportReferenceRequiresMutableVar(node.operator.location)
                return ErrorExpression()
            }
            operand as NameExpression
            if (operand.symbol.isReadOnly) {
                diagnostics.reportReferenceRequiresMutableVar(node.operator.location)
                return ErrorExpression()
            }
            return Reference(operand)
        }
        val operator = UnOperator.bind(node.operator.kind, operand.type)
        if (operator == null) {
            val operatorFunction = scope.tryLookup(
                listOf(Translator.unaryOperatorToString(node.operator.kind)),
                CallableSymbol.generateSuffix(listOf(operand.type), true))
            if (operatorFunction != null && operatorFunction.meta.isOperator) {
                operatorFunction as CallableSymbol
                return CallExpression(NameExpression(operatorFunction), listOf(operand))
            }
            diagnostics.reportUnaryOperator(node.operator.location, node.operator.kind, operand.type)
            return ErrorExpression()
        }
        return UnaryExpression(operator, operand)
    }

    private fun bindBinaryExpression(node: BinaryExpressionNode): BoundExpression {

        val left = bindExpression(node.left)
        val right = bindExpression(node.right)

        if (left.type == TypeSymbol.err || right.type == TypeSymbol.err) {
            return ErrorExpression()
        }

        val operator = BiOperator.bind(node.operator.kind, left.type, right.type)
        if (operator != null) {
            return BinaryExpression(left, operator, right)
        }

        if (node.operator.kind == SyntaxType.IsNotEqual) {
            val operatorFunction = scope.tryLookup(
                listOf(Translator.binaryOperatorToString(SyntaxType.IsEqual)),
                CallableSymbol.generateSuffix(listOf(left.type, right.type), true))
            if (operatorFunction != null && operatorFunction.meta.isOperator) {
                operatorFunction as CallableSymbol
                return UnaryExpression(
                    UnOperator(SyntaxType.Bang, UnOperator.Type.Not, TypeSymbol.Bool),
                    CallExpression(NameExpression(operatorFunction), listOf(left, right)))
            }
        } else {
            val operatorFunction = scope.tryLookup(
                listOf(Translator.binaryOperatorToString(node.operator.kind)),
                CallableSymbol.generateSuffix(listOf(left.type, right.type), true))
            if (operatorFunction != null && operatorFunction.meta.isOperator) {
                operatorFunction as CallableSymbol
                return CallExpression(NameExpression(operatorFunction), listOf(left, right))
            }
        }
        diagnostics.reportBinaryOperator(node.operator.location, left.type, node.operator.kind, right.type)
        return ErrorExpression()
    }

    private fun bindDotAccessExpression(node: BinaryExpressionNode, types: List<TypeSymbol>? = null): Pair<BoundExpression, BoundExpression?> {

        if (node.right.kind != SyntaxType.NameExpression) {
            diagnostics.reportCantBeAfterDot(node.right.location)
            return ErrorExpression() to null
        }

        node.right as NameExpressionNode

        fun bindNamespaceAccess(leftName: String, rightName: String): BoundExpression {
            val symbol = scope.tryLookup(leftName.split('.').toMutableList().apply { add(rightName) }, types?.let { CallableSymbol.generateSuffix(it, false) })
            if (symbol != null) {
                symbol as VariableSymbol
                return NameExpression(symbol)
            }
            val leftNamespace = Namespace[leftName]
            if (leftNamespace == null) {
                diagnostics.reportUndefinedName(node.left.location, leftName)
                return ErrorExpression()
            }
            val namespace = Namespace["$leftName.$rightName"]
            if (namespace != null) {
                return NamespaceFieldAccess(namespace)
            }
            diagnostics.reportNotFoundInNamespace(node.right.location, rightName, leftNamespace)
            return ErrorExpression()
        }

        if (node.left.kind == SyntaxType.NameExpression) {
            node.left as NameExpressionNode
            val leftName = node.left.identifier.string ?: return ErrorExpression() to null
            val rightName = node.right.identifier.string ?: return ErrorExpression() to null
            scope.tryLookup(listOf(leftName)) ?: return bindNamespaceAccess(leftName, rightName) to null
        }

        val left = bindExpression(node.left)
        val name = node.right.identifier.string ?: return ErrorExpression() to null

        if (left.kind == BoundNode.Kind.NamespaceFieldAccess) {
            left as NamespaceFieldAccess
            val leftName = left.namespace.path
            return bindNamespaceAccess(leftName, name) to null
        }

        if (left.type.kind == Symbol.Kind.Struct) {
            val i = (left.type as TypeSymbol.StructTypeSymbol).fields.indexOfFirst { it.name == name }
            if (i != -1) {
                return StructFieldAccess(left, i) to left
            }
        }
        if (types != null) {
            val t = ArrayList<TypeSymbol>().apply {
                add(left.type)
                addAll(types)
            }
            val function = scope.tryLookup(listOf(name), CallableSymbol.generateSuffix(t, true))
            if (function != null) {
                function as VariableSymbol
                return NameExpression(function) to left
            }
            diagnostics.reportUndefinedFunction(node.right.location, name, t, true)
            return ErrorExpression() to null
        }
        diagnostics.reportUndefinedName(node.right.location, name)
        return ErrorExpression() to null
    }

    fun bindGlobalStatement(
        statement: Node,
        syntaxTree: SyntaxTree,
        statementBuilder: ArrayList<Statement>,
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
                val namespace = Namespace(syntaxTree.projectPath + '.' + statement.identifier.string, scope)
                scope = namespace
                for (s in statement.members) {
                    bindGlobalStatement(s, syntaxTree, statementBuilder, symbols)
                }
                scope = prev
            }
            SyntaxType.StructDeclaration -> {}
            else -> throw BinderError("Incorrect statement got to be global (${statement.kind})")
        }
    }

    private fun bindField(node: VariableDeclarationNode): TypeSymbol.StructTypeSymbol.Field {
        val isReadOnly = node.keyword.kind == SyntaxType.Val
        var type = node.typeClauseNode?.let { bindTypeClause(it) }
        if (node.initializer != null) {
            diagnostics.reportUnexpectedToken(node.equals!!.location, node.equals.kind, SyntaxType.LineSeparator)
            val initializer = bindExpression(node.initializer)
            type = type ?: initializer.type
            if (!initializer.type.isOfType(type) && initializer.type != TypeSymbol.err) {
                diagnostics.reportWrongType(node.initializer.location, initializer.type, type)
            }
        }
        return TypeSymbol.StructTypeSymbol.Field(node.identifier.string!!, type!!, isReadOnly)
    }

    companion object {

        fun bindGlobalScope(
            previous: GlobalScope?,
            syntaxTrees: Collection<SyntaxTree>
        ): GlobalScope {

            val parentScope = createParentScopes(previous)
            val binder = Binder(parentScope, null, HashMap(), null)

            val symbols = ArrayList<Symbol>()
            val statementBuilder = ArrayList<Statement>()

            for (syntaxTree in syntaxTrees) {
                val strBuilder = StringBuilder()
                var parent = parentScope
                syntaxTree.projectPath.split('.').forEachIndexed { i, s ->
                    val path = if (i == 0) { strBuilder.append(s).toString() }
                        else { strBuilder.append('.').append(s).toString() }
                    parent = Namespace.getOr(path) { Namespace(path, parent) }
                }
            }
            for (syntaxTree in syntaxTrees) {
                binder.diagnostics.append(syntaxTree.diagnostics)
                val prev = binder.scope
                val namespace = Namespace[syntaxTree.projectPath]!!
                binder.scope = namespace
                for (s in syntaxTree.root.members) {
                    if (s.kind == SyntaxType.StructDeclaration) {
                        s as StructDeclarationNode
                        val path = s.identifier.string!!
                        TypeSymbol.map[path] = TypeSymbol.StructTypeSymbol(path, Array(s.fields.size) {
                            binder.bindField(s.fields[it])
                        }, TypeSymbol.Any)
                    }
                }
                binder.scope = prev
            }
            for (syntaxTree in syntaxTrees) {
                binder.diagnostics.append(syntaxTree.diagnostics)
                val prev = binder.scope
                val namespace = Namespace[syntaxTree.projectPath]!!
                binder.scope = namespace
                for (s in syntaxTree.root.members) {
                    binder.bindGlobalStatement(s, syntaxTree, statementBuilder, symbols)
                }
                binder.scope = prev
            }

            var entryFn = symbols.find {
                it.kind == Symbol.Kind.Function &&
                it.meta.isEntry &&
                it.type == TypeSymbol.Fn.entry
            } as CallableSymbol?

            val diagnostics = binder.diagnostics

            if (entryFn == null) {
                if (isRepl) {
                    entryFn = CallableSymbol(
                        "main",
                        arrayOf(),
                        TypeSymbol.Fn(TypeSymbol.Unit, listOf()),
                        "main",
                        null,
                        Symbol.MetaData()
                    )
                } else {
                    entryFn = CallableSymbol(
                        "main",
                        arrayOf(),
                        TypeSymbol.Fn(TypeSymbol.Unit, listOf()),
                        "main",
                        null,
                        Symbol.MetaData()
                    )
                    if (!isSharedLib) {
                        diagnostics.reportNoMainFn()
                    }
                }
            }

            if (previous != null) {
                diagnostics.append(previous.diagnostics)
            }

            return GlobalScope(
                previous,
                diagnostics,
                symbols,
                statementBuilder,
                entryFn)
        }

        private fun createParentScopes(
            previous: GlobalScope?
        ): Scope {

            val stack = Stack<GlobalScope>()
            var prev = previous

            while (prev != null) {
                stack.push(prev)
                prev = prev.previous
            }

            var parent: Scope = createRootScope()

            while (stack.count() > 0) {
                val previous = stack.pop()
                val scope = Scope(parent)
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

        private fun createRootScope(): Scope {
            return Scope(null)
        }

        fun bindFunction(
            parentScope: Scope,
            functions: HashMap<CallableSymbol, BlockStatement?>,
            symbol: CallableSymbol,
            diagnostics: DiagnosticList,
            functionBodies: HashMap<CallableSymbol, BlockStatement?>?
        ) {
            val binder = Binder(parentScope, symbol, functions, functionBodies)
            when {
                symbol.meta.isExtern -> functions[symbol] = null
                symbol.declarationNode!!.lambdaArrow == null -> {
                    val body = binder.bindBlock(symbol.declarationNode.body as BlockNode, false) as BlockStatement
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
                    if (!expression.type.isOfType(symbol.returnType)) {
                        diagnostics.reportWrongType(body.expression.location, expression.type, symbol.returnType)
                    }
                    functions[symbol] = loweredBody
                    functionBodies?.put(symbol, loweredBody)
                }
            }
            diagnostics.append(binder.diagnostics)
        }

        fun bindProgram(
            previous: Program?,
            globalScope: GlobalScope
        ): Program {

            //val parentScope = createParentScopes(globalScope)
            val functions = HashMap<CallableSymbol, BlockStatement?>()
            val diagnostics = DiagnosticList()

            val functionBodies = if (isSharedLib) {
                val tmp = HashMap<CallableSymbol, BlockStatement?>()
                for (symbol in globalScope.symbols) {
                    if (symbol is CallableSymbol) {
                        bindFunction(Scope(symbol.namespace!!), functions, symbol, diagnostics, tmp)
                    }
                }
                tmp
            } else {
                for (symbol in globalScope.symbols) {
                    if (symbol is CallableSymbol) {
                        bindFunction(Scope(symbol.namespace!!), functions, symbol, diagnostics, null)
                    }
                }
                null
            }

            val statement: BlockStatement
            if (isRepl) {
                val statements = globalScope.statements
                if (statements.size == 1) {
                    val s = statements.last()
                    if (s is ExpressionStatement && s.expression.type != TypeSymbol.Unit) {
                        globalScope.symbols.find {
                            it is CallableSymbol &&
                            it.realName == "io.println" &&
                            it.parameters.size == 1 &&
                            it.parameters[0].type == s.expression.type
                        }?.let {
                            statements[statements.lastIndex] = ExpressionStatement(CallExpression(NameExpression(it as CallableSymbol), listOf(s.expression)))
                        }
                    }
                }
                val body = Lowerer.lower(BlockStatement(
                    globalScope.statements
                ))
                functions[globalScope.mainFn] = body
                statement = BlockStatement(listOf())
            }
            else {
                statement = Lowerer.lower(BlockStatement(
                    globalScope.statements
                ))
            }

            return Program(
                previous,
                diagnostics,
                functions,
                statement,
                functionBodies)
        }
    }

    private fun bindFunctionDeclaration(node: FunctionDeclarationNode): CallableSymbol {

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

        val path = if (scope is Namespace) {
            (scope as Namespace).path + '.' + node.identifier.string!!
        } else {
            function!!.mangledName().substringBeforeLast('.') + '.' + Symbol.genFnUID()
        }
        val function = CallableSymbol(node.identifier.string!!, params.toTypedArray(), TypeSymbol.Fn(type, params.map { it.type }), path, node, meta)
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
                    val expression = bindLiteralExpression(annotation.value as LiteralExpressionNode, TypeSymbol["String"]!!/*TypeSymbol.String*/)
                    meta.cname = expression.value as String
                }
                else -> {
                    diagnostics.reportInvalidAnnotation(annotation.location)
                }
            }
        }

        if (!scope.tryDeclare(function)) {
            diagnostics.reportFunctionAlreadyDeclared(node.identifier.location, function.name, function.parameters.map { it.type }, function.meta.isExtension)
        }

        return function
    }
}