use std*

val badName = "semicolon"

[entry]
val main () -> {
    io.println("Hello world!")
/*
    use secondFile*
    val doit String -> {
        val name = getName()
        use std.string*

        if name == badName {
            io.print("Go away, ")
            io.print(name)
            io.print("!")
            io.println(" Ur not welcome here!")
        }
        : {
            io.print("Hi, ")
            io.print(name)
            io.println("!")
        }

        return name
    }
    val theGoodTheAmazing_A = doit
    val a = theGoodTheAmazing_A()
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

    io.println(10)   // 10
    io.println(0x10) // 16
    io.println(0b10) // 2
    io.println(0s10) // 6
*/
    /*
    io.println(10l)
    io.println(10.1)
    io.println(10.1f)
    io.println(.10)
    io.println(.10f)
    io.println(10f)
    io.println(10.)*/

    loop i : 0..4 {
        io.println(i)
    }

    var i = 0
    loop {
        io.println(i)
        i == 4 ? break
        i += 1
    }

    io.print("\ntype something: ")
    val testReading = io.readln()
    io.print("you typed \"")
    io.print(testReading)
    io.println('"')
    io.println()

    io.println(-1234567)
}

namespace thing {
    val someRandomString = "this is a random string"

    val something () -> io.println(someRandomString)
}