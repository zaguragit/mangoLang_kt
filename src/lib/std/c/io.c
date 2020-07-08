
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

String* readln() {
	String* string = malloc(sizeof(String));
    string->length = 0;
    Int ch;
    string->chars = malloc(512);
    while (((ch = getchar()) != '\n') && (ch != EOF) && (string->length < 512)) {
        string->chars[string->length++] = ch;
    }
    return string;
}
