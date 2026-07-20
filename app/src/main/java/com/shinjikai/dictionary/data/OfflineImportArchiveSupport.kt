package com.shinjikai.dictionary.data

import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream

internal enum class OfflineArchiveKind {
    ZIP,
    TAR,
    TAR_XZ
}

internal fun detectOfflineArchiveKind(fileName: String): OfflineArchiveKind? {
    val normalized = fileName.trim().lowercase(Locale.ROOT)
    return when {
        normalized.endsWith(".tar.xz") || normalized.endsWith(".txz") -> OfflineArchiveKind.TAR_XZ
        normalized.endsWith(".tar") -> OfflineArchiveKind.TAR
        normalized.endsWith(".zip") -> OfflineArchiveKind.ZIP
        else -> null
    }
}

internal fun extractOfflineArchive(
    archiveFile: File,
    targetDir: File
) {
    archiveFile.inputStream().buffered().use { input ->
        when (detectOfflineArchiveKind(archiveFile.name)) {
            OfflineArchiveKind.ZIP -> extractZipStream(input, targetDir)
            OfflineArchiveKind.TAR -> extractTarStream(input, targetDir)
            OfflineArchiveKind.TAR_XZ -> extractTarXzStream(input, targetDir)
            null -> error("Unsupported archive: ${archiveFile.name}")
        }
    }
}

internal data class OfflineImportPayload(
    val jsonlFile: File? = null,
    val extractedImagesDir: File? = null,
    val yomitanDirectory: File? = null
)

internal fun findOfflineImportPayload(rootDir: File): OfflineImportPayload {
    val jsonlFile = rootDir
        .walkTopDown()
        .filter { it.isFile && it.name.equals("raw_shinjikai_data.jsonl", ignoreCase = true) }
        .minByOrNull { it.absolutePath.length }
    val extractedImagesDir = rootDir
        .walkTopDown()
        .filter { it.isDirectory && it.name.equals("yomitan_images", ignoreCase = true) }
        .minByOrNull { it.absolutePath.length }
    val yomitanDirectory = rootDir
        .walkTopDown()
        .filter { file ->
            file.isFile &&
                file.name.lowercase().startsWith("term_bank_") &&
                file.extension.equals("json", ignoreCase = true)
        }
        .map { it.parentFile }
        .firstOrNull()

    return OfflineImportPayload(
        jsonlFile = jsonlFile,
        extractedImagesDir = extractedImagesDir,
        yomitanDirectory = yomitanDirectory
    )
}

internal fun extractZipStream(
    input: InputStream,
    targetDir: File
) {
    val budget = ArchiveExtractionBudget()
    ZipInputStream(input).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            budget.beginEntry(entry.name, entry.size)
            val outFile = safeResolveArchiveEntry(targetDir, entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { output ->
                    copyArchiveEntry(zip, output, entry.name, budget)
                }
            }
            zip.closeEntry()
        }
    }
}

internal fun extractTarStream(
    input: InputStream,
    targetDir: File
) {
    val budget = ArchiveExtractionBudget()
    TarArchiveInputStream(input).use { tar ->
        while (true) {
            val entry = tar.nextEntry ?: break
            require(!entry.isSymbolicLink && !entry.isLink) {
                "Archive links are not supported: ${entry.name}"
            }
            if (!entry.isDirectory && !entry.isFile) {
                continue
            }
            budget.beginEntry(entry.name, entry.size)
            val outFile = safeResolveArchiveEntry(targetDir, entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { output ->
                    copyArchiveEntry(tar, output, entry.name, budget)
                }
            }
        }
    }
}

internal fun extractTarXzStream(
    input: InputStream,
    targetDir: File
) {
    XZCompressorInputStream(input).use { xz ->
        extractTarStream(xz, targetDir)
    }
}

internal fun safeResolveArchiveEntry(rootDir: File, entryName: String): File {
    require(entryName.isNotBlank()) { "Archive entry name is empty." }
    val candidate = File(rootDir, entryName)
    val rootPath = rootDir.canonicalPath
    val candidatePath = candidate.canonicalPath
    val isInsideRoot = candidatePath == rootPath ||
        candidatePath.startsWith(rootPath + File.separator)
    require(isInsideRoot) { "Invalid archive entry path: $entryName" }
    return candidate
}

private class ArchiveExtractionBudget {
    private var entryCount = 0
    private var totalBytes = 0L
    private var currentEntryBytes = 0L

    fun beginEntry(name: String, declaredSize: Long) {
        entryCount += 1
        require(entryCount <= MAX_ARCHIVE_ENTRIES) {
            "Archive contains too many entries."
        }
        currentEntryBytes = 0L
        if (declaredSize >= 0L) {
            require(declaredSize <= MAX_ARCHIVE_ENTRY_BYTES) {
                "Archive entry is too large: $name"
            }
            require(totalBytes + declaredSize <= MAX_ARCHIVE_TOTAL_BYTES) {
                "Archive expands beyond the allowed size."
            }
        }
    }

    fun recordBytes(name: String, count: Int) {
        currentEntryBytes += count
        totalBytes += count
        require(currentEntryBytes <= MAX_ARCHIVE_ENTRY_BYTES) {
            "Archive entry is too large: $name"
        }
        require(totalBytes <= MAX_ARCHIVE_TOTAL_BYTES) {
            "Archive expands beyond the allowed size."
        }
    }
}

private fun copyArchiveEntry(
    input: InputStream,
    output: java.io.OutputStream,
    entryName: String,
    budget: ArchiveExtractionBudget
) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        if (read == 0) continue
        budget.recordBytes(entryName, read)
        output.write(buffer, 0, read)
    }
}

private const val MAX_ARCHIVE_ENTRIES = 25_000
private const val MAX_ARCHIVE_ENTRY_BYTES = 512L * 1024L * 1024L
private const val MAX_ARCHIVE_TOTAL_BYTES = 2L * 1024L * 1024L * 1024L

internal fun stageDirectoryCopy(
    sourceDir: File,
    targetDir: File
): File {
    require(sourceDir.isDirectory) { "Image source directory does not exist." }
    require(sourceDir.walkTopDown().any(File::isFile)) { "Image source directory is empty." }

    val targetParent = targetDir.parentFile ?: error("Image target has no parent directory.")
    targetParent.mkdirs()
    val stagingDir = File(targetParent, ".${targetDir.name}.installing")
    if (stagingDir.exists()) {
        stagingDir.deleteRecursively()
    }
    require(sourceDir.copyRecursively(stagingDir, overwrite = true)) {
        "Unable to stage image directory."
    }
    require(stagingDir.walkTopDown().any(File::isFile)) {
        "Staged image directory is empty."
    }
    return stagingDir
}

internal fun replaceStagedDirectoryAtomically(
    stagingDir: File,
    targetDir: File
) {
    require(stagingDir.isDirectory) { "Staged image directory does not exist." }
    require(stagingDir.walkTopDown().any(File::isFile)) { "Staged image directory is empty." }

    val targetParent = targetDir.parentFile ?: error("Image target has no parent directory.")
    targetParent.mkdirs()
    val backupDir = File(targetParent, ".${targetDir.name}.backup")

    if (!targetDir.exists() && backupDir.exists()) {
        require(backupDir.renameTo(targetDir)) {
            "Unable to restore the previous image directory."
        }
    }
    if (backupDir.exists()) {
        backupDir.deleteRecursively()
    }

    val movedExistingTarget = if (targetDir.exists()) {
        require(targetDir.renameTo(backupDir)) {
            "Unable to preserve the previous image directory."
        }
        true
    } else {
        false
    }

    try {
        require(stagingDir.renameTo(targetDir)) {
            "Unable to activate the staged image directory."
        }
        if (backupDir.exists()) {
            backupDir.deleteRecursively()
        }
    } catch (throwable: Throwable) {
        if (!targetDir.exists() && movedExistingTarget && backupDir.exists()) {
            backupDir.renameTo(targetDir)
        }
        throw throwable
    }
}
