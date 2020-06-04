
<div align="center">
    <h1>Mango Language</h1>
</div>

Right now it doesn't always compile to LLVM, but it will get better support soon.

### Syntax
- Fields are initialized with either "val" (for immutable) or "var" (for mutable). Just like in kotlin!
- The if/else are statements and require curly brackets, but don't require parentheses
- Function declaration syntax is: "fn", the function name, its parameters separated by commas inside parentheses, followed by the return type (if the function returns anything), and either a "->" an expression, or a block statement
- All types are children of the "Any" type
- "Unit" is the type for functions that don't return anything
- Decimal numbers aren't supported yet

As of now the language is in such an early stage that it doesn't even support comments, so the syntax is subject to change.
```kotlin
val valueName = expression
var variableName = expression
val something = "some text and stuff, here are some character escapes \n\t\r\\\""
if 3 > someNumber {

} else if aBoolean || anotherBoolean {

}

fn count (num Int) {
    var x = num
    if x == 0 { println("Done!") }
    else {
        count(x - 1)
        println(String(x))
    }
}
```
