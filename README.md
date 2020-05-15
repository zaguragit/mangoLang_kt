# Mango
Right now it's just a REPL with an interpreter, it doesn't even read files.
The goal is to eventually make it a compiler and write a second compiler with the language itself.

### Syntax
It's very similar to Kotlin, but the if/else are statements and require curly brackets, but don't require parentheses.
As of now the language is in such an early stage that it doesn't even support comments, so the syntax is subject to change.
```kotlin
val valueName = expression
var variableName = expression
val something = "some text and stuff, here are some character escapes \n\t\r\\\""
if 3 > someNumber {

} else if aBoolean || anotherBoolean {

}
```
