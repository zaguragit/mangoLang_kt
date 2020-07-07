
/*
 * returns a random unsigned 64 bit integer (except 0)
 */
fn randomU64 (seed U64) U64 {
    var n = seed
    n = n xor (n << 21)
    n = n xor (n >>> 35)
    n = n xor (n << 4)
    return n
}

[inline]
fn smaller (a Int, b Int) Int -> if a < b { return a } else { return b }

[inline]
fn bigger  (a Int, b Int) Int -> if a > b { return a } else { return b }

/*
[inline]
fn smaller (a Int, b Int) Int -> (a < b) ? a : b

[inline]
fn bigger  (a Int, b Int) Int -> (a > b) ? a : b
*/
