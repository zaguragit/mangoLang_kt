use std*

val badName = "semicolon"

@entry
val main () -> {
    io.println("Hello world!")

    // Number literals test
/*
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

    use std.primitives*

    loop i : 0..4 {
        io.println(i.toString())
    }

    var i = 0
    loop {
        io.println(i.toString())
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