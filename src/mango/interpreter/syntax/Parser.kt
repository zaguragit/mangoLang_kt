package mango.interpreter.syntax

import mango.compilation.DiagnosticList
import mango.interpreter.syntax.nodes.*
import mango.interpreter.text.SourceText
import mango.interpreter.text.TextLocation
import mango.interpreter.text.TextSpan

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
        return if (index >= tokens.size) {
            tokens[tokens.lastIndex]
        } else tokens[index]
    }

    private inline fun peekOverLineSeparators(offset: Int): Token {
        var i = 0
        var o = position
        while (o < tokens.size) {
            val p = tokens[o++]
            if (p.kind != SyntaxType.LineSeparator) {
                if (i == offset) {
                    return p
                }
                i++
            }
        }
        return tokens[tokens.lastIndex]
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
            member?.let { members.add(it) }

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
        while (current.kind == SyntaxType.OpenBracket) {
            val left = next()
            if (left.location.startLineI > lastAnnotationLine + 1 && list.isNotEmpty()) {
                val last = list.last()
                diagnostics.reportInvalidAnnotation(last.location)
            }
            skipSeparators()
            if (current.kind == SyntaxType.Identifier) {
                val identifier = next()
                var colon: Token? = null
                var expression: Node? = null
                if (current.kind == SyntaxType.Colon) {
                    colon = next()
                    expression = parseStringLiteral()
                }
                skipSeparators()
                val right = match(SyntaxType.ClosedBracket)
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

    private fun parseLambda(annotations: Collection<AnnotationNode>): LambdaNode {
        var params: SeparatedNodeList<ParameterNode>? = null
        if (current.kind == SyntaxType.OpenParentheses) {
            position++
            params = parseParamList()
            match(SyntaxType.ClosedParentheses)
        }

        var type: TypeClauseNode? = null
        if (current.kind == SyntaxType.Identifier) {
            type = parseTypeClause()
        }
        return if (annotations.find { it.identifier.string == "extern" } == null) {
            skipSeparators()
            val arrow = match(SyntaxType.LambdaArrow)
            val body = parseStatement()
            LambdaNode(syntaxTree, params, type, arrow, body)
        } else {
            LambdaNode(syntaxTree, params, type, null, null)
        }
    }

    private fun parseParamList(): SeparatedNodeList<ParameterNode> {
        val nodesNSeparators = ArrayList<Node>()

        skipSeparators()
        while (
            current.kind != SyntaxType.ClosedParentheses &&
            current.kind != SyntaxType.EOF
        ) {
            val param = parseParam()
            nodesNSeparators.add(param)

            skipSeparators()

            if (current.kind == SyntaxType.Comma) {
                val comma = match(SyntaxType.Comma)
                nodesNSeparators.add(comma)
            } else {
                break
            }
            skipSeparators()
        }

        return SeparatedNodeList(nodesNSeparators)
    }

    private fun parseParam(): ParameterNode {
        val identifier = match(SyntaxType.Identifier)
        val type = parseTypeClause()
        return ParameterNode(syntaxTree, identifier, type)
    }

    private fun parseGlobalStatement(): TopLevelNode? {
        skipSeparators()
        val statement = if (current.kind == SyntaxType.NamespaceToken) {
            parseNamespace()
        } else parseStatement()
        if (statement !is TopLevelNode) {
            diagnostics.reportStatementCantBeGlobal(statement.location, statement.kind)
            return null
        }
        return statement
    }

    private fun parseUseStatement(): TopLevelNode {

        val keyword = match(SyntaxType.Use)

        val directories = ArrayList<Token>()
        var isInclude = false

        while (current.kind != SyntaxType.LineSeparator) {
            directories.add(match(SyntaxType.Identifier))
            if (current.kind == SyntaxType.LineSeparator) {
                position++
                break
            } else if (current.kind == SyntaxType.Star) {
                position++
                isInclude = true
                break
            } else {
                match(SyntaxType.Dot)
            }
        }

        return UseStatementNode(
            syntaxTree,
            keyword,
            directories,
            isInclude)
    }

    private fun parseTypeDeclaration(annotations: Collection<AnnotationNode>): Node {
        val keyword = match(SyntaxType.Type)
        val identifier = match(SyntaxType.Identifier)
        val parent = if (current.kind == SyntaxType.Colon) {
            next()
            parseTypeClause()
        } else null
        val fields = if (current.kind == SyntaxType.OpenBrace) {
            next()
            parseInsideBraces {
                parseValVarDeclaration(parseAnnotations())
            }.also { match(SyntaxType.ClosedBrace) }
        } else null
        return TypeDeclarationNode(syntaxTree, keyword, identifier, parent, fields)
    }

    private fun parseNamespace(): NamespaceStatementNode {
        val keyword = match(SyntaxType.NamespaceToken)
        val identifier = match(SyntaxType.Identifier)

        val openBrace = match(SyntaxType.OpenBrace)

        val statements = parseInsideBraces {
            parseGlobalStatement()
        }

        val closedBrace = match(SyntaxType.ClosedBrace)
        return NamespaceStatementNode(syntaxTree, keyword, identifier, openBrace, statements, closedBrace)
    }

    private fun parseStatement(): Node {
        skipSeparators()
        val annotations = parseAnnotations()
        return when (current.kind) {
            SyntaxType.Val, SyntaxType.Var -> parseValVarDeclaration(annotations)
            SyntaxType.Loop -> parseLoop()
            SyntaxType.Break -> parseBreakStatement()
            SyntaxType.Continue -> parseContinueStatement()
            SyntaxType.Return -> parseReturnStatement()
            SyntaxType.Use -> parseUseStatement()
            SyntaxType.Type -> parseTypeDeclaration(annotations)
            else -> parseAssignmentStatement()
        }
    }

    private fun parseAssignmentStatement(): Node {
        val left = parseExpression()
        val k = peek(0).kind
        if (isAssignee(left) && (
            k == SyntaxType.Equals ||
            k == SyntaxType.PlusEquals ||
            k == SyntaxType.MinusEquals ||
            k == SyntaxType.DivEquals ||
            k == SyntaxType.TimesEquals ||
            k == SyntaxType.RemEquals ||
            k == SyntaxType.OrEquals ||
            k == SyntaxType.AndEquals
        )) {
            val operatorToken = next()
            val right = parseExpression()
            return AssignmentNode(syntaxTree, left, operatorToken, right)
        }
        return ExpressionStatementNode(syntaxTree, left)
    }

    private fun isAssignee(node: Node) =
        node.kind == SyntaxType.NameExpression ||
        node.kind == SyntaxType.IndexExpression ||
        node.kind == SyntaxType.BinaryExpression && (node as BinaryExpressionNode).operator.kind == SyntaxType.Dot

    private fun parseValVarDeclaration(annotations: Collection<AnnotationNode>): ValVarDeclarationNode {
        skipSeparators()
        val expected = if (current.kind == SyntaxType.Val) { SyntaxType.Val } else { SyntaxType.Var }
        val keyword = match(expected)
        val isError = current.kind == SyntaxType.LineSeparator


        val extensionType: TypeClauseNode?
        if (peek(1).kind == SyntaxType.Dot || peek(1).kind == SyntaxType.LessThan) {
            extensionType = parseTypeClause()
            next()
        } else { extensionType = null }


        val identifier = match(SyntaxType.Identifier)
        if (isError) {
            diagnostics.reportDeclarationAndNameOnSameLine(identifier.location)
        }

        val typeClause = if (extensionType == null && peek(1).kind != SyntaxType.LambdaArrow) parseOptionalValueTypeClause() else null
        var equals: Token? = null
        var initializer: Node? = null

        if (typeClause == null && (
            extensionType != null ||
            current.kind == SyntaxType.OpenParentheses ||
            peek(1).kind == SyntaxType.OpenParentheses ||
            current.kind == SyntaxType.LambdaArrow ||
            peek(1).kind == SyntaxType.LambdaArrow
        )) {
            if (current.kind == SyntaxType.Equals) {
                equals = match(SyntaxType.Equals)
            }
            initializer = parseLambda(annotations)
        } else {
            if (current.kind == SyntaxType.Equals) {
                equals = match(SyntaxType.Equals)
                initializer = parseExpression()
            } else if (typeClause == null) {
                diagnostics.reportCantInferType(identifier.location)
            }
        }

        return ValVarDeclarationNode(syntaxTree, keyword, identifier, typeClause, equals, initializer, extensionType, annotations)
    }

    private fun isComplexTypeClause(): Pair<Boolean, Int> {
        var i = 0
        if (peek(i++).kind != SyntaxType.Identifier) return false to 0
        if (peek(i++).kind != SyntaxType.LessThan) return false to 0
        while (peek(i).kind == SyntaxType.Identifier || peek(i).kind == SyntaxType.Comma) i++
        if (peek(i).kind != SyntaxType.MoreThan) return false to 0
        return true to i
    }

    private fun parseOptionalValueTypeClause(): TypeClauseNode? {
        return if (current.kind == SyntaxType.Identifier) parseTypeClause() else null
    }

    private fun parseTypeClause(): TypeClauseNode {
        val identifier = match(SyntaxType.Identifier)
        var start: Token? = null
        var types: SeparatedNodeList<TypeClauseNode>? = null
        var end: Token? = null
        if (current.kind == SyntaxType.LessThan) {
            start = match(SyntaxType.LessThan)
            types = parseTypeList()
            end = match(SyntaxType.MoreThan)
        }
        return TypeClauseNode(syntaxTree, identifier, start, types, end)
    }

    private fun parseTypeList(): SeparatedNodeList<TypeClauseNode> {
        val nodesNSeparators = ArrayList<Node>()

        while (
            current.kind != SyntaxType.ClosedParentheses &&
            current.kind != SyntaxType.EOF
        ) {
            val param = parseTypeClause()
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

    private fun parseExpressionStatement(): ExpressionStatementNode {
        val expression = parseExpression()
        return ExpressionStatementNode(syntaxTree, expression)
    }

    private fun parseBlock(): BlockNode {
        val openBrace = match(SyntaxType.OpenBrace)

        val statements = parseInsideBraces {
            parseStatement()
        }

        val closedBrace = match(SyntaxType.ClosedBrace)
        return BlockNode(syntaxTree, null, openBrace, statements, closedBrace, false)
    }

    private fun parseElseClause(): ElseClauseNode? {
        skipSeparators()
        if (current.kind != SyntaxType.Colon) {
            return null
        }
        val colon = next()
        val then = parseStatement()
        return ElseClauseNode(syntaxTree, colon, then)
    }

    private fun parseLoop(): Node {
        val keyword = match(SyntaxType.Loop)
        if (peek(1).kind == SyntaxType.Colon) {
            val identifier = match(SyntaxType.Identifier)
            val inToken = match(SyntaxType.Colon)
            val lowerBound = parseExpression()
            val rangeToken = match(SyntaxType.Range)
            val upperBound = parseExpression()
            val body = parseStatement()
            return ForStatementNode(syntaxTree, keyword, identifier, inToken, lowerBound, rangeToken, upperBound, body)
        }
        val body = parseStatement()
        return LoopStatementNode(syntaxTree, keyword, body)
    }

    private fun parseBreakStatement(): Node {
        val keyword = match(SyntaxType.Break)
        return BreakStatementNode(syntaxTree, keyword)
    }

    private fun parseContinueStatement(): Node {
        val keyword = match(SyntaxType.Continue)
        return ContinueStatementNode(syntaxTree, keyword)
    }

    private fun parseReturnStatement(): Node {
        val keyword = match(SyntaxType.Return)
        val keywordLine = sourceText.getLineI(keyword.span.start)
        val currentLine = sourceText.getLineI(current.span.start)
        val expression = if (currentLine == keywordLine && current.kind != SyntaxType.EOF) {
            parseExpression()
        } else null
        return ReturnStatementNode(syntaxTree, keyword, expression)
    }

    private fun parseExpression() = parseBinaryExpression()

    private fun parsePostUnaryExpression(pre: Node, parentPrecedence: Int): Node {
        if (parentPrecedence != SyntaxType.Dot.getBinaryOperatorPrecedence()) {
            when (current.kind) {
                SyntaxType.OpenBracket -> {
                    val open = match(SyntaxType.OpenBracket)
                    val arguments = parseArguments()
                    val closed = match(SyntaxType.ClosedBracket)
                    return IndexExpressionNode(syntaxTree, pre, open, arguments, closed)
                }
                SyntaxType.OpenParentheses -> {
                    val open = match(SyntaxType.OpenParentheses)
                    val arguments = parseArguments()
                    val closed = match(SyntaxType.ClosedParentheses)
                    return CallExpressionNode(syntaxTree, pre, open, arguments, closed)
                }
                SyntaxType.As -> {
                    val keyword = match(SyntaxType.As)
                    val type = parseTypeClause()
                    return CastExpressionNode(syntaxTree, pre, keyword, type)
                }
                SyntaxType.QuestionMark -> if (parentPrecedence < SyntaxType.QuestionMark.getBinaryOperatorPrecedence()) {
                    val q = match(SyntaxType.QuestionMark)
                    val then = parseStatement()
                    val elseClause = parseElseClause()
                    return IfNode(syntaxTree, pre, q, then, elseClause)
                }
            }
        }
        return pre
    }

    private fun parseBinaryExpression(parentPrecedence: Int = 0): Node {

        val unaryOperatorPrecedence = current.kind.getUnaryOperatorPrecedence()

        var left = if (unaryOperatorPrecedence != 0 && unaryOperatorPrecedence >= parentPrecedence) {
            val operatorToken = next()
            val operand = parsePostUnaryExpression(parseBinaryExpression(unaryOperatorPrecedence), parentPrecedence)
            UnaryExpressionNode(syntaxTree, operatorToken, operand)
        } else {
            val primary = parsePrimaryExpression()
            primary
        }

        while (true) {
            left = parsePostUnaryExpression(left, parentPrecedence)
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
        SyntaxType.OpenParentheses -> parseLambda(listOf())
        SyntaxType.False, SyntaxType.True -> parseBooleanLiteral()
        SyntaxType.I8,
        SyntaxType.I16,
        SyntaxType.I32,
        SyntaxType.I64 -> parseIntLiteral()
        SyntaxType.Float,
        SyntaxType.Double-> parseFloatLiteral()
        SyntaxType.String -> parseStringLiteral()
        SyntaxType.Char -> parseCharLiteral()
        SyntaxType.Unsafe -> parseUnsafeExpression()
        SyntaxType.OpenBrace -> parseBlock()
        else -> parseNameExpression()
    }

    private fun parseBooleanLiteral(): LiteralExpressionNode {
        val token = next()
        val isTrue = token.kind == SyntaxType.True
        return LiteralExpressionNode(syntaxTree, token, isTrue)
    }

    private fun parseIntLiteral(): LiteralExpressionNode {
        val token = next()
        return LiteralExpressionNode(syntaxTree, if (
            token.kind == SyntaxType.I8 ||
            token.kind == SyntaxType.I16 ||
            token.kind == SyntaxType.I32 ||
            token.kind == SyntaxType.I64) {
            token
        } else {
            diagnostics.reportUnexpectedToken(current.location, current.kind, SyntaxType.I32)
            Token(syntaxTree, SyntaxType.I32, current.position).apply { isMissing = true }
        })
    }

    private fun parseUnsafeExpression(): Node {
        val keyword = match(SyntaxType.Unsafe)
        val openBrace = match(SyntaxType.OpenBrace)

        val statements = parseInsideBraces {
            parseStatement()
        }

        val closedBrace = match(SyntaxType.ClosedBrace)
        return BlockNode(syntaxTree, keyword, openBrace, statements, closedBrace, true)
    }

    private fun parseFloatLiteral(): Node {
        val token = next()
        return LiteralExpressionNode(syntaxTree, if (
            token.kind == SyntaxType.Float ||
            token.kind == SyntaxType.Double) {
            token
        } else {
            diagnostics.reportUnexpectedToken(current.location, current.kind, SyntaxType.Float)
            Token(syntaxTree, SyntaxType.Float, current.position).apply { isMissing = true }
        })
    }

    private fun parseStringLiteral() = LiteralExpressionNode(syntaxTree, match(SyntaxType.String))

    private fun parseCharLiteral() = LiteralExpressionNode(syntaxTree, match(SyntaxType.Char))

    private fun parseNameExpression(): Node {
        val (_, i) = isComplexTypeClause()

        if (peek(i + 1).kind == SyntaxType.OpenBrace) {
            if (peekOverLineSeparators(i + 2).kind == SyntaxType.Identifier) {
                if (peekOverLineSeparators(i + 3).kind == SyntaxType.Colon) {
                    return parseStructInitialization(parseTypeClause())
                }
            }
            if (levelContains(SyntaxType.Comma)) {
                return parseCollectionInitialization(parseTypeClause())
            }
        }

        val identifier = match(SyntaxType.Identifier)

        return NameExpressionNode(syntaxTree, identifier)
    }

    private fun levelContains(type: SyntaxType): Boolean {
        var i = 1
        var braceDepth = 0
        var bracketDepth = 0
        var parenthesesDepth = 0
        while (true) {
            when (peek(i).kind) {
                SyntaxType.OpenBrace -> braceDepth++
                SyntaxType.ClosedBrace -> {
                    braceDepth--
                    if (braceDepth == 0) return false
                }
                SyntaxType.OpenBracket -> bracketDepth++
                SyntaxType.ClosedBracket -> {
                    bracketDepth--
                }
                SyntaxType.OpenParentheses -> parenthesesDepth++
                SyntaxType.ClosedParentheses -> {
                    parenthesesDepth--
                }
                SyntaxType.EOF -> return false
                else -> if (parenthesesDepth == 0 &&
                            bracketDepth == 0 &&
                            braceDepth == 0 &&
                            peek(i).kind == type) return true
            }
            i++
        }
    }

    private fun parseStructInitialization(type: TypeClauseNode): Node {
        val openBrace = match(SyntaxType.OpenBrace)
        val statements = parseInsideBraces {
            val name = match(SyntaxType.Identifier)
            val colon = match(SyntaxType.Colon)
            val expression = parseExpression()
            AssignmentNode(syntaxTree, name, colon, expression)
        }
        val closedBrace = match(SyntaxType.ClosedBrace)
        return StructInitializationNode(syntaxTree, type, openBrace, statements, closedBrace)
    }

    private fun parseCollectionInitialization(type: TypeClauseNode): Node {
        val openBrace = match(SyntaxType.OpenBrace)
        val expressions = parseArguments()
        val closedBrace = match(SyntaxType.ClosedBrace)
        return CollectionInitializationNode(syntaxTree, type, openBrace, expressions, closedBrace)
    }

    private fun parseArguments(): SeparatedNodeList<Node> {

        val nodesNSeparators = ArrayList<Node>()

        skipSeparators()
        while (
            current.kind != SyntaxType.ClosedParentheses &&
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
            skipSeparators()
        }

        return SeparatedNodeList(nodesNSeparators)
    }

    private fun <T : Node> parseInsideBraces(
        fn: () -> T?
    ): ArrayList<T> {
        skipSeparators()
        val statements = ArrayList<T>()
        while (
            current.kind != SyntaxType.EOF &&
            current.kind != SyntaxType.ClosedBrace
        ) {
            skipSeparators()
            val startToken = current

            fn()?.let { statements.add(it) }

            skipSeparators()
            if (startToken == current) {
                skipSeparators()
                next()
            }
        }
        return statements
    }
}