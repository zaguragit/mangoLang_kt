package mango.compilation

object Target {
    const val UNKNOWN = -1
    const val LINUX_X64 = 0
    const val LINUX_ARM64 = 1
    const val MACOS_X64 = 2
    const val MINGW_X64 = 3
    const val BARE_X64 = 4
    const val BARE_ARM64 = 5

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