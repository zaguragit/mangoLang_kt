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
*/

[inline]
val smaller (a I64, b I64) I64 -> a < b ? a : b
[inline]
val bigger (a I64, b I64) I64 -> a > b ? a : b

[inline]
val smaller (a I32, b I32) I32 -> a < b ? a : b
[inline]
val bigger (a I32, b I32) I32 -> a > b ? a : b

[inline]
val smaller (a I16, b I16) I16 -> a < b ? a : b
[inline]
val bigger (a I16, b I16) I16 -> a > b ? a : b

[inline]
val smaller (a I8, b I8) I8 -> a < b ? a : b
[inline]
val bigger (a I8, b I8) I8 -> a > b ? a : b