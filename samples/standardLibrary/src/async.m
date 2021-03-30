use sys

type Process {
    val pid I32
}

[internal]
[cname: "fork"]
val fork () I32

[inline]
val getCurrentProcess () Process -> {
    [internal]
    [cname: "getpid"]
    val getpid () I32
    Process { pid: getpid() }
}

val async (fn Void()) Process -> {
    val child_pid = fork()
    child_pid == 0 ? {
        fn()
        sys.exit()
    }
    Process { pid: child_pid }
}