
Unit print(String* string) {
	Int i = 0;
	while (i < string->length) {
		putchar(string->chars[i++]);
	}
}

Unit println(String* string) {
	print(string);
	putchar('\n');
}

String readln() {
	String string;
    string.length = 0;
    Int ch;
    char* buff = malloc(256);
    while (((ch = getchar()) != '\n') && (ch != EOF) && (string.length != 255)) {
        buff[string.length++] = ch;
    }
    buff[string.length] = '\0';
    string.chars = realloc(buff, string.length + 1);
    return string;
}
