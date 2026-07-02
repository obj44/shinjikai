package com.shinjikai.dictionary.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.io.File
import java.net.URI

const val OFFLINE_IMAGE_DIR_META_KEY = "offline_image_dir"

private val WINDOWS_ABSOLUTE_IMAGE_PATH_REGEX = Regex("""^[A-Za-z]:/.*""")

private val PICTURE_REFERENCE_KEYS = listOf(
    "Filename",
    "FileName",
    "filename",
    "fileName",
    "File",
    "file",
    "Url",
    "URL",
    "url",
    "Href",
    "href",
    "Link",
    "link",
    "Src",
    "src",
    "Source",
    "source",
    "Path",
    "path",
    "RelativePath",
    "relativePath",
    "LocalPath",
    "localPath",
    "Image",
    "image",
    "ImageUrl",
    "ImageURL",
    "imageUrl",
    "Picture",
    "picture",
    "Media",
    "media"
)

internal fun WordDetailsResponse.withCanonicalPictureElements(): WordDetailsResponse {
    return copy(
        word = word.copy(
            pictures = word.pictures.orEmpty().mapNotNull(::normalizeStoredPictureElement),
            meanings = word.meanings.map { meaning ->
                meaning.copy(
                    pictures = meaning.pictures.mapNotNull(::normalizeStoredPictureElement)
                )
            }
        )
    )
}

internal fun WordDetailsResponse.withResolvedOfflineImages(imageDirectory: File?): WordDetailsResponse {
    return withResolvedOfflineImages(imageDirectory?.absolutePath?.replace('\\', '/'))
}

internal fun WordDetailsResponse.withResolvedOfflineImages(imageRoot: String?): WordDetailsResponse {
    val canonical = withCanonicalPictureElements()
    if (imageRoot.isNullOrBlank()) return canonical
    return canonical.copy(
        word = canonical.word.copy(
            pictures = canonical.word.pictures.orEmpty().mapNotNull { picture ->
                resolveStoredPictureElement(picture, imageRoot)
            },
            meanings = canonical.word.meanings.map { meaning ->
                meaning.copy(
                    pictures = meaning.pictures.mapNotNull { picture ->
                        resolveStoredPictureElement(picture, imageRoot)
                    }
                )
            }
        )
    )
}

internal fun normalizeStoredPictureElement(picture: JsonElement): JsonElement? {
    if (picture.isJsonNull) return null
    val reference = extractPictureReference(picture)?.takeIf { it.isNotBlank() }
        ?: return picture.takeUnless { it.isJsonNull }
    return canonicalPictureElement(
        reference = reference.replace('\\', '/'),
        description = extractPictureDescription(picture)
    )
}

internal fun resolveStoredPictureElement(
    picture: JsonElement,
    imageDirectory: File?
): JsonElement? {
    return resolveStoredPictureElement(
        picture = picture,
        imageRoot = imageDirectory?.absolutePath?.replace('\\', '/')
    )
}

internal fun resolveStoredPictureElement(
    picture: JsonElement,
    imageRoot: String?
): JsonElement? {
    if (picture.isJsonNull) return null
    val value = extractPictureReference(picture)?.trim().orEmpty()
    if (value.isBlank()) return picture.takeUnless { it.isJsonNull }
    return canonicalPictureElement(
        reference = resolveOfflineImagePath(value, imageRoot),
        description = extractPictureDescription(picture)
    )
}

private fun canonicalPictureElement(reference: String, description: String?): JsonElement {
    if (description.isNullOrBlank()) return JsonPrimitive(reference)
    return JsonObject().apply {
        addProperty("Filename", reference)
        addProperty("Description", description.trim())
    }
}

internal fun extractPictureDescription(picture: JsonElement): String? {
    if (!picture.isJsonObject) return null
    val obj = picture.asJsonObject
    return listOf("Description", "description", "Caption", "caption", "Alt", "alt")
        .firstNotNullOfOrNull { key ->
            obj.get(key)
                ?.takeIf(JsonElement::isJsonPrimitive)
                ?.asJsonPrimitive
                ?.takeIf { it.isString }
                ?.asString
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }
}

internal fun extractPictureReference(picture: JsonElement): String? {
    return when {
        picture.isJsonNull -> null
        picture.isJsonPrimitive -> picture.asJsonPrimitive
            .takeIf { it.isString }
            ?.asString
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        picture.isJsonArray -> extractPictureReferenceFromArray(picture.asJsonArray)
        picture.isJsonObject -> extractPictureReferenceFromObject(picture.asJsonObject)
        else -> null
    }
}

private fun extractPictureReferenceFromArray(array: JsonArray): String? {
    array.forEach { element ->
        extractPictureReference(element)?.let { return it }
    }
    return null
}

private fun extractPictureReferenceFromObject(obj: JsonObject): String? {
    PICTURE_REFERENCE_KEYS.forEach { key ->
        obj.get(key)?.let { candidate ->
            extractPictureReference(candidate)?.let { return it }
        }
    }

    obj.entrySet().forEach { (_, value) ->
        extractPictureReference(value)?.let { return it }
    }

    return null
}

internal fun resolveOfflineImagePath(reference: String, imageDirectory: File?): String {
    return resolveOfflineImagePath(
        reference = reference,
        imageRoot = imageDirectory?.absolutePath?.replace('\\', '/')
    )
}

internal fun resolveOfflineImagePath(reference: String, imageRoot: String?): String {
    val normalized = reference.replace('\\', '/').trim()
    if (normalized.isBlank()) return normalized
    val root = imageRoot?.trimEnd('/')?.takeIf { it.isNotBlank() }
    val bundledRelativePath = extractBundledImageRelativePath(normalized)
    if (root != null && bundledRelativePath != null) {
        return resolveImageRoot(root, bundledRelativePath)
    }
    if (WINDOWS_ABSOLUTE_IMAGE_PATH_REGEX.matches(normalized)) return normalized
    if (normalized.startsWith("file:/", ignoreCase = true)) return normalized
    if (normalized.startsWith("https://", ignoreCase = true)) return normalized
    if (normalized.startsWith("http://", ignoreCase = true)) return normalized
    if (normalized.startsWith("//")) return normalized
    if (normalized.startsWith("/")) return normalized
    if (root == null) return normalized
    val relative = normalized
        .removePrefix("./")
        .let { path ->
            if (root.endsWith("/yomitan_images", ignoreCase = true) && path.startsWith("yomitan_images/")) {
                path.removePrefix("yomitan_images/")
            } else {
                path
            }
        }
    return resolveImageRoot(root, relative)
}

private fun extractBundledImageRelativePath(reference: String): String? {
    val normalized = reference.replace('\\', '/')
    val marker = "/static/word_pictures/"
    if (normalized.startsWith(marker)) {
        return normalized.removePrefix(marker).takeIf { it.isNotBlank() }
    }
    if (!normalized.startsWith("http://", true) &&
        !normalized.startsWith("https://", true) &&
        !normalized.startsWith("//")
    ) {
        return null
    }
    val uriText = if (normalized.startsWith("//")) "https:$normalized" else normalized
    return runCatching { URI(uriText) }
        .getOrNull()
        ?.takeIf { uri -> uri.host?.endsWith("shinjikai.app", ignoreCase = true) == true }
        ?.path
        ?.substringAfter(marker, missingDelimiterValue = "")
        ?.takeIf { it.isNotBlank() }
}

private fun resolveImageRoot(root: String, relative: String): String {
    return when {
        root.startsWith("file:///android_asset/", ignoreCase = true) -> "$root/$relative"
        root.startsWith("file:/", ignoreCase = true) -> "$root/$relative"
        root.startsWith("content://", ignoreCase = true) -> "$root/$relative"
        else -> File(root).resolve(relative).absolutePath.replace('\\', '/')
    }
}
