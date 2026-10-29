package io.github.jennaslee.silverapp.guard

import io.github.jennaslee.silverapp.guard.IncomingMessage.Source
import io.github.jennaslee.silverapp.guard.RuleResult.Level
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * 한국어 스미싱 데이터셋으로 1차 규칙을 평가한다 (일반 테스트 실행에서는 데이터가 없으면 건너뜀).
 *
 * 데이터: https://huggingface.co/datasets/jmjmjm3/kor-smishing-message (CC BY-NC-SA 4.0, 저장소에 포함하지 않음)
 * 실행:
 *   SMISHING_CSV=<smishing.csv 경로> ./gradlew testDebugUnitTest --tests '*RulesDatasetEval*'
 *   (환경변수가 없으면 ../../.data/kor-smishing/smishing.csv 를 찾는다)
 * 결과: app/build/reports/rules-eval.txt
 *
 * 데이터에 보낸 사람 정보가 없으므로 모두 "연락처에 없는 국내 휴대폰 번호"로 가정한다.
 */
class RulesDatasetEval {

    private data class Sample(val content: String, val smishing: Boolean)

    @Test
    fun evaluate() {
        val csv = File(System.getenv("SMISHING_CSV") ?: "../../.data/kor-smishing/smishing.csv")
        assumeTrue("데이터셋 없음: ${csv.absolutePath}", csv.exists())

        val samples = parseCsv(csv.readText().removePrefix("﻿"))
            .drop(1)
            .mapNotNull { row ->
                val content = row.getOrNull(0)?.trim().orEmpty()
                val label = row.getOrNull(1)?.trim()
                when {
                    content.isEmpty() -> null
                    label == "스미싱" -> Sample(content, true)
                    label == "정상" -> Sample(content, false)
                    else -> null
                }
            }
            .distinctBy { it.content }

        val results = samples.map { s ->
            s to MessageRules.check(IncomingMessage(Source.SMS, "01012345678", s.content, senderInContacts = false))
        }

        val report = buildString {
            fun section(title: String) = appendLine("\n## $title")
            fun pct(n: Int, total: Int) = "%d (%.1f%%)".format(n, 100.0 * n / total)

            for (smishing in listOf(true, false)) {
                val group = results.filter { it.first.smishing == smishing }
                val byLevel = Level.entries.associateWith { lv -> group.count { it.second.level == lv } }
                section(if (smishing) "스미싱 ${group.size}건" else "정상 ${group.size}건")
                Level.entries.forEach { lv -> appendLine("- $lv: ${pct(byLevel.getValue(lv), group.size)}") }
                if (smishing) {
                    appendLine("→ 놓침(SAFE): ${pct(byLevel.getValue(Level.SAFE), group.size)}")
                } else {
                    val toServer = byLevel.getValue(Level.SUSPICIOUS) + byLevel.getValue(Level.HIGH_RISK)
                    appendLine("→ 서버로 보냄: ${pct(toServer, group.size)}, 서버 실패 시 잘못 경고(HIGH_RISK): ${pct(byLevel.getValue(Level.HIGH_RISK), group.size)}")
                }
            }

            section("규칙 항목별 적중 (스미싱 / 정상)")
            val hitNames = results.flatMap { r -> r.second.hits.map { it.substringBefore(':') + if (it.startsWith("keyword:")) ":" + it.substringAfter(':') else "" } }.toSet()
            hitNames.map { h ->
                val sm = results.count { it.first.smishing && it.second.hits.any { x -> x == h } }
                val no = results.count { !it.first.smishing && it.second.hits.any { x -> x == h } }
                Triple(h, sm, no)
            }.sortedByDescending { it.second + it.third }.take(40).forEach { (h, sm, no) ->
                appendLine("- $h: $sm / $no")
            }

            section("놓친 스미싱 예시 (점수 순, 60개)")
            results.filter { it.first.smishing && it.second.level == Level.SAFE }
                .sortedByDescending { it.second.score }
                .take(60)
                .forEach { (s, r) -> appendLine("- [${r.score}] ${s.content.oneLine(120)}") }

            section("정상인데 HIGH_RISK (40개)")
            results.filter { !it.first.smishing && it.second.level == Level.HIGH_RISK }
                .take(40)
                .forEach { (s, r) -> appendLine("- [${r.score} ${r.hits}] ${s.content.oneLine(120)}") }
        }

        val out = File("build/reports/rules-eval.txt")
        out.parentFile.mkdirs()
        out.writeText("# 1차 규칙 평가 (${samples.size}건, 중복 제외)\n$report")
        println(out.absolutePath)

        // 서버로 넘어가는 메시지(SUSPICIOUS 이상)를 규칙 결과와 함께 내보낸다.
        // 백엔드 scripts/eval-messages.mjs 가 이 파일로 실제 흐름과 같은 평가를 한다. 데이터셋 옆에 저장 (저장소 밖).
        val flagged = File(csv.parentFile, "rules-flagged.jsonl")
        flagged.writeText(
            results.filter { it.second.level != Level.SAFE }.joinToString("\n") { (s, r) ->
                buildJsonObject {
                    put("content", s.content)
                    put("smishing", s.smishing)
                    put("level", r.level.name)
                    put("hits", JsonArray(r.hits.map { JsonPrimitive(it) }))
                }.toString()
            },
        )
        println(flagged.absolutePath)
    }

    private fun String.oneLine(max: Int) = replace(Regex("\\s+"), " ").take(max)

    /** 따옴표 안 줄바꿈을 지원하는 간단한 CSV 파서 */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ } else quoted = false
                } else {
                    field.append(c)
                }
            } else when (c) {
                '"' -> quoted = true
                ',' -> { row.add(field.toString()); field.clear() }
                '\n' -> { row.add(field.toString()); field.clear(); rows.add(row); row = mutableListOf() }
                '\r' -> {}
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString()); rows.add(row) }
        return rows
    }
}
