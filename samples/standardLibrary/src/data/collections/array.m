/*
[final]
[generic: T]
type Array {
    val length I32
    [private]
    val items Ptr<T>
}

[generic: T]
val (a Array<T>) get (i I32) T -> unsafe {
    a.items[i]
}

[generic: T]
val (a Array<T>) set (i I32, item T) -> unsafe {
    a.items[i] = item
}

[inline]
[generic: T, R]
val (a Array<T>) map (fn R(T)) Array<R> -> mapTo(Array { length: a.length }, fn)

[inline]
[generic: T, R]
val (a Array<T>) mapTo (dest Array<R>, fn R(T)) Array<R> -> {
    loop i : 0..a.length - 1 {
        dest[i] = fn(a[i])
    }
    dest
}

*/