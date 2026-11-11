package com.meongcare.app.domain.voice

import com.meongcare.app.domain.card.Symptom
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * 문장 모음(`src/test/resources/voice/symptom-sentences.tsv`)의 줄마다 [symptomsIn] 결과를 확인한다.
 * 기획 담당도 줄을 더할 수 있게 파일로 뒀다. 틀린 줄은 모아서 한 번에 보인다.
 */
class SymptomSentenceCorpusTest {

    private data class Row(val line: Int, val sentence: String, val expected: Set<Symptom>, val source: String)

    private fun rows(): List<Row> {
        val text = requireNotNull(javaClass.getResource(CORPUS)) { "$CORPUS 가 없다" }.readText()
        return text.lines().mapIndexedNotNull { index, raw ->
            if (raw.isBlank() || raw.startsWith("#")) return@mapIndexedNotNull null
            val cols = raw.split('\t')
            require(cols.size == 3) { "${index + 1}번 줄: 열이 3개가 아니다 (${cols.size}개): $raw" }
            val expected = cols[1].split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { name ->
                requireNotNull(Symptom.entries.find { it.name == name }) { "${index + 1}번 줄: 모르는 증상 이름 $name" }
            }.toSet()
            Row(index + 1, cols[0], expected, cols[2])
        }
    }

    @Test
    fun `문장 모음의 모든 줄이 기대 증상과 같다`() {
        val wrong = rows().mapNotNull { row ->
            val actual = symptomsIn(row.sentence)
            if (actual == row.expected) null
            else "${row.line}번 줄 \"${row.sentence}\" (${row.source}): 기대 ${row.expected.names()} / 실제 ${actual.names()}"
        }
        if (wrong.isNotEmpty()) fail("문장 모음과 다른 줄 ${wrong.size}개:\n" + wrong.joinToString("\n"))
    }

    @Test
    fun `문장 모음 파일이 비어 있지 않고 형식이 맞다`() {
        val rows = rows()
        assertTrue("문장이 너무 적다: ${rows.size}", rows.size >= 20)
        assertTrue("관찰 문장이 없다", rows.any { it.source.startsWith("관찰") })
        val covered = rows.flatMap { it.expected }.toSet()
        val missing = Symptom.entries.toSet() - covered
        assertTrue("문장이 없는 증상: $missing", missing.isEmpty())
        assertTrue("증상이 없는 문장(부정·오탐 사례)이 없다", rows.any { it.expected.isEmpty() })
    }

    private fun Set<Symptom>.names(): String = if (isEmpty()) "(없음)" else inPresetOrderNames()

    private fun Set<Symptom>.inPresetOrderNames(): String = sortedBy { it.ordinal }.joinToString(",") { it.name }

    private companion object {
        const val CORPUS = "/voice/symptom-sentences.tsv"
    }
}
