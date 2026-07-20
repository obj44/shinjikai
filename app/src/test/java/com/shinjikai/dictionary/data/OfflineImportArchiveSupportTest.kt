package com.shinjikai.dictionary.data

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfflineImportArchiveSupportTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `detects dict tar zip as zip and nested dict tar as tar`() {
        assertEquals(OfflineArchiveKind.ZIP, detectOfflineArchiveKind("raw-images.dict.tar.zip"))
        assertEquals(OfflineArchiveKind.TAR, detectOfflineArchiveKind("raw-images.dict.tar"))
        assertEquals(OfflineArchiveKind.TAR_XZ, detectOfflineArchiveKind("raw-images.tar.xz"))
    }

    @Test
    fun `extractOfflineArchive unpacks nested tar payload from dict tar zip flow`() {
        val workDir = temp.newFolder("archives")
        val nestedTar = File(workDir, "raw-images.dict.tar")
        writeTar(
            archive = nestedTar,
            entries = mapOf(
                "payload/raw_shinjikai_data.jsonl" to """{"Word":{"id":1,"kana":"ねこ","writings":[{"text":"猫"}],"meanings":[{"arabic":"cat"}]}}""",
                "payload/yomitan_images/cat.png" to "image"
            )
        )

        val topZip = File(workDir, "raw-images.dict.tar.zip")
        writeZip(
            archive = topZip,
            entries = mapOf(
                nestedTar.name to nestedTar.readBytes()
            )
        )

        val extractDir = temp.newFolder("extract")
        extractOfflineArchive(topZip, extractDir)

        val extractedTar = File(extractDir, nestedTar.name)
        assertTrue(extractedTar.exists())

        val nestedExtractDir = temp.newFolder("nested-extract")
        extractOfflineArchive(extractedTar, nestedExtractDir)

        val jsonl = nestedExtractDir.walkTopDown()
            .firstOrNull { it.isFile && it.name == "raw_shinjikai_data.jsonl" }
        val imagesDir = nestedExtractDir.walkTopDown()
            .firstOrNull { it.isDirectory && it.name == "yomitan_images" }

        assertNotNull(jsonl)
        assertNotNull(imagesDir)
        assertTrue(jsonl!!.readText().contains("cat"))
    }

    @Test
    fun `findOfflineImportPayload locates both text zip and images zip extracted from one selected archive`() {
        val workDir = temp.newFolder("combined-archives")

        val textZip = File(workDir, "dictionary-text.zip")
        writeZip(
            archive = textZip,
            entries = mapOf(
                "payload/raw_shinjikai_data.jsonl" to """
                    {"Word":{"id":1,"kana":"ねこ","writings":[{"text":"猫"}],"meanings":[{"arabic":"cat"}]}}
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
        )

        val imagesZip = File(workDir, "dictionary-images.zip")
        writeZip(
            archive = imagesZip,
            entries = mapOf(
                "payload/yomitan_images/cat.png" to "image".toByteArray(Charsets.UTF_8)
            )
        )

        val selectedArchive = File(workDir, "offline-bundle.zip")
        writeZip(
            archive = selectedArchive,
            entries = mapOf(
                textZip.name to textZip.readBytes(),
                imagesZip.name to imagesZip.readBytes()
            )
        )

        val extractDir = temp.newFolder("combined-extract")
        extractOfflineArchive(selectedArchive, extractDir)

        val nestedArchivesDir = File(extractDir, "__offline_import_archives__")
        extractOfflineArchive(
            File(extractDir, textZip.name),
            File(nestedArchivesDir, "dictionary_text_zip")
        )
        extractOfflineArchive(
            File(extractDir, imagesZip.name),
            File(nestedArchivesDir, "dictionary_images_zip")
        )

        val payload = findOfflineImportPayload(extractDir)

        assertNotNull(payload.jsonlFile)
        assertNotNull(payload.extractedImagesDir)
        assertTrue(payload.jsonlFile!!.readText().contains("cat"))
        assertEquals("yomitan_images", payload.extractedImagesDir!!.name)
        assertTrue(File(payload.extractedImagesDir, "cat.png").exists())
    }

    @Test
    fun `archive entries cannot escape into a sibling with the same path prefix`() {
        val root = temp.newFolder("safe-root")

        try {
            safeResolveArchiveEntry(root, "../safe-root-escape/payload.txt")
            fail("Expected sibling-prefix traversal to be rejected.")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("Invalid archive entry path"))
        }
    }

    @Test
    fun `staged image replacement preserves a complete directory`() {
        val parent = temp.newFolder("atomic-images")
        val target = File(parent, "yomitan_images").apply {
            mkdirs()
            resolve("old.png").writeText("old")
        }
        val source = temp.newFolder("new-images").apply {
            resolve("new.png").writeText("new")
        }

        val staging = stageDirectoryCopy(source, target)
        replaceStagedDirectoryAtomically(staging, target)

        assertTrue(File(target, "new.png").exists())
        assertTrue(!File(target, "old.png").exists())
        assertTrue(!File(parent, ".yomitan_images.backup").exists())
    }

    private fun writeZip(archive: File, entries: Map<String, ByteArray>) {
        ZipOutputStream(FileOutputStream(archive)).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
    }

    private fun writeTar(archive: File, entries: Map<String, String>) {
        TarArchiveOutputStream(FileOutputStream(archive)).use { tar ->
            entries.forEach { (name, content) ->
                val bytes = content.toByteArray(Charsets.UTF_8)
                val entry = TarArchiveEntry(name).apply {
                    size = bytes.size.toLong()
                }
                tar.putArchiveEntry(entry)
                tar.write(bytes)
                tar.closeArchiveEntry()
            }
            tar.finish()
        }
    }
}
