typedef int Int;

typedef char I8;
typedef short I16;
typedef int I32;
typedef long I64;
typedef unsigned char U8;
typedef unsigned short U16;
typedef unsigned int U32;
typedef unsigned long U64;

typedef char Bool;

typedef void Unit;

//#define ret return

typedef struct String {
	Int length;
	char* chars;
} String;