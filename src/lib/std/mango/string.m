/*
struct String {
	val length Int
	val chars I8*
}
*/

[extern]
[cname: "String$toInt"]
fn stringToInt (text String) Int

[extern]
[cname: "Int$toString"]
fn intToString (int Int) String

/*
[extern]
[cname: "String$equals"]
fn String.equals Bool
*/