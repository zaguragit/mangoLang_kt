use std*

val badName = "semicolon"

/*[extern]
[cname: "malloc"]
fn malloc (bytes I32) Ptr<I16>*/

[entry]
fn main -> {
    io.println("Hello world!")
/*
    use secondFile*
    fn doit String {
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
        if i == 4 break
        i += 1
    }

    val testReading = io.readln()
    io.print("-> ")
    io.println(testReading)
    io.println()

    io.println(-1234567)
}

namespace thing {
    val someRandomString = "this is a random string"

    fn something -> io.println(someRandomString)
}