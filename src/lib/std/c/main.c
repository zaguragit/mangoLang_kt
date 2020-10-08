typedef void Unit;

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

typedef struct {
	U32 length;
	I16* chars;
} String;

#include <stdlib.h>
#include <stdio.h>

Unit flushPrint () {
	fflush(stdout);
}

String* intToString (Int num, Int radix) {
	String* string = malloc(sizeof(String));
	string->chars = malloc(50);
	string->length = 0;

    Bool isNegative = false;

    if (num == 0) {
        string->chars[string->length++] = '0';
        return string;
    }

    if (num < 0 && radix == 10) {
        isNegative = true;
        num = -num;
    }

    while (num != 0) {
        Int rem = num % radix;
        string->chars[string->length++] = (rem > 9) ? (rem - 10) + 'a' : rem + '0';
        num /= radix;
    }

    if (isNegative) {
        string->chars[string->length++] = '-';
    }

	Int i = 0;
	Int j = string->length - 1;
	while (i < (string->length / 2 + string->length % 2)) {
		char tmp = string->chars[i];
		string->chars[i] = string->chars[j];
		string->chars[j] = tmp;
		i++;
		j--;
	}

    return string;
}