
case $1 in
    'build')
        clang src/c.c -o out/linux/tmp.o -O3 -c -fPIC
        gcc out/linux/std.o out/linux/tmp.o -o out/linux/std.so -shared -fPIC -O3
        rm out/linux/tmp.o out/linux/std.o
    ;;
    'install')
        sudo rm -r /usr/local/lib/mangoLang/std
        sudo mkdir /usr/local/lib/mangoLang/

        sudo cp -r out /usr/local/lib/mangoLang/std
    ;;
    'clean')
        rm -r out
    ;;
    *)
        echo $1
    ;;
esac