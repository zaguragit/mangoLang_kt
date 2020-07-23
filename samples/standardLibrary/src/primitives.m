
[inline]
fn Bool.toString String {
    if this { return "true" }
    else { return "false" }
}

[inline]
fn Bool.toInt Int {
    if this { return 1 }
    else { return 0 }
}

[inline]
fn Int.toBool Bool -> this != 0

[extern]
[cname: "intToString"]
fn Int.toString (radix Int) String

[inline]
fn Int.toString String -> this.toString(10)