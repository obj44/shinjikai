package com.shinjikai.dictionary.data

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OfflineImageSupportTest {
    @Test
    fun `canonical picture normalization unwraps nested bookmark image objects`() {
        val details = WordDetailsResponse(
            word = WordDetailsWord(
                id = 1,
                meanings = listOf(
                    Meaning(
                        arabic = "test",
                        pictures = listOf(
                            JsonParser.parseString("""{"Image":{"Filename":"cat.png"}}"""),
                            JsonParser.parseString("""{"media":[{"url":"gallery/dog.webp"}]}""")
                        )
                    )
                )
            )
        )

        val pictures = details.withCanonicalPictureElements()
            .word
            .meanings
            .single()
            .pictures
            .map { it.asString }

        assertEquals(listOf("cat.png", "gallery/dog.webp"), pictures)
    }

    @Test
    fun `resolved offline images keep absolute file paths for bookmarked details`() {
        val imageRoot = File("build/test-offline/yomitan_images").absolutePath.replace('\\', '/')
        val details = WordDetailsResponse(
            word = WordDetailsWord(
                id = 2,
                meanings = listOf(
                    Meaning(
                        arabic = "test",
                        pictures = listOf(
                            JsonParser.parseString("""{"Picture":{"Path":"animals/cat.jpg"}}"""),
                            JsonParser.parseString(""""already.png"""")
                        )
                    )
                )
            )
        )

        val pictures = details.withResolvedOfflineImages(imageRoot)
            .word
            .meanings
            .single()
            .pictures
            .map { it.asString }

        assertEquals("$imageRoot/animals/cat.jpg", pictures[0])
        assertEquals("$imageRoot/already.png", pictures[1])
    }

    @Test
    fun `picture reference extractor scans nested arrays and fallback keys`() {
        val picture = JsonParser.parseString(
            """{"payload":{"items":[{"href":"https://example.com/image.jpg"}]}}"""
        )

        val extracted = extractPictureReference(picture)

        assertEquals("https://example.com/image.jpg", extracted)
        assertTrue(extracted!!.startsWith("https://"))
    }

    @Test
    fun `official image urls resolve to bundled image files`() {
        val imageRoot = File("build/test-offline/yomitan_images").absolutePath.replace('\\', '/')

        val resolved = resolveOfflineImagePath(
            "https://shinjikai.app/static/word_pictures/gallery/cat.jpg",
            imageRoot
        )

        assertEquals("$imageRoot/gallery/cat.jpg", resolved)
    }

    @Test
    fun `windows absolute image paths are preserved`() {
        val resolved = resolveOfflineImagePath(
            "C:\\images\\word.png",
            File("build/test-offline/yomitan_images")
        )

        assertEquals("C:/images/word.png", resolved)
    }

    @Test
    fun `picture captions survive canonicalization and path resolution`() {
        val imageRoot = File("build/test-offline/yomitan_images").absolutePath.replace('\\', '/')
        val picture = JsonParser.parseString(
            """{"Filename":"4252.jpg","Description":"شجرة كرز"}"""
        )

        val normalized = normalizeStoredPictureElement(picture)!!
        val resolved = resolveStoredPictureElement(normalized, imageRoot)!!

        assertEquals("شجرة كرز", extractPictureDescription(resolved))
        assertEquals("$imageRoot/4252.jpg", extractPictureReference(resolved))
    }
}
