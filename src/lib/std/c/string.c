
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

Int stringToInt (String* string, Int radix) {
	Int n = 0;
	Int p = 1;
	U32 length = string->length;
	for (Int i = length - 1; i >= 0; i--) {
		n += ((Int)(string->chars[i] - '0')) * p;
		p *= radix;
	}
	return n;
}

Bool String$equals (String* str0, String* str1) {
	U32 size = str0->length;
	if (size != str1->length) return false;
    U32 i = 0;
    while (i < size) {
        if (str0->chars[i] != str1->chars[i]) {
            return false;
        }
        i++;
    }
	return true;
}
