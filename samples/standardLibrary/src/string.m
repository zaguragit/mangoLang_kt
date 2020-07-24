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
[operator]
[cname: "String$equals"]
fn String.equals(other String) Bool
