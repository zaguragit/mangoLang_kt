
<div align="center">
    <p><img width=100% src="/art/banner.svg"/></p>
    <h1>MangoLang</h1>
</div>

### Technical details
- Strings are structs of: { length I32, chars I8* }
- All types are children of the "Any" type
- "Unit" is the type for functions that don't return anything
- Decimal numbers aren't completely supported yet

### Syntax
- Field initialization: ``` (val (for immutable) | var (for mutable)) <name> ((optional) <type>) = <value> ```
- if/else expressions: ``` <condition> ? <then> ((optional) : <else>) ```
- Function declaration: ``` fn <name> '('<params separated by commas>')' ((optional) <type>) -> <expression> ```
- Use statement: ``` use <dot-separated namespaces> ```, and an optional '*' to include the content of the namespace (the equivalent of "using namespace" in c++)

### Syntactic sugar
- Functions can be declared inside other functions


```rust
use std.io*

val valueName = expression
var variableName = expression
val something = "some text and stuff, here are some character escapes \n\t\r\\\""

fn count (num Int) -> {
    var x = num
    x == 0 ? println("Done!") : {
        count(x - 1)
        println(x)
    }
}
```
