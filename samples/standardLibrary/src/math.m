/*
/*
 * returns a random unsigned 64 bit integer (except 0)
 */
val randomU64 (seed U64) U64 -> {
    var n = seed
    n = n.xor(n << 21)
    n = n.xor(n >>> 35)
    n = n.xor(n << 4)
    n
}

[inline]
val smaller (a Int, b Int) Int -> a < b ? a : b

[inline]
val bigger (a Int, b Int) Int -> a > b ? a : b

*/