use std*

val badName = "semicolon"

[entry]
fn test {
    use secondFile*
    fn doit String {
        val name = getName()
        use std.string*

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
    use std.string*
    io.println(a.length.toString())
    io.print("length, but in hex: ")
    io.println(a.length.toString(16))
    io.print("a string: ")
    io.println(secondFile.astring)
    thing.something()

    // Number literals test

    io.println(10)
    io.println(0x10)
    io.println(0b10)
    io.println(0s10)
    /*
    io.println(10l)
    io.println(10.1)
    io.println(10.1f)
    io.println(.10)
    io.println(.10f)
    io.println(10f)
    io.println(10.)*/
}


namespace thing {
    val someRandomString = "this is a random string"

    fn something {
        io.println(someRandomString)
    }
}