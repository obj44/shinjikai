package com.shinjikai.dictionary.data

/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Japanese deinflection rules and transform model ported from Yomitan's Japanese language transforms.
 * Original copyright (C) 2024-2026 Yomitan Authors.
 * Source: https://github.com/yomidevs/yomitan/tree/master/ext/js/language/ja
 */

private const val MAX_DEINFLECTION_STATES = 512
private const val MAX_DEINFLECTION_CANDIDATES = 256

internal object JapaneseDeinflector {
    fun generateCandidates(term: String): List<String> {
        val normalized = term.trim()
        if (normalized.isBlank()) return emptyList()

        val results = ArrayList<TransformedText>(64)
        val seenStates = HashSet<String>()
        results.add(TransformedText(text = normalized, conditions = 0, trace = emptyList()))
        seenStates.add(stateKey(normalized, 0))

        var index = 0
        while (index < results.size && results.size < MAX_DEINFLECTION_STATES) {
            val current = results[index]
            index += 1

            compiledRules.forEachIndexed { ruleIndex, rule ->
                if (results.size >= MAX_DEINFLECTION_STATES) return@forEachIndexed
                if (!conditionsMatch(current.conditions, rule.conditionsIn)) return@forEachIndexed
                if (!rule.matches(current.text)) return@forEachIndexed
                if (current.trace.any { it.ruleIndex == ruleIndex && it.text == current.text }) return@forEachIndexed

                val nextText = rule.deinflect(current.text)
                if (nextText.isBlank()) return@forEachIndexed
                val nextConditions = rule.conditionsOut
                if (!seenStates.add(stateKey(nextText, nextConditions))) return@forEachIndexed
                results.add(
                    TransformedText(
                        text = nextText,
                        conditions = nextConditions,
                        trace = listOf(TraceFrame(ruleIndex = ruleIndex, text = current.text)) + current.trace
                    )
                )
            }
        }

        return results
            .asSequence()
            .map { it.text }
            .distinct()
            .take(MAX_DEINFLECTION_CANDIDATES)
            .toList()
    }

    private fun stateKey(text: String, conditions: Int): String = "$text|$conditions"

    private fun conditionsMatch(currentConditions: Int, nextConditions: Int): Boolean {
        return currentConditions == 0 || (currentConditions and nextConditions) != 0
    }

    private data class TransformedText(
        val text: String,
        val conditions: Int,
        val trace: List<TraceFrame>
    )

    private data class TraceFrame(
        val ruleIndex: Int,
        val text: String
    )

    private enum class RuleType {
        Suffix,
        WholeWord
    }

    private data class ConditionDefinition(
        val id: String,
        val subConditions: List<String>,
        val isDictionaryForm: Boolean
    )

    private data class RuleDefinition(
        val transformId: String,
        val type: RuleType,
        val inflected: String,
        val deinflected: String,
        val conditionsIn: List<String>,
        val conditionsOut: List<String>
    )

    private data class CompiledRule(
        val type: RuleType,
        val inflected: String,
        val deinflected: String,
        val conditionsIn: Int,
        val conditionsOut: Int
    ) {
        fun matches(text: String): Boolean {
            return when (type) {
                RuleType.Suffix -> text.endsWith(inflected)
                RuleType.WholeWord -> text == inflected
            }
        }

        fun deinflect(text: String): String {
            return when (type) {
                RuleType.Suffix -> text.dropLast(inflected.length) + deinflected
                RuleType.WholeWord -> deinflected
            }
        }
    }

    private val compiledRules: List<CompiledRule> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val conditionFlags = buildConditionFlags(parseConditionDefinitions())
        parseRuleDefinitions().map { definition ->
            CompiledRule(
                type = definition.type,
                inflected = definition.inflected,
                deinflected = definition.deinflected,
                conditionsIn = conditionFlags.strictFlags(definition.conditionsIn),
                conditionsOut = conditionFlags.strictFlags(definition.conditionsOut)
            )
        }
    }

    private fun buildConditionFlags(definitions: List<ConditionDefinition>): Map<String, Int> {
        val flags = LinkedHashMap<String, Int>()
        var nextFlagIndex = 0
        var pending = definitions

        while (pending.isNotEmpty()) {
            val nextPending = ArrayList<ConditionDefinition>()
            pending.forEach { definition ->
                if (definition.subConditions.isEmpty()) {
                    require(nextFlagIndex < Int.SIZE_BITS) { "Maximum number of Japanese deinflection conditions exceeded." }
                    flags[definition.id] = 1 shl nextFlagIndex
                    nextFlagIndex += 1
                } else {
                    val subFlags = definition.subConditions.mapNotNull(flags::get)
                    if (subFlags.size == definition.subConditions.size) {
                        flags[definition.id] = subFlags.fold(0) { acc, value -> acc or value }
                    } else {
                        nextPending.add(definition)
                    }
                }
            }
            require(nextPending.size != pending.size) { "Japanese deinflection condition graph could not be resolved." }
            pending = nextPending
        }

        return flags
    }

    private fun Map<String, Int>.strictFlags(conditionTypes: List<String>): Int {
        return conditionTypes.fold(0) { flags, condition ->
            flags or requireNotNull(this[condition]) { "Unknown Japanese deinflection condition: $condition" }
        }
    }

    private fun parseConditionDefinitions(): List<ConditionDefinition> {
        return CONDITION_DATA.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { line ->
                val parts = line.split('	', limit = 3)
                require(parts.size == 3) { "Invalid Japanese condition row: $line" }
                ConditionDefinition(
                    id = parts[0],
                    subConditions = parts[1].splitCsv(),
                    isDictionaryForm = parts[2] == "1"
                )
            }
            .toList()
    }

    private fun parseRuleDefinitions(): List<RuleDefinition> {
        return RULE_DATA.lineSequence()
            .filter(String::isNotEmpty)
            .map { line ->
                val rawParts = line.split('	', limit = 6)
                val parts = rawParts + List(6 - rawParts.size) { "" }
                require(parts.size == 6) { "Invalid Japanese deinflection rule row: $line" }
                RuleDefinition(
                    transformId = parts[0],
                    type = when (parts[1]) {
                        "suffix" -> RuleType.Suffix
                        "wholeWord" -> RuleType.WholeWord
                        else -> error("Unsupported Japanese deinflection rule type: ${parts[1]}")
                    },
                    inflected = parts[2],
                    deinflected = parts[3],
                    conditionsIn = parts[4].splitCsv(),
                    conditionsOut = parts[5].splitCsv()
                )
            }
            .toList()
    }

    private fun String.splitCsv(): List<String> {
        if (isBlank()) return emptyList()
        return split(',').filter { it.isNotBlank() }
    }

    private const val CONDITION_DATA = """
v	v1,v5,vk,vs,vz	0
v1	v1d,v1p	1
v1d		0
v1p		0
v5	v5d,v5s	1
v5d		0
v5s	v5ss,v5sp	0
v5ss		0
v5sp		0
vk		1
vs		1
vz		1
adj-i		1
-ます		0
-ません		0
-て		0
-ば		0
-く		0
-た		0
-ん		0
-なさい		0
-ゃ		0
"""

    private const val RULE_DATA = """
-ば	suffix	ければ	い	-ば	adj-i
-ば	suffix	えば	う	-ば	v5
-ば	suffix	けば	く	-ば	v5
-ば	suffix	げば	ぐ	-ば	v5
-ば	suffix	せば	す	-ば	v5
-ば	suffix	てば	つ	-ば	v5
-ば	suffix	ねば	ぬ	-ば	v5
-ば	suffix	べば	ぶ	-ば	v5
-ば	suffix	めば	む	-ば	v5
-ば	suffix	れば	る	-ば	v1,v5,vk,vs,vz
-ば	suffix	れば		-ば	-ます
-ゃ	suffix	けりゃ	ければ	-ゃ	-ば
-ゃ	suffix	きゃ	ければ	-ゃ	-ば
-ゃ	suffix	や	えば	-ゃ	-ば
-ゃ	suffix	きゃ	けば	-ゃ	-ば
-ゃ	suffix	ぎゃ	げば	-ゃ	-ば
-ゃ	suffix	しゃ	せば	-ゃ	-ば
-ゃ	suffix	ちゃ	てば	-ゃ	-ば
-ゃ	suffix	にゃ	ねば	-ゃ	-ば
-ゃ	suffix	びゃ	べば	-ゃ	-ば
-ゃ	suffix	みゃ	めば	-ゃ	-ば
-ゃ	suffix	りゃ	れば	-ゃ	-ば
-ちゃ	suffix	ちゃ	る	v5	v1
-ちゃ	suffix	いじゃ	ぐ	v5	v5
-ちゃ	suffix	いちゃ	く	v5	v5
-ちゃ	suffix	しちゃ	す	v5	v5
-ちゃ	suffix	っちゃ	う	v5	v5
-ちゃ	suffix	っちゃ	く	v5	v5
-ちゃ	suffix	っちゃ	つ	v5	v5
-ちゃ	suffix	っちゃ	る	v5	v5
-ちゃ	suffix	んじゃ	ぬ	v5	v5
-ちゃ	suffix	んじゃ	ぶ	v5	v5
-ちゃ	suffix	んじゃ	む	v5	v5
-ちゃ	suffix	じちゃ	ずる	v5	vz
-ちゃ	suffix	しちゃ	する	v5	vs
-ちゃ	suffix	為ちゃ	為る	v5	vs
-ちゃ	suffix	きちゃ	くる	v5	vk
-ちゃ	suffix	来ちゃ	来る	v5	vk
-ちゃ	suffix	來ちゃ	來る	v5	vk
-ちゃう	suffix	ちゃう	る	v5	v1
-ちゃう	suffix	いじゃう	ぐ	v5	v5
-ちゃう	suffix	いちゃう	く	v5	v5
-ちゃう	suffix	しちゃう	す	v5	v5
-ちゃう	suffix	っちゃう	う	v5	v5
-ちゃう	suffix	っちゃう	く	v5	v5
-ちゃう	suffix	っちゃう	つ	v5	v5
-ちゃう	suffix	っちゃう	る	v5	v5
-ちゃう	suffix	んじゃう	ぬ	v5	v5
-ちゃう	suffix	んじゃう	ぶ	v5	v5
-ちゃう	suffix	んじゃう	む	v5	v5
-ちゃう	suffix	じちゃう	ずる	v5	vz
-ちゃう	suffix	しちゃう	する	v5	vs
-ちゃう	suffix	為ちゃう	為る	v5	vs
-ちゃう	suffix	きちゃう	くる	v5	vk
-ちゃう	suffix	来ちゃう	来る	v5	vk
-ちゃう	suffix	來ちゃう	來る	v5	vk
-ちまう	suffix	ちまう	る	v5	v1
-ちまう	suffix	いじまう	ぐ	v5	v5
-ちまう	suffix	いちまう	く	v5	v5
-ちまう	suffix	しちまう	す	v5	v5
-ちまう	suffix	っちまう	う	v5	v5
-ちまう	suffix	っちまう	く	v5	v5
-ちまう	suffix	っちまう	つ	v5	v5
-ちまう	suffix	っちまう	る	v5	v5
-ちまう	suffix	んじまう	ぬ	v5	v5
-ちまう	suffix	んじまう	ぶ	v5	v5
-ちまう	suffix	んじまう	む	v5	v5
-ちまう	suffix	じちまう	ずる	v5	vz
-ちまう	suffix	しちまう	する	v5	vs
-ちまう	suffix	為ちまう	為る	v5	vs
-ちまう	suffix	きちまう	くる	v5	vk
-ちまう	suffix	来ちまう	来る	v5	vk
-ちまう	suffix	來ちまう	來る	v5	vk
-しまう	suffix	てしまう	て	v5	-て
-しまう	suffix	でしまう	で	v5	-て
-なさい	suffix	なさい	る	-なさい	v1
-なさい	suffix	いなさい	う	-なさい	v5
-なさい	suffix	きなさい	く	-なさい	v5
-なさい	suffix	ぎなさい	ぐ	-なさい	v5
-なさい	suffix	しなさい	す	-なさい	v5
-なさい	suffix	ちなさい	つ	-なさい	v5
-なさい	suffix	になさい	ぬ	-なさい	v5
-なさい	suffix	びなさい	ぶ	-なさい	v5
-なさい	suffix	みなさい	む	-なさい	v5
-なさい	suffix	りなさい	る	-なさい	v5
-なさい	suffix	じなさい	ずる	-なさい	vz
-なさい	suffix	しなさい	する	-なさい	vs
-なさい	suffix	為なさい	為る	-なさい	vs
-なさい	suffix	きなさい	くる	-なさい	vk
-なさい	suffix	来なさい	来る	-なさい	vk
-なさい	suffix	來なさい	來る	-なさい	vk
-そう	suffix	そう	い		adj-i
-そう	suffix	そう	る		v1
-そう	suffix	いそう	う		v5
-そう	suffix	きそう	く		v5
-そう	suffix	ぎそう	ぐ		v5
-そう	suffix	しそう	す		v5
-そう	suffix	ちそう	つ		v5
-そう	suffix	にそう	ぬ		v5
-そう	suffix	びそう	ぶ		v5
-そう	suffix	みそう	む		v5
-そう	suffix	りそう	る		v5
-そう	suffix	じそう	ずる		vz
-そう	suffix	しそう	する		vs
-そう	suffix	為そう	為る		vs
-そう	suffix	きそう	くる		vk
-そう	suffix	来そう	来る		vk
-そう	suffix	來そう	來る		vk
-すぎる	suffix	すぎる	い	v1	adj-i
-すぎる	suffix	すぎる	る	v1	v1
-すぎる	suffix	いすぎる	う	v1	v5
-すぎる	suffix	きすぎる	く	v1	v5
-すぎる	suffix	ぎすぎる	ぐ	v1	v5
-すぎる	suffix	しすぎる	す	v1	v5
-すぎる	suffix	ちすぎる	つ	v1	v5
-すぎる	suffix	にすぎる	ぬ	v1	v5
-すぎる	suffix	びすぎる	ぶ	v1	v5
-すぎる	suffix	みすぎる	む	v1	v5
-すぎる	suffix	りすぎる	る	v1	v5
-すぎる	suffix	じすぎる	ずる	v1	vz
-すぎる	suffix	しすぎる	する	v1	vs
-すぎる	suffix	為すぎる	為る	v1	vs
-すぎる	suffix	きすぎる	くる	v1	vk
-すぎる	suffix	来すぎる	来る	v1	vk
-すぎる	suffix	來すぎる	來る	v1	vk
-過ぎる	suffix	過ぎる	い	v1	adj-i
-過ぎる	suffix	過ぎる	る	v1	v1
-過ぎる	suffix	い過ぎる	う	v1	v5
-過ぎる	suffix	き過ぎる	く	v1	v5
-過ぎる	suffix	ぎ過ぎる	ぐ	v1	v5
-過ぎる	suffix	し過ぎる	す	v1	v5
-過ぎる	suffix	ち過ぎる	つ	v1	v5
-過ぎる	suffix	に過ぎる	ぬ	v1	v5
-過ぎる	suffix	び過ぎる	ぶ	v1	v5
-過ぎる	suffix	み過ぎる	む	v1	v5
-過ぎる	suffix	り過ぎる	る	v1	v5
-過ぎる	suffix	じ過ぎる	ずる	v1	vz
-過ぎる	suffix	し過ぎる	する	v1	vs
-過ぎる	suffix	為過ぎる	為る	v1	vs
-過ぎる	suffix	き過ぎる	くる	v1	vk
-過ぎる	suffix	来過ぎる	来る	v1	vk
-過ぎる	suffix	來過ぎる	來る	v1	vk
-たい	suffix	たい	る	adj-i	v1
-たい	suffix	いたい	う	adj-i	v5
-たい	suffix	きたい	く	adj-i	v5
-たい	suffix	ぎたい	ぐ	adj-i	v5
-たい	suffix	したい	す	adj-i	v5
-たい	suffix	ちたい	つ	adj-i	v5
-たい	suffix	にたい	ぬ	adj-i	v5
-たい	suffix	びたい	ぶ	adj-i	v5
-たい	suffix	みたい	む	adj-i	v5
-たい	suffix	りたい	る	adj-i	v5
-たい	suffix	じたい	ずる	adj-i	vz
-たい	suffix	したい	する	adj-i	vs
-たい	suffix	為たい	為る	adj-i	vs
-たい	suffix	きたい	くる	adj-i	vk
-たい	suffix	来たい	来る	adj-i	vk
-たい	suffix	來たい	來る	adj-i	vk
-たら	suffix	かったら	い		adj-i
-たら	suffix	たら	る		v1
-たら	suffix	いたら	く		v5
-たら	suffix	いだら	ぐ		v5
-たら	suffix	したら	す		v5
-たら	suffix	ったら	う		v5
-たら	suffix	ったら	つ		v5
-たら	suffix	ったら	る		v5
-たら	suffix	んだら	ぬ		v5
-たら	suffix	んだら	ぶ		v5
-たら	suffix	んだら	む		v5
-たら	suffix	じたら	ずる		vz
-たら	suffix	したら	する		vs
-たら	suffix	為たら	為る		vs
-たら	suffix	きたら	くる		vk
-たら	suffix	来たら	来る		vk
-たら	suffix	來たら	來る		vk
-たら	suffix	いったら	いく		v5
-たら	suffix	行ったら	行く		v5
-たら	suffix	逝ったら	逝く		v5
-たら	suffix	往ったら	往く		v5
-たら	suffix	こうたら	こう		v5
-たら	suffix	とうたら	とう		v5
-たら	suffix	請うたら	請う		v5
-たら	suffix	乞うたら	乞う		v5
-たら	suffix	恋うたら	恋う		v5
-たら	suffix	問うたら	問う		v5
-たら	suffix	訪うたら	訪う		v5
-たら	suffix	宣うたら	宣う		v5
-たら	suffix	曰うたら	曰う		v5
-たら	suffix	給うたら	給う		v5
-たら	suffix	賜うたら	賜う		v5
-たら	suffix	揺蕩うたら	揺蕩う		v5
-たら	suffix	のたもうたら	のたまう		v5
-たら	suffix	たもうたら	たまう		v5
-たら	suffix	たゆとうたら	たゆたう		v5
-たら	suffix	ましたら	ます		-ます
-たり	suffix	かったり	い		adj-i
-たり	suffix	たり	る		v1
-たり	suffix	いたり	く		v5
-たり	suffix	いだり	ぐ		v5
-たり	suffix	したり	す		v5
-たり	suffix	ったり	う		v5
-たり	suffix	ったり	つ		v5
-たり	suffix	ったり	る		v5
-たり	suffix	んだり	ぬ		v5
-たり	suffix	んだり	ぶ		v5
-たり	suffix	んだり	む		v5
-たり	suffix	じたり	ずる		vz
-たり	suffix	したり	する		vs
-たり	suffix	為たり	為る		vs
-たり	suffix	きたり	くる		vk
-たり	suffix	来たり	来る		vk
-たり	suffix	來たり	來る		vk
-たり	suffix	いったり	いく		v5
-たり	suffix	行ったり	行く		v5
-たり	suffix	逝ったり	逝く		v5
-たり	suffix	往ったり	往く		v5
-たり	suffix	こうたり	こう		v5
-たり	suffix	とうたり	とう		v5
-たり	suffix	請うたり	請う		v5
-たり	suffix	乞うたり	乞う		v5
-たり	suffix	恋うたり	恋う		v5
-たり	suffix	問うたり	問う		v5
-たり	suffix	訪うたり	訪う		v5
-たり	suffix	宣うたり	宣う		v5
-たり	suffix	曰うたり	曰う		v5
-たり	suffix	給うたり	給う		v5
-たり	suffix	賜うたり	賜う		v5
-たり	suffix	揺蕩うたり	揺蕩う		v5
-たり	suffix	のたもうたり	のたまう		v5
-たり	suffix	たもうたり	たまう		v5
-たり	suffix	たゆとうたり	たゆたう		v5
-て	suffix	くて	い	-て	adj-i
-て	suffix	て	る	-て	v1
-て	suffix	いて	く	-て	v5
-て	suffix	いで	ぐ	-て	v5
-て	suffix	して	す	-て	v5
-て	suffix	って	う	-て	v5
-て	suffix	って	つ	-て	v5
-て	suffix	って	る	-て	v5
-て	suffix	んで	ぬ	-て	v5
-て	suffix	んで	ぶ	-て	v5
-て	suffix	んで	む	-て	v5
-て	suffix	じて	ずる	-て	vz
-て	suffix	して	する	-て	vs
-て	suffix	為て	為る	-て	vs
-て	suffix	きて	くる	-て	vk
-て	suffix	来て	来る	-て	vk
-て	suffix	來て	來る	-て	vk
-て	suffix	いって	いく	-て	v5
-て	suffix	行って	行く	-て	v5
-て	suffix	逝って	逝く	-て	v5
-て	suffix	往って	往く	-て	v5
-て	suffix	こうて	こう	-て	v5
-て	suffix	とうて	とう	-て	v5
-て	suffix	請うて	請う	-て	v5
-て	suffix	乞うて	乞う	-て	v5
-て	suffix	恋うて	恋う	-て	v5
-て	suffix	問うて	問う	-て	v5
-て	suffix	訪うて	訪う	-て	v5
-て	suffix	宣うて	宣う	-て	v5
-て	suffix	曰うて	曰う	-て	v5
-て	suffix	給うて	給う	-て	v5
-て	suffix	賜うて	賜う	-て	v5
-て	suffix	揺蕩うて	揺蕩う	-て	v5
-て	suffix	のたもうて	のたまう	-て	v5
-て	suffix	たもうて	たまう	-て	v5
-て	suffix	たゆとうて	たゆたう	-て	v5
-て	suffix	まして	ます		-ます
-ず	suffix	ず	る		v1
-ず	suffix	かず	く		v5
-ず	suffix	がず	ぐ		v5
-ず	suffix	さず	す		v5
-ず	suffix	たず	つ		v5
-ず	suffix	なず	ぬ		v5
-ず	suffix	ばず	ぶ		v5
-ず	suffix	まず	む		v5
-ず	suffix	らず	る		v5
-ず	suffix	わず	う		v5
-ず	suffix	ぜず	ずる		vz
-ず	suffix	せず	する		vs
-ず	suffix	為ず	為る		vs
-ず	suffix	こず	くる		vk
-ず	suffix	来ず	来る		vk
-ず	suffix	來ず	來る		vk
-ぬ	suffix	ぬ	る		v1
-ぬ	suffix	かぬ	く		v5
-ぬ	suffix	がぬ	ぐ		v5
-ぬ	suffix	さぬ	す		v5
-ぬ	suffix	たぬ	つ		v5
-ぬ	suffix	なぬ	ぬ		v5
-ぬ	suffix	ばぬ	ぶ		v5
-ぬ	suffix	まぬ	む		v5
-ぬ	suffix	らぬ	る		v5
-ぬ	suffix	わぬ	う		v5
-ぬ	suffix	ぜぬ	ずる		vz
-ぬ	suffix	せぬ	する		vs
-ぬ	suffix	為ぬ	為る		vs
-ぬ	suffix	こぬ	くる		vk
-ぬ	suffix	来ぬ	来る		vk
-ぬ	suffix	來ぬ	來る		vk
-ん	suffix	ん	る	-ん	v1
-ん	suffix	かん	く	-ん	v5
-ん	suffix	がん	ぐ	-ん	v5
-ん	suffix	さん	す	-ん	v5
-ん	suffix	たん	つ	-ん	v5
-ん	suffix	なん	ぬ	-ん	v5
-ん	suffix	ばん	ぶ	-ん	v5
-ん	suffix	まん	む	-ん	v5
-ん	suffix	らん	る	-ん	v5
-ん	suffix	わん	う	-ん	v5
-ん	suffix	ぜん	ずる	-ん	vz
-ん	suffix	せん	する	-ん	vs
-ん	suffix	為ん	為る	-ん	vs
-ん	suffix	こん	くる	-ん	vk
-ん	suffix	来ん	来る	-ん	vk
-ん	suffix	來ん	來る	-ん	vk
-んばかり	suffix	んばかり	る		v1
-んばかり	suffix	かんばかり	く		v5
-んばかり	suffix	がんばかり	ぐ		v5
-んばかり	suffix	さんばかり	す		v5
-んばかり	suffix	たんばかり	つ		v5
-んばかり	suffix	なんばかり	ぬ		v5
-んばかり	suffix	ばんばかり	ぶ		v5
-んばかり	suffix	まんばかり	む		v5
-んばかり	suffix	らんばかり	る		v5
-んばかり	suffix	わんばかり	う		v5
-んばかり	suffix	ぜんばかり	ずる		vz
-んばかり	suffix	せんばかり	する		vs
-んばかり	suffix	為んばかり	為る		vs
-んばかり	suffix	こんばかり	くる		vk
-んばかり	suffix	来んばかり	来る		vk
-んばかり	suffix	來んばかり	來る		vk
-んとする	suffix	んとする	る	vs	v1
-んとする	suffix	かんとする	く	vs	v5
-んとする	suffix	がんとする	ぐ	vs	v5
-んとする	suffix	さんとする	す	vs	v5
-んとする	suffix	たんとする	つ	vs	v5
-んとする	suffix	なんとする	ぬ	vs	v5
-んとする	suffix	ばんとする	ぶ	vs	v5
-んとする	suffix	まんとする	む	vs	v5
-んとする	suffix	らんとする	る	vs	v5
-んとする	suffix	わんとする	う	vs	v5
-んとする	suffix	ぜんとする	ずる	vs	vz
-んとする	suffix	せんとする	する	vs	vs
-んとする	suffix	為んとする	為る	vs	vs
-んとする	suffix	こんとする	くる	vs	vk
-んとする	suffix	来んとする	来る	vs	vk
-んとする	suffix	來んとする	來る	vs	vk
-む	suffix	む	る		v1
-む	suffix	かむ	く		v5
-む	suffix	がむ	ぐ		v5
-む	suffix	さむ	す		v5
-む	suffix	たむ	つ		v5
-む	suffix	なむ	ぬ		v5
-む	suffix	ばむ	ぶ		v5
-む	suffix	まむ	む		v5
-む	suffix	らむ	る		v5
-む	suffix	わむ	う		v5
-む	suffix	ぜむ	ずる		vz
-む	suffix	せむ	する		vs
-む	suffix	為む	為る		vs
-む	suffix	こむ	くる		vk
-む	suffix	来む	来る		vk
-む	suffix	來む	來る		vk
-ざる	suffix	ざる	る		v1
-ざる	suffix	かざる	く		v5
-ざる	suffix	がざる	ぐ		v5
-ざる	suffix	さざる	す		v5
-ざる	suffix	たざる	つ		v5
-ざる	suffix	なざる	ぬ		v5
-ざる	suffix	ばざる	ぶ		v5
-ざる	suffix	まざる	む		v5
-ざる	suffix	らざる	る		v5
-ざる	suffix	わざる	う		v5
-ざる	suffix	ぜざる	ずる		vz
-ざる	suffix	せざる	する		vs
-ざる	suffix	為ざる	為る		vs
-ざる	suffix	こざる	くる		vk
-ざる	suffix	来ざる	来る		vk
-ざる	suffix	來ざる	來る		vk
-ねば	suffix	ねば	る	-ば	v1
-ねば	suffix	かねば	く	-ば	v5
-ねば	suffix	がねば	ぐ	-ば	v5
-ねば	suffix	さねば	す	-ば	v5
-ねば	suffix	たねば	つ	-ば	v5
-ねば	suffix	なねば	ぬ	-ば	v5
-ねば	suffix	ばねば	ぶ	-ば	v5
-ねば	suffix	まねば	む	-ば	v5
-ねば	suffix	らねば	る	-ば	v5
-ねば	suffix	わねば	う	-ば	v5
-ねば	suffix	ぜねば	ずる	-ば	vz
-ねば	suffix	せねば	する	-ば	vs
-ねば	suffix	為ねば	為る	-ば	vs
-ねば	suffix	こねば	くる	-ば	vk
-ねば	suffix	来ねば	来る	-ば	vk
-ねば	suffix	來ねば	來る	-ば	vk
-く	suffix	く	い	-く	adj-i
causative	suffix	させる	る	v1	v1
causative	suffix	かせる	く	v1	v5
causative	suffix	がせる	ぐ	v1	v5
causative	suffix	させる	す	v1	v5
causative	suffix	たせる	つ	v1	v5
causative	suffix	なせる	ぬ	v1	v5
causative	suffix	ばせる	ぶ	v1	v5
causative	suffix	ませる	む	v1	v5
causative	suffix	らせる	る	v1	v5
causative	suffix	わせる	う	v1	v5
causative	suffix	じさせる	ずる	v1	vz
causative	suffix	ぜさせる	ずる	v1	vz
causative	suffix	させる	する	v1	vs
causative	suffix	為せる	為る	v1	vs
causative	suffix	せさせる	する	v1	vs
causative	suffix	為させる	為る	v1	vs
causative	suffix	こさせる	くる	v1	vk
causative	suffix	来させる	来る	v1	vk
causative	suffix	來させる	來る	v1	vk
short causative	suffix	さす	る	v5ss	v1
short causative	suffix	かす	く	v5sp	v5
short causative	suffix	がす	ぐ	v5sp	v5
short causative	suffix	さす	す	v5ss	v5
short causative	suffix	たす	つ	v5sp	v5
short causative	suffix	なす	ぬ	v5sp	v5
short causative	suffix	ばす	ぶ	v5sp	v5
short causative	suffix	ます	む	v5sp	v5
short causative	suffix	らす	る	v5sp	v5
short causative	suffix	わす	う	v5sp	v5
short causative	suffix	じさす	ずる	v5ss	vz
short causative	suffix	ぜさす	ずる	v5ss	vz
short causative	suffix	さす	する	v5ss	vs
short causative	suffix	為す	為る	v5ss	vs
short causative	suffix	こさす	くる	v5ss	vk
short causative	suffix	来さす	来る	v5ss	vk
short causative	suffix	來さす	來る	v5ss	vk
imperative	suffix	ろ	る		v1
imperative	suffix	よ	る		v1
imperative	suffix	え	う		v5
imperative	suffix	け	く		v5
imperative	suffix	げ	ぐ		v5
imperative	suffix	せ	す		v5
imperative	suffix	て	つ		v5
imperative	suffix	ね	ぬ		v5
imperative	suffix	べ	ぶ		v5
imperative	suffix	め	む		v5
imperative	suffix	れ	る		v5
imperative	suffix	じろ	ずる		vz
imperative	suffix	ぜよ	ずる		vz
imperative	suffix	しろ	する		vs
imperative	suffix	せよ	する		vs
imperative	suffix	為ろ	為る		vs
imperative	suffix	為よ	為る		vs
imperative	suffix	こい	くる		vk
imperative	suffix	来い	来る		vk
imperative	suffix	來い	來る		vk
continuative	suffix	い	いる		v1d
continuative	suffix	え	える		v1d
continuative	suffix	き	きる		v1d
continuative	suffix	ぎ	ぎる		v1d
continuative	suffix	け	ける		v1d
continuative	suffix	げ	げる		v1d
continuative	suffix	じ	じる		v1d
continuative	suffix	せ	せる		v1d
continuative	suffix	ぜ	ぜる		v1d
continuative	suffix	ち	ちる		v1d
continuative	suffix	て	てる		v1d
continuative	suffix	で	でる		v1d
continuative	suffix	に	にる		v1d
continuative	suffix	ね	ねる		v1d
continuative	suffix	ひ	ひる		v1d
continuative	suffix	び	びる		v1d
continuative	suffix	へ	へる		v1d
continuative	suffix	べ	べる		v1d
continuative	suffix	み	みる		v1d
continuative	suffix	め	める		v1d
continuative	suffix	り	りる		v1d
continuative	suffix	れ	れる		v1d
continuative	suffix	い	う		v5
continuative	suffix	き	く		v5
continuative	suffix	ぎ	ぐ		v5
continuative	suffix	し	す		v5
continuative	suffix	ち	つ		v5
continuative	suffix	に	ぬ		v5
continuative	suffix	び	ぶ		v5
continuative	suffix	み	む		v5
continuative	suffix	り	る		v5
continuative	suffix	き	くる		vk
continuative	suffix	し	する		vs
continuative	suffix	来	来る		vk
continuative	suffix	來	來る		vk
negative	suffix	くない	い	adj-i	adj-i
negative	suffix	ない	る	adj-i	v1
negative	suffix	かない	く	adj-i	v5
negative	suffix	がない	ぐ	adj-i	v5
negative	suffix	さない	す	adj-i	v5
negative	suffix	たない	つ	adj-i	v5
negative	suffix	なない	ぬ	adj-i	v5
negative	suffix	ばない	ぶ	adj-i	v5
negative	suffix	まない	む	adj-i	v5
negative	suffix	らない	る	adj-i	v5
negative	suffix	わない	う	adj-i	v5
negative	suffix	じない	ずる	adj-i	vz
negative	suffix	しない	する	adj-i	vs
negative	suffix	為ない	為る	adj-i	vs
negative	suffix	こない	くる	adj-i	vk
negative	suffix	来ない	来る	adj-i	vk
negative	suffix	來ない	來る	adj-i	vk
negative	suffix	ません	ます	-ません	-ます
-さ	suffix	さ	い		adj-i
passive	suffix	かれる	く	v1	v5
passive	suffix	がれる	ぐ	v1	v5
passive	suffix	される	す	v1	v5d,v5sp
passive	suffix	たれる	つ	v1	v5
passive	suffix	なれる	ぬ	v1	v5
passive	suffix	ばれる	ぶ	v1	v5
passive	suffix	まれる	む	v1	v5
passive	suffix	われる	う	v1	v5
passive	suffix	られる	る	v1	v5
passive	suffix	じされる	ずる	v1	vz
passive	suffix	ぜされる	ずる	v1	vz
passive	suffix	される	する	v1	vs
passive	suffix	為れる	為る	v1	vs
passive	suffix	こられる	くる	v1	vk
passive	suffix	来られる	来る	v1	vk
passive	suffix	來られる	來る	v1	vk
-た	suffix	かった	い	-た	adj-i
-た	suffix	た	る	-た	v1
-た	suffix	いた	く	-た	v5
-た	suffix	いだ	ぐ	-た	v5
-た	suffix	した	す	-た	v5
-た	suffix	った	う	-た	v5
-た	suffix	った	つ	-た	v5
-た	suffix	った	る	-た	v5
-た	suffix	んだ	ぬ	-た	v5
-た	suffix	んだ	ぶ	-た	v5
-た	suffix	んだ	む	-た	v5
-た	suffix	じた	ずる	-た	vz
-た	suffix	した	する	-た	vs
-た	suffix	為た	為る	-た	vs
-た	suffix	きた	くる	-た	vk
-た	suffix	来た	来る	-た	vk
-た	suffix	來た	來る	-た	vk
-た	suffix	いった	いく	-た	v5
-た	suffix	行った	行く	-た	v5
-た	suffix	逝った	逝く	-た	v5
-た	suffix	往った	往く	-た	v5
-た	suffix	こうた	こう	-た	v5
-た	suffix	とうた	とう	-た	v5
-た	suffix	請うた	請う	-た	v5
-た	suffix	乞うた	乞う	-た	v5
-た	suffix	恋うた	恋う	-た	v5
-た	suffix	問うた	問う	-た	v5
-た	suffix	訪うた	訪う	-た	v5
-た	suffix	宣うた	宣う	-た	v5
-た	suffix	曰うた	曰う	-た	v5
-た	suffix	給うた	給う	-た	v5
-た	suffix	賜うた	賜う	-た	v5
-た	suffix	揺蕩うた	揺蕩う	-た	v5
-た	suffix	のたもうた	のたまう	-た	v5
-た	suffix	たもうた	たまう	-た	v5
-た	suffix	たゆとうた	たゆたう	-た	v5
-た	suffix	ました	ます	-た	-ます
-た	suffix	でした		-た	-ません
-た	suffix	かった		-た	-ません,-ん
-ます	suffix	ます	る	-ます	v1
-ます	wholeWord	いらっしゃいます	いらっしゃる	-ます	v5d
-ます	wholeWord	ございます	ござる	-ます	v5d
-ます	wholeWord	なさいます	なさる	-ます	v5d
-ます	wholeWord	くださいます	くださる	-ます	v5d
-ます	wholeWord	下さいます	下さる	-ます	v5d
-ます	wholeWord	おっしゃいます	おっしゃる	-ます	v5d
-ます	wholeWord	仰います	仰る	-ます	v5d
-ます	wholeWord	仰有います	仰有る	-ます	v5d
-ます	suffix	います	う	-ます	v5d
-ます	suffix	きます	く	-ます	v5d
-ます	suffix	ぎます	ぐ	-ます	v5d
-ます	suffix	します	す	-ます	v5d,v5s
-ます	suffix	ちます	つ	-ます	v5d
-ます	suffix	にます	ぬ	-ます	v5d
-ます	suffix	びます	ぶ	-ます	v5d
-ます	suffix	みます	む	-ます	v5d
-ます	suffix	ります	る	-ます	v5d
-ます	suffix	じます	ずる	-ます	vz
-ます	suffix	します	する	-ます	vs
-ます	suffix	為ます	為る	-ます	vs
-ます	suffix	きます	くる	-ます	vk
-ます	suffix	来ます	来る	-ます	vk
-ます	suffix	來ます	來る	-ます	vk
-ます	suffix	くあります	い	-ます	adj-i
potential	suffix	れる	る	v1	v1,v5d
potential	suffix	える	う	v1	v5d
potential	suffix	ける	く	v1	v5d
potential	suffix	げる	ぐ	v1	v5d
potential	suffix	せる	す	v1	v5d
potential	suffix	てる	つ	v1	v5d
potential	suffix	ねる	ぬ	v1	v5d
potential	suffix	べる	ぶ	v1	v5d
potential	suffix	める	む	v1	v5d
potential	suffix	できる	する	v1	vs
potential	suffix	出来る	する	v1	vs
potential	suffix	これる	くる	v1	vk
potential	suffix	来れる	来る	v1	vk
potential	suffix	來れる	來る	v1	vk
potential or passive	suffix	られる	る	v1	v1
potential or passive	suffix	ざれる	ずる	v1	vz
potential or passive	suffix	ぜられる	ずる	v1	vz
potential or passive	suffix	せられる	する	v1	vs
potential or passive	suffix	為られる	為る	v1	vs
potential or passive	suffix	こられる	くる	v1	vk
potential or passive	suffix	来られる	来る	v1	vk
potential or passive	suffix	來られる	來る	v1	vk
volitional	suffix	よう	る		v1
volitional	suffix	おう	う		v5
volitional	suffix	こう	く		v5
volitional	suffix	ごう	ぐ		v5
volitional	suffix	そう	す		v5
volitional	suffix	とう	つ		v5
volitional	suffix	のう	ぬ		v5
volitional	suffix	ぼう	ぶ		v5
volitional	suffix	もう	む		v5
volitional	suffix	ろう	る		v5
volitional	suffix	じよう	ずる		vz
volitional	suffix	しよう	する		vs
volitional	suffix	為よう	為る		vs
volitional	suffix	こよう	くる		vk
volitional	suffix	来よう	来る		vk
volitional	suffix	來よう	來る		vk
volitional	suffix	ましょう	ます		-ます
volitional	suffix	かろう	い		adj-i
volitional slang	suffix	よっか	る		v1
volitional slang	suffix	おっか	う		v5
volitional slang	suffix	こっか	く		v5
volitional slang	suffix	ごっか	ぐ		v5
volitional slang	suffix	そっか	す		v5
volitional slang	suffix	とっか	つ		v5
volitional slang	suffix	のっか	ぬ		v5
volitional slang	suffix	ぼっか	ぶ		v5
volitional slang	suffix	もっか	む		v5
volitional slang	suffix	ろっか	る		v5
volitional slang	suffix	じよっか	ずる		vz
volitional slang	suffix	しよっか	する		vs
volitional slang	suffix	為よっか	為る		vs
volitional slang	suffix	こよっか	くる		vk
volitional slang	suffix	来よっか	来る		vk
volitional slang	suffix	來よっか	來る		vk
volitional slang	suffix	ましょっか	ます		-ます
-まい	suffix	まい			v
-まい	suffix	まい	る		v1
-まい	suffix	じまい	ずる		vz
-まい	suffix	しまい	する		vs
-まい	suffix	為まい	為る		vs
-まい	suffix	こまい	くる		vk
-まい	suffix	来まい	来る		vk
-まい	suffix	來まい	來る		vk
-まい	suffix	まい			-ます
-おく	suffix	ておく	て	v5	-て
-おく	suffix	でおく	で	v5	-て
-おく	suffix	とく	て	v5	-て
-おく	suffix	どく	で	v5	-て
-おく	suffix	ないでおく	ない	v5	adj-i
-おく	suffix	ないどく	ない	v5	adj-i
-いる	suffix	ている	て	v1	-て
-いる	suffix	ておる	て	v5	-て
-いる	suffix	てる	て	v1p	-て
-いる	suffix	でいる	で	v1	-て
-いる	suffix	でおる	で	v5	-て
-いる	suffix	でる	で	v1p	-て
-いる	suffix	とる	て	v5	-て
-いる	suffix	ないでいる	ない	v1	adj-i
-き	suffix	き	い		adj-i
-げ	suffix	げ	い		adj-i
-げ	suffix	気	い		adj-i
-がる	suffix	がる	い	v5	adj-i
-やがる	suffix	やがる	る	v5	v1
-やがる	suffix	いやがる	う	v5	v5
-やがる	suffix	きやがる	く	v5	v5
-やがる	suffix	ぎやがる	ぐ	v5	v5
-やがる	suffix	しやがる	す	v5	v5
-やがる	suffix	ちやがる	つ	v5	v5
-やがる	suffix	にやがる	ぬ	v5	v5
-やがる	suffix	びやがる	ぶ	v5	v5
-やがる	suffix	みやがる	む	v5	v5
-やがる	suffix	りやがる	る	v5	v5
-やがる	suffix	じやがる	ずる	v5	vz
-やがる	suffix	しやがる	する	v5	vs
-やがる	suffix	為やがる	為る	v5	vs
-やがる	suffix	きやがる	くる	v5	vk
-やがる	suffix	来やがる	来る	v5	vk
-やがる	suffix	來やがる	來る	v5	vk
-え	suffix	ねえ	ない		adj-i
-え	suffix	めえ	むい		adj-i
-え	suffix	みい	むい		adj-i
-え	suffix	ちぇえ	つい		adj-i
-え	suffix	ちい	つい		adj-i
-え	suffix	せえ	すい		adj-i
-え	suffix	ええ	いい		adj-i
-え	suffix	ええ	わい		adj-i
-え	suffix	ええ	よい		adj-i
-え	suffix	いぇえ	よい		adj-i
-え	suffix	うぇえ	わい		adj-i
-え	suffix	けえ	かい		adj-i
-え	suffix	げえ	がい		adj-i
-え	suffix	げえ	ごい		adj-i
-え	suffix	せえ	さい		adj-i
-え	suffix	めえ	まい		adj-i
-え	suffix	ぜえ	ずい		adj-i
-え	suffix	っぜえ	ずい		adj-i
-え	suffix	れえ	らい		adj-i
-え	suffix	れえ	らい		adj-i
-え	suffix	ちぇえ	ちゃい		adj-i
-え	suffix	でえ	どい		adj-i
-え	suffix	れえ	れい		adj-i
-え	suffix	べえ	ばい		adj-i
-え	suffix	てえ	たい		adj-i
-え	suffix	ねぇ	ない		adj-i
-え	suffix	めぇ	むい		adj-i
-え	suffix	みぃ	むい		adj-i
-え	suffix	ちぃ	つい		adj-i
-え	suffix	せぇ	すい		adj-i
-え	suffix	けぇ	かい		adj-i
-え	suffix	げぇ	がい		adj-i
-え	suffix	げぇ	ごい		adj-i
-え	suffix	せぇ	さい		adj-i
-え	suffix	めぇ	まい		adj-i
-え	suffix	ぜぇ	ずい		adj-i
-え	suffix	っぜぇ	ずい		adj-i
-え	suffix	れぇ	らい		adj-i
-え	suffix	でぇ	どい		adj-i
-え	suffix	れぇ	れい		adj-i
-え	suffix	べぇ	ばい		adj-i
-え	suffix	てぇ	たい		adj-i
n-slang	suffix	んなさい	りなさい		-なさい
n-slang	suffix	らんない	られない	adj-i	adj-i
n-slang	suffix	んない	らない	adj-i	adj-i
n-slang	suffix	んなきゃ	らなきゃ		-ゃ
n-slang	suffix	んなきゃ	れなきゃ		-ゃ
imperative negative slang	suffix	んな	る		v
kansai-ben negative	suffix	へん	ない		adj-i
kansai-ben negative	suffix	ひん	ない		adj-i
kansai-ben negative	suffix	せえへん	しない		adj-i
kansai-ben negative	suffix	へんかった	なかった	-た	-た
kansai-ben negative	suffix	ひんかった	なかった	-た	-た
kansai-ben negative	suffix	うてへん	ってない		adj-i
kansai-ben -て	suffix	うて	って	-て	-て
kansai-ben -て	suffix	おうて	あって	-て	-て
kansai-ben -て	suffix	こうて	かって	-て	-て
kansai-ben -て	suffix	ごうて	がって	-て	-て
kansai-ben -て	suffix	そうて	さって	-て	-て
kansai-ben -て	suffix	ぞうて	ざって	-て	-て
kansai-ben -て	suffix	とうて	たって	-て	-て
kansai-ben -て	suffix	どうて	だって	-て	-て
kansai-ben -て	suffix	のうて	なって	-て	-て
kansai-ben -て	suffix	ほうて	はって	-て	-て
kansai-ben -て	suffix	ぼうて	ばって	-て	-て
kansai-ben -て	suffix	もうて	まって	-て	-て
kansai-ben -て	suffix	ろうて	らって	-て	-て
kansai-ben -て	suffix	ようて	やって	-て	-て
kansai-ben -て	suffix	ゆうて	いって	-て	-て
kansai-ben -た	suffix	うた	った	-た	-た
kansai-ben -た	suffix	おうた	あった	-た	-た
kansai-ben -た	suffix	こうた	かった	-た	-た
kansai-ben -た	suffix	ごうた	がった	-た	-た
kansai-ben -た	suffix	そうた	さった	-た	-た
kansai-ben -た	suffix	ぞうた	ざった	-た	-た
kansai-ben -た	suffix	とうた	たった	-た	-た
kansai-ben -た	suffix	どうた	だった	-た	-た
kansai-ben -た	suffix	のうた	なった	-た	-た
kansai-ben -た	suffix	ほうた	はった	-た	-た
kansai-ben -た	suffix	ぼうた	ばった	-た	-た
kansai-ben -た	suffix	もうた	まった	-た	-た
kansai-ben -た	suffix	ろうた	らった	-た	-た
kansai-ben -た	suffix	ようた	やった	-た	-た
kansai-ben -た	suffix	ゆうた	いった	-た	-た
kansai-ben -たら	suffix	うたら	ったら
kansai-ben -たら	suffix	おうたら	あったら
kansai-ben -たら	suffix	こうたら	かったら
kansai-ben -たら	suffix	ごうたら	がったら
kansai-ben -たら	suffix	そうたら	さったら
kansai-ben -たら	suffix	ぞうたら	ざったら
kansai-ben -たら	suffix	とうたら	たったら
kansai-ben -たら	suffix	どうたら	だったら
kansai-ben -たら	suffix	のうたら	なったら
kansai-ben -たら	suffix	ほうたら	はったら
kansai-ben -たら	suffix	ぼうたら	ばったら
kansai-ben -たら	suffix	もうたら	まったら
kansai-ben -たら	suffix	ろうたら	らったら
kansai-ben -たら	suffix	ようたら	やったら
kansai-ben -たら	suffix	ゆうたら	いったら
kansai-ben -たり	suffix	うたり	ったり
kansai-ben -たり	suffix	おうたり	あったり
kansai-ben -たり	suffix	こうたり	かったり
kansai-ben -たり	suffix	ごうたり	がったり
kansai-ben -たり	suffix	そうたり	さったり
kansai-ben -たり	suffix	ぞうたり	ざったり
kansai-ben -たり	suffix	とうたり	たったり
kansai-ben -たり	suffix	どうたり	だったり
kansai-ben -たり	suffix	のうたり	なったり
kansai-ben -たり	suffix	ほうたり	はったり
kansai-ben -たり	suffix	ぼうたり	ばったり
kansai-ben -たり	suffix	もうたり	まったり
kansai-ben -たり	suffix	ろうたり	らったり
kansai-ben -たり	suffix	ようたり	やったり
kansai-ben -たり	suffix	ゆうたり	いったり
kansai-ben -く	suffix	う	く		-く
kansai-ben -く	suffix	こう	かく		-く
kansai-ben -く	suffix	ごう	がく		-く
kansai-ben -く	suffix	そう	さく		-く
kansai-ben -く	suffix	とう	たく		-く
kansai-ben -く	suffix	のう	なく		-く
kansai-ben -く	suffix	ぼう	ばく		-く
kansai-ben -く	suffix	もう	まく		-く
kansai-ben -く	suffix	ろう	らく		-く
kansai-ben -く	suffix	よう	よく		-く
kansai-ben -く	suffix	しゅう	しく		-く
kansai-ben adjective -て	suffix	うて	くて	-て	-て
kansai-ben adjective -て	suffix	こうて	かくて	-て	-て
kansai-ben adjective -て	suffix	ごうて	がくて	-て	-て
kansai-ben adjective -て	suffix	そうて	さくて	-て	-て
kansai-ben adjective -て	suffix	とうて	たくて	-て	-て
kansai-ben adjective -て	suffix	のうて	なくて	-て	-て
kansai-ben adjective -て	suffix	ぼうて	ばくて	-て	-て
kansai-ben adjective -て	suffix	もうて	まくて	-て	-て
kansai-ben adjective -て	suffix	ろうて	らくて	-て	-て
kansai-ben adjective -て	suffix	ようて	よくて	-て	-て
kansai-ben adjective -て	suffix	しゅうて	しくて	-て	-て
kansai-ben adjective negative	suffix	うない	くない	adj-i	adj-i
kansai-ben adjective negative	suffix	こうない	かくない	adj-i	adj-i
kansai-ben adjective negative	suffix	ごうない	がくない	adj-i	adj-i
kansai-ben adjective negative	suffix	そうない	さくない	adj-i	adj-i
kansai-ben adjective negative	suffix	とうない	たくない	adj-i	adj-i
kansai-ben adjective negative	suffix	のうない	なくない	adj-i	adj-i
kansai-ben adjective negative	suffix	ぼうない	ばくない	adj-i	adj-i
kansai-ben adjective negative	suffix	もうない	まくない	adj-i	adj-i
kansai-ben adjective negative	suffix	ろうない	らくない	adj-i	adj-i
kansai-ben adjective negative	suffix	ようない	よくない	adj-i	adj-i
kansai-ben adjective negative	suffix	しゅうない	しくない	adj-i	adj-i
"""
}
