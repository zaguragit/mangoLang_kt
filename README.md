
<div align="center">
    <p><img width=100% src="/art/banner.svg"/></p>
    <h1>MangoLang</h1>
</div>

### Technical details
- Strings are structs of: { length I32, chars I16* }
- All types are children of the "Any" type
- Decimal numbers aren't completely supported yet

### Syntax
- Functions can be declared inside other functions


- Field initialization:
```
('val' (immutable) | 'var' (mutable)) <name> ((optional) <type>) = <value>
example: var a = 36
```
- Lambdas:
```
'('<params separated by commas>')' (optional <type>) -> <expression>
example: (a I32, b I32) Int -> a + b
```
- Function declaration:
```
'val' (optional <extensionType>'.')<name> (optional '=') <lambda>
example: val String.get (i Int) -> unsafe { this.chars[i] }
```
- Type declaration:
```
'type' <name> (optional ':' <parentType>) '{' <fields> '}'
example: type ProcessID : Int
```
- if/else expressions:
```
<condition> ? <then> ((optional) ':' <else>)
example: a > b ? a : b
```
- Use statement: 
```
'use' <dot-separated namespaces> // and an optional '*' to include the content of the namespace (the equivalent of "using namespace" in c++)
example: use std.io*
```

### Example
```kotlin
use std.io*

val valueName = expression
var variableName = expression
val something = "some text and stuff, here are some character escapes \n\t\r\\\""

val count (num Int) -> {
    var x = num
    x == 0 ? println("Done!") : {
        count(x - 1)
        println(x)
    }
}
```
