String Int$toString(Int n) {
	String string;
	string.chars = malloc(50);
	string.length = 0;
	
	Bool sign = n < 0;
	if (sign) n = -n;
	
	do string.chars[string.length++] = n % 10 + '0';
	while ((n /= 10) > 0);

	if (sign) string.chars[string.length++] = '-';
	string.chars[string.length] = '\0';
	Int i = 0;
	Int j = string.length - 1;
	while (i < (string.length / 2 + string.length % 2)) {
		char tmp = string.chars[i];
		string.chars[i] = string.chars[j];
		string.chars[j] = tmp;
		i++;
		j--;
	}
	return string;
}

Int String$toInt(String* string) {
	Int n = 0;
	Int p = 1;
	U32 length = string->length;
	for (Int i = length - 1; i >= 0; i--) {
		n += ((Int)(string->chars[i] - '0')) * p;
		p *= 10;
	}
	return n;
}

Bool String$equals(String* str0, String* str1) {
	U32 size = str0->length;
	if (size != str1->length) return 0;
	else {
		U32 i = 0;
		while (i <= size) {
			if (str0->chars[i] != str1->chars[i]) {
				return 0;
			}
			i++;
		}
	}
	return 1;
}
