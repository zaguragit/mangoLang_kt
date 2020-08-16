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
[cname: "stringEquals"]
fn String.equals(other String) Bool

[inline]
[operator]
fn String.get(i Int) I8 -> unsafe { this.chars[i] }