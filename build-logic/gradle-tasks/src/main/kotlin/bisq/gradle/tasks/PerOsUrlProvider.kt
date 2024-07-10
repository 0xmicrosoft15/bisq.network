package bisq.gradle.tasks

interface PerOsUrlProvider {
    val urlPrefix: String

    val linuxUrl: String
    val macOsUrl: String
    val macOsAarch64Url: String
    val windowsUrl: String

    val url: String
        get() = urlPrefix + getUrlSuffix()

    private fun getUrlSuffix() =
        when (getOS()) {
            OS.LINUX -> linuxUrl
            OS.MAC_OS -> macOsUrl
            OS.MAC_OS_AARCH64 -> macOsAarch64Url
            OS.WINDOWS -> windowsUrl
        }

}