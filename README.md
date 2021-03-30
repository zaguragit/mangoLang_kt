
<div align="center">
<p><img width=100% src="/art/banner.svg"/></p>
<h1>MangoLang</h1>

[Documentation](DOCUMENTATION.md)

![Follow on twitter](https://img.shields.io/twitter/follow/mangoLang?color=219DE9&label=Follow&logo=twitter&logoColor=219DE9&style=flat)
</div>

Mango is an experimental language, that compiles to LLVM IR

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
