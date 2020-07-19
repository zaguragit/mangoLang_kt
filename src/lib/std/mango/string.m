/*
struct String {
	val length Int
	val chars I8*
}
*/

[extern]
[cname: "stringToInt"]
fn String.toInt (radix Int) Int

[inline]
fn String.toInt Int -> this.toInt(10)

[extern]
[cname: "intToString"]
fn Int.toString (radix Int) String

[inline]
fn Int.toString String -> this.toString(10)

[inline]
fn Bool.toString String {
    if this { return "true" }
    else { return "false" }
}

/*
[extern]
[cname: "Int.toStringRadix"]
fn Int.toString (radix Int) String

[extern]
[cname: "String$equals"]
fn String.equals Bool
*/