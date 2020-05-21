# Mango
Right now it's just a REPL with an interpreter, it doesn't even read files.
The goal is to eventually make it a compiler and write a second compiler using the language itself.

### Syntax
The if/else are statements and require curly brackets, but don't require parentheses.
As of now the language is in such an early stage that it doesn't even support comments (i guess strings are a way around that), so the syntax is subject to change.
```kotlin
val valueName = expression
var variableName = expression
val something = "some text and stuff, here are some character escapes \n\t\r\\\""
if 3 > someNumber {

} else if aBoolean || anotherBoolean {

}

"function syntax: fn <name>: <paramName> <paramType>, <paramName> <paramType> ... ( -> <statement> | -> <returnType> <blockStatement> | -> <blockStatement> | <blockStatement>)"
fn count: num Int {
    var x = num
    if x == 0 { println("Done!") }
    else {
        count(x - 1)
        println(String(x))
    }
}
```
