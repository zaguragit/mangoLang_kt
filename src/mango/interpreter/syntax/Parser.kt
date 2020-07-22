package mango.interpreter.syntax

import mango.compilation.DiagnosticList
import mango.interpreter.syntax.nodes.*
import mango.interpreter.text.SourceText
import mango.interpreter.text.TextLocation
import mango.interpreter.text.TextSpan
import mango.isRepl

class Parser(
    val syntaxTree: SyntaxTree
) {

    val sourceText: SourceText = syntaxTree.sourceText
    val diagnostics = DiagnosticList()
    private val tokens: Array<Token>
    var position = 0

    init {
        val tokens = ArrayList<Token>()
        val lexer = Lexer(syntaxTree)
        var token: Token
        do {
            token = lexer.nextToken()
            if (token.kind != SyntaxType.Bad &&
                token.kind != SyntaxType.SingleLineComment &&
                token.kind != SyntaxType.MultilineComment) {
                tokens.add(token)
            }
        }
        while (token.kind != SyntaxType.EOF)

        this.tokens = tokens.toTypedArray()
        diagnostics.append(lexer.diagnostics)
    }

    private inline fun peek(offset: Int): Token {
        val index = position + offset
        return if (index >= tokens.size) tokens[tokens.lastIndex] else tokens[index]
    }

    private inline val current get() = peek(0)

    private inline fun next(): Token {
        val current = current
        position++
        return current
    }

    private fun match(type: SyntaxType): Token = when (current.kind) {
        type -> next()
        SyntaxType.LineSeparator -> {
            skipSeparators()
            match(type)
        }
        else -> {
            diagnostics.reportUnexpectedToken(current.location, current.kind, type)
            Token(syntaxTree, type, current.position).apply { isMissing = true }
        }
    }

    private inline fun skipSeparators() {
        while (current.kind == SyntaxType.LineSeparator) {
            position++
        }
    }

    fun parseCompilationUnit(): NamespaceNode {
        val statement = parseMembers()
        match(SyntaxType.EOF)
        return NamespaceNode(syntaxTree, statement)
    }

    private fun parseMembers(): Collection<TopLevelNode> {
        val members = ArrayList<TopLevelNode>()

        while (current.kind != SyntaxType.EOF) {
            val startToken = current

            val member = parseGlobalStatement()
            members.add(member)

            if (startToken == current) {
                next()
            }

            skipSeparators()
        }

        return members
    }

    private fun parseAnnotations(): Collection<AnnotationNode> {
        val list = ArrayList<AnnotationNode>()
        var lastAnnotationLine = 0
        while (current.kind == SyntaxType.OpenSquareBracket) {
            val left = next()
            if (left.location.startLineI > lastAnnotationLine + 1 && list.isNotEmpty()) {
                val last = list.last()
                diagnostics.reportInvalidAnnotation(last.location)
            }
            skipSeparators()
            if (current.kind == SyntaxType.Identifier) {
                val identifier = next()
                var colon: Token? = null
                var expression: ExpressionNode? = null
                if (current.kind == SyntaxType.Colon) {
                    colon = next()
                    expression = parseStringLiteral()
                }
                skipSeparators()
                val right = match(SyntaxType.ClosedSquareBracket)
                if (current.kind == SyntaxType.LineSeparator) {
                    position++
                }
                lastAnnotationLine = right.location.endLineI
                list.add(AnnotationNode(syntaxTree, left, identifier, colon, expression, right))
            } else {
                diagnostics.reportInvalidAnnotation(TextLocation(left.location.text, TextSpan(left.span.start, 2)))
            }
        }
        return list
    }

    private fun parseFunctionDeclaration(annotations: Collection<AnnotationNode>): FunctionDeclarationNode {
        val keyword = match(SyntaxType.Fn)
        if (current.kind == SyntaxType.LineSeparator) {
            diagnostics.reportDeclarationAndNameOnSameLine(keyword.location)
        }
        val extensionType: TypeClauseNode?
        if (peek(1).kind == SyntaxType.Dot) {
            extensionType = parseTypeClause()
            next()
        } else { extensionType = null }
        val identifier = match(SyntaxType.Identifier)

        var params: SeparatedNodeList<ParameterNode>? = null
        skipSeparators()
        if (current.kind == SyntaxType.OpenRoundedBracket) {
            position++
            params = parseParamList()
            match(SyntaxType.ClosedRoundedBracket)
        }

        skipSeparators()
        var type: TypeClauseNode? = null
        if (current.kind == SyntaxType.Identifier) {
            type = parseTypeClause()
        }
        return if (annotations.find { it.identifier.string == "extern" } == null) {
            skipSeparators()
            var lambdaArrow: Token? = null
            val body: StatementNode
            if (current.kind == SyntaxType.LambdaArrow) {
                lambdaArrow = next()
                body = parseExpressionStatement()
            } else {
                body = parseBlockStatement()
            }
            FunctionDeclarationNode(syntaxTree, keyword, identifier, type, params, lambdaArrow, body, annotations, extensionType)
        } else {
            FunctionDeclarationNode(syntaxTree, keyword, identifier, type, params, null, null, annotations, extensionType)
        }
    }

    private fun parseParamList(): SeparatedNodeList<ParameterNode> {
        val nodesNSeparators = ArrayList<Node>()

        while (
            current.kind != SyntaxType.ClosedRoundedBracket &&
            current.kind != SyntaxType.EOF
        ) {
            val param = parseParam()
            nodesNSeparators.add(param)

            if (current.kind == SyntaxType.Comma) {
                val comma = match(SyntaxType.Comma)
                nodesNSeparators.add(comma)
            } else {
                break
            }
        }

        return SeparatedNodeList(nodesNSeparators)
    }

    private fun parseParam(): ParameterNode {
        val identifier = match(SyntaxType.Identifier)
        val type = parseTypeClause()
        return ParameterNode(syntaxTree, identifier, type)
    }

    private fun parseGlobalStatement(): TopLevelNode {
        skipSeparators()
        val statement = if (current.kind == SyntaxType.NamespaceToken) {
            parseNamespace()
        } else parseStatement()
        if (statement !is TopLevelNode) {
            if (!isRepl) {
                diagnostics.reportStatementCantBeGlobal(statement.location, statement.kind)
            }
            return ReplStatementNode(statement)
        }
        return statement
    }

    private fun parseUseStatement(): TopLevelNode {

        val keyword = match(SyntaxType.Use)

        val directories = ArrayList<Token>()
        var isInclude = false

        while (current.kind != SyntaxType.LineSeparator) {
            if (current.kind == SyntaxType.Identifier) {
                directories.add(current)
                position++
                if (current.kind == SyntaxType.Dot) {
                    position++
                } else if (current.kind == SyntaxType.LineSeparator) {
                    position++
                    break
                } else if (current.kind == SyntaxType.Star) {
                    position++
                    isInclude = true
                    break
                } else {
                    diagnostics.reportIncorrectUseStatement(next().location)
                    break
                }
            } else {
                diagnostics.reportIncorrectUseStatement(current.location)
                break
            }
        }

        return UseStatementNode(
                syntaxTree,
                keyword,
                directories,
                isInclude)
    }

    private fun parseNamespace(): NamespaceStatementNode {
        val keyword = match(SyntaxType.NamespaceToken)
        val identifier = match(SyntaxType.Identifier)

        val openBrace = match(SyntaxType.OpenCurlyBracket)

        val statements = ArrayList<TopLevelNode>()

        while (
            current.kind != SyntaxType.EOF &&
            current.kind != SyntaxType.ClosedCurlyBracket
        ) {
            skipSeparators()
            val startToken = current

            val statement = parseGlobalStatement()
            statements.add(statement)

            skipSeparators()
            if (startToken == current) {
                skipSeparators()
                next()
            }
        }

        val closedBrace = match(SyntaxType.ClosedCurlyBracket)
        return NamespaceStatementNode(syntaxTree, keyword, identifier, openBrace, statements, closedBrace)
    }

    private fun parseStatement(): StatementNode {
        skipSeparators()
        val annotations = parseAnnotations()
        return when (current.kind) {
            SyntaxType.OpenCurlyBracket -> parseBlockStatement()
            SyntaxType.Val, SyntaxType.Var -> parseVariableDeclaration()
            SyntaxType.If -> parseIfStatement()
            SyntaxType.While -> parseWhileStatement()
            SyntaxType.For -> parseForStatement()
            SyntaxType.Break -> parseBreakStatement()
            SyntaxType.Continue -> parseContinueStatement()
            SyntaxType.Return -> parseReturnStatement()
            SyntaxType.Fn -> parseFunctionDeclaration(annotations)
            SyntaxType.Use -> parseUseStatement()
            else -> parseExpressionStatement()
        }
    }

    private fun parseVariableDeclaration(): VariableDeclarationNode {
        skipSeparators()
        val expected = if (current.kind == SyntaxType.Val) { SyntaxType.Val }
            else { SyntaxType.Var }
        val keyword = match(expected)
        val isError = current.kind == SyntaxType.LineSeparator
        val identifier = match(SyntaxType.Identifier)
        if (isError) {
            diagnostics.reportDeclarationAndNameOnSameLine(identifier.location)
        }
        val typeClause = parseOptionalValueTypeClause()
        val equals = match(SyntaxType.Equals)
        val initializer = parseExpression()
        return VariableDeclarationNode(syntaxTree, keyword, identifier, typeClause, equals, initializer)
    }

    private fun parseOptionalValueTypeClause(): TypeClauseNode? {
        if (current.kind == SyntaxType.Equals) {
            return null
        }
        return parseTypeClause()
    }

    private fun parseTypeClause(): TypeClauseNode {
        val identifier = match(SyntaxType.Identifier)
        return TypeClauseNode(syntaxTree, identifier)
    }

    private fun parseExpressionStatement(): ExpressionStatementNode {
        val expression = parseExpression()
        return ExpressionStatementNode(syntaxTree, expression)
    }

    private fun parseBlockStatement(): BlockStatementNode {
        val statements = ArrayList<StatementNode>()
        val openBrace = match(SyntaxType.OpenCurlyBracket)

        while (
            current.kind != SyntaxType.EOF &&
            current.kind != SyntaxType.ClosedCurlyBracket
        ) {
            skipSeparators()
            val startToken = current

            val statement = parseStatement()
            statements.add(statement)

            skipSeparators()
            if (startToken == current) {
                skipSeparators()
                next()
            }
        }
        val closeBrace = match(SyntaxType.ClosedCurlyBracket)
        return BlockStatementNode(syntaxTree, openBrace, statements, closeBrace)
    }

    private fun parseIfStatement(): IfStatementNode {
        val keyword = match(SyntaxType.If)
        val condition = parseExpression()
        val statement = parseBlockStatement()
        val elseClause = parseElseClause()
        return IfStatementNode(syntaxTree, keyword, condition, statement, elseClause)
    }

    private fun parseElseClause(): ElseClauseNode? {
        skipSeparators()
        if (current.kind != SyntaxType.Else) {
            return null
        }
        val keyword = next()
        val isIfNext = current.kind == SyntaxType.If
        val statement = if (isIfNext) { parseIfStatement() } else { parseBlockStatement() }
        return ElseClauseNode(syntaxTree, keyword, statement)
    }

    private fun parseWhileStatement(): WhileStatementNode {
        val keyword = match(SyntaxType.While)
        val condition = parseExpression()
        val body = parseBlockStatement()
        return WhileStatementNode(syntaxTree, keyword, condition, body)
    }

    private fun parseForStatement(): ForStatementNode {
        val keyword = match(SyntaxType.For)
        val identifier = match(SyntaxType.Identifier)
        val inToken = match(SyntaxType.In)
        val lowerBound = parseExpression()
        val rangeToken = match(SyntaxType.Range)
        val upperBound = parseExpression()
        val body = parseBlockStatement()
        return ForStatementNode(syntaxTree, keyword, identifier, inToken, lowerBound, rangeToken, upperBound, body)
    }

    private fun parseBreakStatement(): StatementNode {
        val keyword = match(SyntaxType.Break)
        return BreakStatementNode(syntaxTree, keyword)
    }

    private fun parseContinueStatement(): StatementNode {
        val keyword = match(SyntaxType.Continue)
        return ContinueStatementNode(syntaxTree, keyword)
    }

    private fun parseReturnStatement(): StatementNode {
        val keyword = match(SyntaxType.Return)
        val keywordLine = sourceText.getLineI(keyword.span.start)
        val currentLine = sourceText.getLineI(current.span.start)
        val expression = if (currentLine == keywordLine && current.kind != SyntaxType.EOF) {
            parseExpression()
        } else null
        return ReturnStatementNode(syntaxTree, keyword, expression)
    }

    private fun parseExpression() = parseAssignmentExpression()

    private fun parseAssignmentExpression(): ExpressionNode {
        if (peek(0).kind == SyntaxType.Identifier &&
            peek(1).kind == SyntaxType.Equals) {
            val identifierToken = next()
            val operatorToken = next()
            val right = parseAssignmentExpression()
            return AssignmentExpressionNode(syntaxTree, identifierToken, operatorToken, right)
        }
        return parseBinaryExpression()
    }

    private fun parseBinaryExpression(parentPrecedence: Int = 0): ExpressionNode {
        var left: ExpressionNode

        val unaryOperatorPrecedence = current.kind.getUnaryOperatorPrecedence()
        left = if (unaryOperatorPrecedence != 0 && unaryOperatorPrecedence >= parentPrecedence) {
            val operatorToken = next()
            val operand = parseBinaryExpression(unaryOperatorPrecedence)
            UnaryExpressionNode(syntaxTree, operatorToken, operand)
        } else {
            parsePrimaryExpression()
        }

        while (true) {
            val precedence = current.kind.getBinaryOperatorPrecedence()
            if (precedence == 0 || precedence <= parentPrecedence)
                break
            val operatorToken = next()
            val right = parseBinaryExpression(precedence)
            left = BinaryExpressionNode(syntaxTree, left, operatorToken, right)
        }
        return left
    }

    private fun parsePrimaryExpression() = when (current.kind) {
        SyntaxType.OpenRoundedBracket -> parseParenthesizedExpression()
        SyntaxType.False, SyntaxType.True -> parseBooleanLiteral()
        SyntaxType.Int -> parseNumberLiteral()
        SyntaxType.String -> parseStringLiteral()
        SyntaxType.In -> parseNumberLiteral()
        else -> parseNameExpression()
    }

    private fun parseParenthesizedExpression(): ParenthesizedExpressionNode {
        val left = match(SyntaxType.OpenRoundedBracket)
        val expression = parseExpression()
        val right = match(SyntaxType.ClosedRoundedBracket)
        return ParenthesizedExpressionNode(syntaxTree, left, expression, right)
    }

    private fun parseBooleanLiteral(): LiteralExpressionNode {
        val token = next()
        val isTrue = token.kind == SyntaxType.True
        return LiteralExpressionNode(syntaxTree, token, isTrue)
    }

    private fun parseNumberLiteral() = LiteralExpressionNode(syntaxTree, match(SyntaxType.Int))

    private fun parseStringLiteral() = LiteralExpressionNode(syntaxTree, match(SyntaxType.String))

    private fun parseNameExpression(): ExpressionNode {
        val identifier = match(SyntaxType.Identifier)
        return when (peek(0).kind) {
            SyntaxType.OpenRoundedBracket -> {
                val leftBracket = match(SyntaxType.OpenRoundedBracket)
                val arguments = parseArguments()
                val rightBracket = match(SyntaxType.ClosedRoundedBracket)
                CallExpressionNode(syntaxTree, identifier, leftBracket, arguments, rightBracket)
            }
            else -> NameExpressionNode(syntaxTree, identifier)
        }
    }

    private fun parseArguments(): SeparatedNodeList<ExpressionNode> {

        val nodesNSeparators = ArrayList<Node>()

        while (
            current.kind != SyntaxType.ClosedRoundedBracket &&
            current.kind != SyntaxType.EOF
        ) {
            val expression = parseExpression()
            nodesNSeparators.add(expression)

            skipSeparators()
            if (current.kind == SyntaxType.Comma) {
                val comma = match(SyntaxType.Comma)
                nodesNSeparators.add(comma)
            } else {
                break
            }
        }

        return SeparatedNodeList(nodesNSeparators)
    }
}