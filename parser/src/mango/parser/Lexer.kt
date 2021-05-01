package mango.parser

import shared.DiagnosticList
import shared.text.TextLocation
import shared.text.TextSpan

class Lexer(
    private val textFile: TextFile
) {
    private val sourceText = textFile.sourceText

    private var position = 0

    val diagnostics = DiagnosticList()

    private inline val current get() = peek(0)
    private inline fun lookAhead() = peek(1)
    private inline fun peek(offset: Int): Char {
        val i = position + offset
        return if (i >= sourceText.length) '\u0000' else sourceText[i]
    }

    fun nextToken(): Token {

        while (current == ' ' || current == '\t') { position++ }

        if (current.isLetter() || current == '_') {
            return readIdentifierOrKeyword()
        }

        return when (current) {
            '➞', '→', '➜', '➙', '➝', '➡', '⇾', '➤' -> Token(textFile, SyntaxType.LambdaArrow, position++, string = current.toString())
            //'✓', '✔' -> Token(textFile, SyntaxType.True, position++, string = current.toString())
            //'✕', '✖', '✗', '✘' -> Token(textFile, SyntaxType.False, position++, string = current.toString())
            '÷' -> Token(textFile, SyntaxType.Div, position++, string = current.toString())
            '∅' -> Token(textFile, SyntaxType.Null, position++, string = current.toString())

            '@' -> Token(textFile, SyntaxType.At, position++, string = current.toString())

            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' -> readNumberToken()

            '+' -> when (lookAhead()) {
                '=' -> Token(textFile, SyntaxType.PlusEquals, position, string = "+=").also { position += 2 }
                else -> Token(textFile, SyntaxType.Plus, position++, string = "+")
            }
            '-' -> when (lookAhead()) {
                '=' -> Token(textFile, SyntaxType.MinusEquals, position, string = "-=").also { position += 2 }
                '>' -> Token(textFile, SyntaxType.LambdaArrow, position, string = "->").also { position += 2 }
                else -> Token(textFile, SyntaxType.Minus, position++, string = "-")
            }
            '*' -> when (lookAhead()) {
                '=' -> Token(textFile, SyntaxType.TimesEquals, position, string = "*=").also { position += 2 }
                else -> Token(textFile, SyntaxType.Star, position++, string = "*")
            }
            '/' -> when (lookAhead()) {
                '=' -> Token(textFile, SyntaxType.DivEquals, position, string = "/=").also { position += 2 }
                '/' -> readSingleLineComment()
                '*' -> readMultilineComment()
                else -> Token(textFile, SyntaxType.Div, position++, string = "/")
            }
            '%' -> when (lookAhead()) {
                '=' -> Token(textFile, SyntaxType.RemEquals, position, string = "%=").also { position += 2 }
                else -> Token(textFile, SyntaxType.Rem, position++, string = "%")
            }
            '&' -> when (lookAhead()) {
                '=' -> Token(textFile, SyntaxType.AndEquals, position, string = "&=").also { position += 2 }
                '&' -> Token(textFile, SyntaxType.LogicAnd, position, string = "&&").also { position += 2 }
                else -> Token(textFile, SyntaxType.BitAnd, position++, string = "&")
            }
            '|' -> when (lookAhead()) {
                '=' -> Token(textFile, SyntaxType.OrEquals, position, string = "|=").also { position += 2 }
                '|' -> Token(textFile, SyntaxType.LogicOr, position, string = "||").also { position += 2 }
                else -> Token(textFile, SyntaxType.BitOr, position++, string = "|")
            }
            '(' -> Token(textFile, SyntaxType.OpenParentheses, position++, string = "(")
            ')' -> Token(textFile, SyntaxType.ClosedParentheses, position++, string = ")")
            '{' -> Token(textFile, SyntaxType.OpenBrace, position++, string = "{")
            '}' -> Token(textFile, SyntaxType.ClosedBrace, position++, string = "}")
            '[' -> Token(textFile, SyntaxType.OpenBracket, position++, string = "[")
            ']' -> Token(textFile, SyntaxType.ClosedBracket, position++, string = "]")
            '!' -> when (lookAhead()) {
                '=' -> when {
                    peek(2) == '=' -> Token(textFile, SyntaxType.IsNotIdentityEqual, position, string = "!==").also { position += 3 }
                    else -> Token(textFile, SyntaxType.IsNotEqual, position, string = "!=").also { position += 2 }
                }
                '!' -> Token(textFile, SyntaxType.DoubleBang, position, string = "!!").also { position += 2 }
                else -> Token(textFile, SyntaxType.Bang, position++, string = "!")
            }
            '?' -> Token(textFile, SyntaxType.QuestionMark, position++, string = "?")
            '=' -> when (lookAhead()) {
                '=' -> when {
                    peek(2) == '=' -> Token(textFile, SyntaxType.IsIdentityEqual, position, string = "===").also { position += 3 }
                    else -> Token(textFile, SyntaxType.IsEqual, position, string = "==").also { position += 2 }
                }
                else -> Token(textFile, SyntaxType.Equals, position++, string = "=")
            }
            '>' -> when (lookAhead()) {
                '=' -> Token(textFile, SyntaxType.IsEqualOrMore, position, string = ">=").also { position += 2 }
                else -> Token(textFile, SyntaxType.MoreThan, position++, string = ">")
            }
            '<' -> when (lookAhead()) {
                '=' -> Token(textFile, SyntaxType.IsEqualOrLess, position, string = ">=").also { position += 2 }
                else -> Token(textFile, SyntaxType.LessThan, position++, string = ">")
            }
            ',' -> Token(textFile, SyntaxType.Comma, position++, string = ",")
            ':' -> Token(textFile, SyntaxType.Colon, position++, string = ":")
            '\'' -> readChar()
            '"' -> readString()
            '\n', '\r', ';', ';' -> Token(textFile, SyntaxType.LineSeparator, position++)
            '\u0000' -> Token(textFile, SyntaxType.EOF, sourceText.lastIndex)
            else -> {
                diagnostics.reportBadCharacter(TextLocation(sourceText, TextSpan(position, 1)), current)
                Token(textFile, SyntaxType.Bad, position++)
            }
        }
    }

    private fun readNumberToken(): Token {
        val start = position
        val stringBuilder = StringBuilder()

        var isFloat = false
        if (current == '.') {
            if (lookAhead() == '.') {
                return Token(textFile, SyntaxType.Range, position, string = "..").also { position += 2 }
            } else if (!lookAhead().isDigit()) {
                return Token(textFile, SyntaxType.Dot, position++, string = ".")
            } else {
                isFloat = true
                stringBuilder.append('.')
                position++
            }
        }
        var isDouble = true
        var isLong = false
        if (current == '0') {
            if (lookAhead() == 'x' || lookAhead() == 'b' || lookAhead() == 's') {
                stringBuilder.append('0')
                position++
                stringBuilder.append(current)
                position++
            }
        }
        loop@ while (current.isDigit()) {
            stringBuilder.append(current)
            position++
            while (current == '_') {
                position++
            }
            if (!current.isDigit()) {
                when (current) {
                    '.' -> if (lookAhead() == '.') break@loop else if (isFloat) {
                        diagnostics.reportBadCharacter(TextLocation(sourceText, TextSpan(position, 1)), current)
                    } else {
                        isFloat = true
                        stringBuilder.append('.')
                        position++
                    }
                    'f' -> {
                        isDouble = false
                        isFloat = true
                        position++
                        break@loop
                    }
                    'l' -> if (isFloat) {
                        diagnostics.reportBadCharacter(TextLocation(sourceText, TextSpan(position, 1)), current)
                    } else {
                        isLong = true
                        position++
                        break@loop
                    }
                    else -> if (current.isLetter()) {
                        diagnostics.reportBadCharacter(TextLocation(sourceText, TextSpan(position, 1)), current)
                    }
                }
            }
        }
        val textRaw = stringBuilder.toString()
        if (isFloat) {
            if (isDouble) {
                val d = textRaw.toDoubleOrNull()
                if (d != null) {
                    return Token(textFile, SyntaxType.Double, start, d, textRaw)
                }
            } else {
                val f = textRaw.toFloatOrNull()
                if (f != null) {
                    return Token(textFile, SyntaxType.Float, start, f, textRaw)
                }
            }
        } else {
            val (radix, text) = when {
                textRaw.startsWith("0x") -> {
                    16 to textRaw.substring(2)
                }
                textRaw.startsWith("0b") -> {
                    2 to textRaw.substring(2)
                }
                textRaw.startsWith("0s") -> {
                    6 to textRaw.substring(2)
                }
                else -> 10 to textRaw
            }
            if (!isLong) {
                val i8 = text.toByteOrNull(radix)
                if (i8 != null) {
                    return Token(textFile, SyntaxType.I8, start, i8, textRaw)
                }
                val i16 = text.toShortOrNull(radix)
                if (i16 != null) {
                    return Token(textFile, SyntaxType.I16, start, i16, textRaw)
                }
                val i32 = text.toIntOrNull(radix)
                if (i32 != null) {
                    return Token(textFile, SyntaxType.I32, start, i32, textRaw)
                }
            }
            val i64 = text.toLongOrNull(radix)
            if (i64 != null) {
                return Token(textFile, SyntaxType.I64, start, i64, textRaw)
            }
        }
        diagnostics.notANumber(TextLocation(sourceText, TextSpan(start, position - start)), textRaw)
        return Token(textFile, SyntaxType.I32, start, 1, textRaw)
    }

    fun readIdentifierOrKeyword(): Token {
        val start = position++
        while (current.isLetterOrDigit() || current == '_') {
            position++
        }
        val text = sourceText.getText(start, position - start)
        val type = Translator.stringToTokenKind(text)
        return Token(textFile, type, start, string = text)
    }

    fun readCharExcape(): Char? {
        return when (lookAhead()) {
            'n' -> '\n'
            't' -> '\t'
            'r' -> '\r'
            '\\' -> '\\'
            '0' -> '\u0000'
            else -> null
        }
    }

    fun readChar(): Token {
        val start = position++
        val char = when (current) {
            '\\' -> {
                val escape = readCharExcape()
                if (escape == null) {
                    diagnostics.reportInvalidCharacterEscape(
                        TextLocation(sourceText, TextSpan(position, 1)),
                        current.toString())
                    position++
                    ' '
                } else {
                    position++
                    escape
                }
            }
            '\u0000' -> {
                diagnostics.reportBadCharacter(TextLocation(sourceText, TextSpan(start, 1)), '\'')
                ' '
            }
            else -> current
        }
        position++
        if (current != '\'') {
            diagnostics.reportBadCharacter(TextLocation(sourceText, TextSpan(position, 1)), current)
        }
        position++
        return Token(textFile, SyntaxType.Char, start, char, "'$char'")
    }

    fun readString(): Token {
        val start = ++position
        val builder = StringBuilder()
        loop@ while (current != '"') {
            when (current) {
                '\\' -> when (lookAhead()) {
                    '"' -> {
                        builder.append('"')
                        position += 2
                    }
                    else -> {
                        val escape = readCharExcape()
                        if (escape == null) {
                            diagnostics.reportInvalidCharacterEscape(
                                    TextLocation(sourceText, TextSpan(position, 1)),
                                    current.toString())
                            position++
                        } else {
                            builder.append(escape)
                            position += 2
                        }
                    }
                }
                '\u0000' -> {
                    diagnostics.reportUnterminatedString(TextLocation(sourceText, TextSpan(start, builder.length)))
                    break@loop
                }
                else -> {
                    builder.append(current)
                    position++
                }
            }
        }
        val text = builder.toString()
        position++
        return Token(textFile, SyntaxType.String, start - 1, text, '"' + text + '"')
    }

    private fun readSingleLineComment(): Token {
        val start = position
        position += 2
        var reading = true
        val builder = StringBuilder()
        while (reading) when (current) {
            '\u0000', '\n', '\r' -> reading = false
            else -> {
                builder.append(current)
                position++
            }
        }
        val text = builder.toString()
        return Token(textFile, SyntaxType.SingleLineComment, start, text, "//$text")
    }

    private fun readMultilineComment(): Token {
        val start = position
        position += 2
        var nestedCommentDepth = 0
        val builder = StringBuilder()
        loop@ while (true) when {
            current == '*' && lookAhead() == '/' -> if (nestedCommentDepth == 0) {
                position += 2
                break@loop
            } else {
                nestedCommentDepth--
                builder.append(current)
                position++
            }
            current == '/' && lookAhead() == '*' -> {
                nestedCommentDepth++
                builder.append(current)
                position++
            }
            current == '\u0000' -> {
                diagnostics.reportUnterminatedMultilineComment(TextLocation(sourceText, TextSpan.fromBounds(start, position)))
                break@loop
            }
            else -> {
                builder.append(current)
                position++
            }
        }
        val text = builder.toString()
        return Token(textFile, SyntaxType.MultilineComment, start, text, "/*$text*/")
    }
}