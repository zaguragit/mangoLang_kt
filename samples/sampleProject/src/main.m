
val creatorsName = "leo"

[entry] fn test {

    use secondFile*

    {
        fn doit String {
            val name = getName()

            if name == creatorsName {
                println("Hi, \" + name + \"!!")
            }
            else {
                println("Hi, \" + name + \"!")
            }

            return name
        }
        println(doit())
        {
            println(doit())
            println(doit())
        }
        println(doit())
    }
}