use std*

val badName = "semicolon"

[entry]
fn test {
    use secondFile*
    fn doit String {
        val name = getName()

        if name == badName { // string comparisons don't work yet, so this is always false
            io.print("Go away, ")
            io.print(name)
            io.print("!")
            io.println(" Ur not welcome here!")
        }
        else {
            io.print("Hi, ")
            io.print(name)
            io.println("!")
        }

        return name
    }
    val a = doit()
    io.print("name: ")
    io.println(a)
    io.print("length: ")
    io.println(a.length)
    io.print("a string: ")
    io.println(secondFile.astring)
}