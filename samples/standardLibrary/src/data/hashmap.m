/*
type MapEntry {
    val key Ptr<Char>
    val value Ptr<Char>
}

type HashMap {
    val size I32
    val count I32
    val items Ptr<MapEntry>
}

[private]
val newItem (key Ptr<Char>, value Ptr<Char>) MapEntry -> MapEntry {
    key: key
    value: value
}

val new () HashMap -> HashMap {
    size: 53
    count: 0
    items: Ptr<MapEntry> { length: 53 }
}*/