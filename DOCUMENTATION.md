
<div align="center">
    <h1>MangoLang Documentation</h1>
</div>

### Technical details
- Strings are structs of: { length I32, chars I16* }
- All types are children of the "Any" type
- Decimal numbers aren't completely supported yet

# Syntax
All the keywords in the language:
```
val var type namespace loop break continue ret as use true false null unsafe
```

### Field declaration
The ```val``` and ```var``` keywords are used to declare a new variable or function.
Use ```var``` if it's mutable, and ```val``` if it's not.
A normal variable declaration would be like ```val a = 4``` or ```var b = 4```, just like in Kotlin.

You can also specify the type of the variable like so: ```val c String = "hi"```.

If you want to declare a function, the syntax would be like ```val doSomething = (aParam Int) Void -> {}```,
but when declaring a function, you can omit the equals sign, so it would go like this: ```val doSomething (aParam Int) Void -> {}```.
Overloads are only allowed if the equals sign is omitted. More on function declarations in [the lambdas section](#Lambdas)

Functions can be declared inside other functions.

If you want to declare an extension function, add the target type in parentheses, as if it was a parameter,
before the function name, like ```val (s String) toInt () Int -> 0```. Now you can call it like ```"abcdefg".toInt()```.

### Lambdas
A lambda in MangoLang consists of the parameters (```name Type```) inside parentheses, optionally followed by the return type.
If the function you are declaring isn't external, you have to add a function body, which consists of an arrow (```->```), and the expression you want the function to return.
Code blocks (```{ println(); 1 }```) are expressions.
Here's an example of a lambda: ```(a I32, b I32) Int -> a + b```.

### Type declaration
```
'type' <name> (optional ':' <parentType>) '{' <fields> '}'
example: type ProcessID : Int
```

### if/else expressions
```
<condition> ? <then> ((optional) ':' <else>)
example: a > b ? a : b
```

### Use statement
```
'use' <dot-separated namespaces> // and an optional '*' to include the content of the namespace (the equivalent of "using namespace" in c++)
example: use std.io*
```
