package bisq.gradle.packaging

import bisq.gradle.common.OS
import bisq.gradle.common.getOS
import org.apache.commons.codec.binary.Hex
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest


abstract class Sha256HashTask : DefaultTask() {

    @get:InputFiles
    abstract val inputFiles: ListProperty<File>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    private val digest = MessageDigest.getInstance("SHA-256")

    @TaskAction
    fun run() {
        val fileHashes = inputFiles.get().map { file ->
            val hash = digest.digest(file.readBytes())
            Pair(file.name, Hex.encodeHexString(hash))
        }

        // linux:file_name:sha256_hash
        val osName = getOsName()
        val lines = fileHashes.map { nameAndHash -> "$osName:${nameAndHash.first}:${nameAndHash.second}" }

        outputFile.asFile.get()
            .writeText(lines.joinToString(separator = "\n"))
    }

    private fun getOsName(): String =
        when (getOS()) {
            OS.LINUX -> "linux"
            OS.MAC_OS -> "macOS"
            OS.WINDOWS -> "windows"
        }
}
