package mango.cli.console

object Highlighter {
    fun keyword(str: String) = Console.BLUE_BRIGHT + str + Console.RESET
    fun type(str: String) = Console.YELLOW_BRIGHT + str + Console.RESET
    fun literal(str: String) = Console.GREEN_BRIGHT + str + Console.RESET
    fun label(str: String) = Console.YELLOW + str + Console.RESET
}