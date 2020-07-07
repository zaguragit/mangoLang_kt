
case $1 in
    'build')
        clang std/c/main.c -o out/std.so -shared
    ;;
    'install')
        sudo rm -r /usr/local/lib/mangoLang/
        sudo mkdir -p /usr/local/lib/mangoLang/
        sudo cp out/* /usr/local/lib/mangoLang/
        sudo ldconfig -n -v /usr/local/lib/mangoLang
    ;;
    *)
        echo $1
    ;;
esac