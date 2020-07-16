use std*

val creatorsName = "leo"

[entry]
fn test {
    use secondFile*
    fn doit String {
        val name = getName()

        if name == creatorsName {
            io.println("Hi, \" + name + \"!!")
        }
        else {
            io.println("Hi, \" + name + \"!")
        }

        return name
    }
    val a = doit()
    std.io.println(a)
    {
        io.println(std.string.intToString(a.length))
        io.println(string.intToString(a.length))
    }
    io.println(secondFile.astring)
}

fn test02 {
    val name = secondFile.getName()

    if name == creatorsName {
        io.println("Hi, \" + name + \"!!")
    }
    else {
        io.println("Hi, \" + name + \"!")
    }

    io.print(name)
    io.print(" ")
    io.println(string.intToString(name.length))
}