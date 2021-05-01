/*
@inline
val executeCommand (args Array<String>) I32 -> {
    @cname("execvp")
    val execvp (file Ptr<Char>, argv Ptr<Ptr<Char>>) I32
    unsafe {
        val argArray = args.map((it String) -> it.chars)
        execvp(cmd.chars, argArray.ptr)
    }
}
*/
@cname("sleep")
val sleep (seconds I32)

@cname("exit")
val exit ()