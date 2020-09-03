package mango.compilation

enum class Target {
    UNKNOWN,
    LINUX_X64,
    LINUX_ARM64,
    MACOS_X64,
    MINGW_X64,
    BARE_X64,
    BARE_ARM64;

    companion object {
        operator fun get(string: String) = when (string.toLowerCase()) {
            "linux-arm64" -> LINUX_ARM64
            "linux-x64" -> LINUX_X64
            "macos-x64" -> MACOS_X64
            "win-x64", "mingw-x64" -> MINGW_X64
            "bare-x64" -> BARE_X64
            "bare-arm64" -> BARE_ARM64
            else -> UNKNOWN
        }
    }
}