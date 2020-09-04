
typedef          char   I8;
typedef unsigned char   U8;

typedef          short  I16;
typedef unsigned short  U16;

typedef          int    Int;
typedef          int    I32;
typedef unsigned int    U32;

typedef          long   I64;
typedef unsigned long   U64;

typedef _Bool Bool;
#define true  1
#define false 0

typedef void Unit;

typedef struct {
	U32 length;
	I16* chars;
} String;