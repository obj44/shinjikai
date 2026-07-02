package com.shinjikai.dictionary.data

private val GLOSSARY_BULLET_PREFIX_REGEX = Regex("""(?m)^\s*[\uD83D\uDD39\u25AA\u2022\u25CF\u25E6]\s*""")
private val SEARCH_PREVIEW_MULTISPACE_REGEX = Regex("""\s{2,}""")

internal fun cleanOfflineGlossaryText(glossary: String): String {
    return glossary
        .replace(GLOSSARY_BULLET_PREFIX_REGEX, "")
        .trim()
}

internal fun buildOfflineSearchPreview(glossary: String): String {
    return cleanOfflineGlossaryText(glossary)
        .replace("\n", " ")
        .replace(SEARCH_PREVIEW_MULTISPACE_REGEX, " ")
        .trim()
}
