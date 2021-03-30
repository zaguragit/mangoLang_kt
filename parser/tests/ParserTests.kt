import mango.parser.Parser
import mango.parser.TextFile
import org.junit.Test
import shared.text.SourceText

class ParserTests {

    @Test
    fun typeParsing() {
        testCouldBeType("Ptr")
        testCouldBeType("StringBuilder")
        testCouldBeType("mango.text.StringBuilder")
        testCouldBeType("Ptr<I16>")
        testCouldBeType("Pair<I16, I16>")
    }

    fun testCouldBeType(string: String) {
        val p = getParser(string)
        val (c, i) = p.couldBeTypeClause()
        assert(c)
        val li = p.tokens.lastIndex - 1
        assert(i == li) { "'$string' parsing ended at token $i, and should've at $li" }
    }

    fun getParser(text: String): Parser {
        return Parser(TextFile(SourceText(text, "Test"), "mango.test"))
    }
}