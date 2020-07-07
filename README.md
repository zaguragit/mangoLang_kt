
<div align="center">
    <p><img width=128px src="/art/icon/128.png"/></p>
    <h1>MangoLang</h1>
</div>

### Technical details
- Strings are structs of: { length I32, chars I8* }
- All types are children of the "Any" type
- "Unit" is the type for functions that don't return anything
- All integer types are children of AnyI
- All unsigned integer types are children of AnyU (although they don't work yet)
- Decimal numbers aren't supported yet

### Syntax
- Field initialization: ``` "val" (for immutable) | "var" (for mutable) <name> (optional <type>) = <value> ```
- if/else statements: ``` "if" <condition> { } (optional "else" {} | "else if" {}) ```
- Function declaration: ``` "fn" <name> '('<params separated by commas>')' (optional <type>), ("->" <expression> | <block statement>) ```
- Use statement: ``` "use" <dot-separated namespaces> ```, and an optional '*' to include the content of the namespace (the equivalent of "using namespace" in c++)

### Syntactic sugar
- Functions can be declared inside other functions


```rust
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