
struct String {
	val length Int
	val chars U8*
}

[extern]
[cName: "String$toInt"]
fn String.toInt Int

[extern]
[cName: "Int$toString"]
fn Int.toString String

[extern]
[cName: "String$equals"]
fn String.equals Bool
