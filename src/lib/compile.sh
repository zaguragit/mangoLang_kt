
case $1 in
    'build')
        clang std/c/main.c -o out/std.so -shared -fPIC -O3
    ;;
    'install')
        sudo rm -r /usr/local/lib/mangoLang/
        sudo rm -r /usr/local/include/mangoLang/
        sudo mkdir /usr/local/lib/mangoLang/
        sudo mkdir /usr/local/include/mangoLang/

        sudo cp out/* /usr/local/lib/mangoLang/

        sudo mkdir /usr/local/include/mangoLang/std
        sudo cp std/mango/* /usr/local/include/mangoLang/std/
    ;;
    *)
        echo $1
    ;;
esac