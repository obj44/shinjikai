package com.shinjikai.dictionary.data

class LocalYomitanSource(
    private val yomitanDao: YomitanDao
) : DictionarySource {
    override suspend fun searchWords(term: String, page: Int): Result<SearchWordsResponse> {
        val query = term.trim()
        if (query.isBlank()) return Result.success(SearchWordsResponse(items = emptyList()))
        if (page > 0) {
            return Result.success(
                SearchWordsResponse(
                    items = emptyList(),
                    page = page,
                    pageCount = 1,
                    totalCount = 0
                )
            )
        }
        return runCatching {
            val rows = if (isArabicQuery(query)) {
                val normalizedQuery = normalizeArabic(query)
                val tokens = normalizedQuery.split(' ').filter { it.isNotBlank() }
                yomitanDao.searchArabic(
                    term = query,
                    normalizedTerm = normalizedQuery
                )
                    .asSequence()
                    .map { row ->
                        val normalizedGlossary = normalizeArabic(row.glossary)
                        val score = scoreArabicMatch(
                            query = normalizedQuery,
                            glossary = normalizedGlossary,
                            tokens = tokens
                        )
                        Triple(row, normalizedGlossary, score)
                    }
                    .filter { (_, glossary, score) ->
                        score < Int.MAX_VALUE && glossary.isNotBlank()
                    }
                    .sortedWith(
                        compareBy<Triple<YomitanTermEntity, String, Int>> { it.third }
                            .thenBy { it.second.length }
                            .thenBy { it.first.id }
                    )
                    .map { it.first }
                    .distinctBy { row ->
                        "${row.expression}\u0000${row.reading}\u0000${row.glossary}"
                    }
                    .take(80)
                    .toList()
            } else {
                yomitanDao.search(
                    term = query,
                    prefix = "$query%"
                ).distinctBy { row ->
                    "${row.expression}\u0000${row.reading}\u0000${row.glossary}"
                }
            }

            SearchWordsResponse(
                items = rows.map { row ->
                    SearchItem(
                        id = row.id,
                        kana = row.reading,
                        writings = listOf(Writing(text = row.expression)),
                        meaningSummary = buildSearchPreview(row.glossary),
                        jlpt = 0
                    )
                },
                page = 0,
                pageCount = if (rows.isEmpty()) 0 else 1,
                totalCount = rows.size
            )
        }
    }

    override suspend fun loadWordDetails(id: Int): Result<WordDetailsResponse> {
        return runCatching {
            val row = yomitanDao.getById(id)
                ?: error("No local entry found for id=$id")
            WordDetailsResponse(
                word = WordDetailsWord(
                    id = row.id,
                    kana = row.reading,
                    writings = listOf(Writing(text = row.expression)),
                    meanings = listOf(
                        Meaning(
                            arabic = cleanGlossary(row.glossary),
                            note = row.note
                        )
                    ),
                    jlpt = 0
                )
            )
        }
    }

    override suspend fun loadCategories(): Result<LoadCategoriesResponse> {
        return Result.success(LoadCategoriesResponse())
    }

    override suspend fun loadCategory(id: Int, page: Int): Result<LoadCategoryResponse> {
        return Result.success(
            LoadCategoryResponse(
                category = CategoryRef(id = id, name = "Local Category"),
                members = SearchWordsResponse()
            )
        )
    }

    private fun buildSearchPreview(glossary: String): String {
        return cleanGlossary(glossary)
            .replace("\n", " ")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }

    private fun cleanGlossary(glossary: String): String {
        return glossary
            .replace(Regex("""(?m)^\s*[🔹▪•●◦]\s*"""), "")
            .trim()
    }

    private fun isArabicQuery(text: String): Boolean {
        return text.any { ch -> ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F' }
    }

    private fun normalizeArabic(text: String): String {
        if (text.isBlank()) return ""
        return text
            .replace(Regex("""[\u064B-\u065F\u0670\u06D6-\u06ED]"""), "")
            .replace("ـ", "")
            .replace(Regex("""[أإآٱ]"""), "ا")
            .replace("ى", "ي")
            .replace("ؤ", "و")
            .replace("ئ", "ي")
            .replace("ة", "ه")
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }

    private fun scoreArabicMatch(
        query: String,
        glossary: String,
        tokens: List<String>
    ): Int {
        if (query.isBlank() || glossary.isBlank()) return Int.MAX_VALUE
        val hasAllTokens = tokens.all { glossary.contains(it) }
        if (!hasAllTokens) return Int.MAX_VALUE
        if (glossary == query) return 0
        if (glossary.startsWith("$query ")) return 1
        if (glossary.startsWith(query)) return 2
        val wholeWord = Regex("""(^|\s)${Regex.escape(query)}(\s|$)""")
        if (wholeWord.containsMatchIn(glossary)) return 3

        // Prefer earlier occurrences for partial matches.
        val index = glossary.indexOf(query)
        return if (index >= 0) 100 + index else Int.MAX_VALUE
    }
}
