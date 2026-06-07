package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.viewmodel.csvEscape
import io.github.gonbei774.calisthenicsmemory.viewmodel.csvRow
import io.github.gonbei774.calisthenicsmemory.viewmodel.parseCsv
import io.github.gonbei774.calisthenicsmemory.viewmodel.parseCsvRecords
import org.junit.Assert.*
import org.junit.Test

/**
 * RFC 4180 準拠 CSV ヘルパー（CsvUtils.kt）の単体テスト。
 * Issue #93: カンマ・クォート・改行を含むフィールドの round-trip を保証する。
 */
class CsvUtilsTest {

    // ========================================
    // csvEscape
    // ========================================

    @Test
    fun csvEscape_plainText_noChange() {
        assertEquals("Wall Push-up", csvEscape("Wall Push-up"))
        assertEquals("", csvEscape(""))
        assertEquals("Dynamic", csvEscape("Dynamic"))
    }

    @Test
    fun csvEscape_withComma_isQuoted() {
        assertEquals(
            "\"Feet-Assisted Hang (18mm, Half-Crimp)\"",
            csvEscape("Feet-Assisted Hang (18mm, Half-Crimp)")
        )
    }

    @Test
    fun csvEscape_withDoubleQuote_isEscapedAndQuoted() {
        // He said "hi" -> "He said ""hi"""
        assertEquals("\"He said \"\"hi\"\"\"", csvEscape("He said \"hi\""))
    }

    @Test
    fun csvEscape_withNewline_isQuoted() {
        assertEquals("\"line1\nline2\"", csvEscape("line1\nline2"))
        assertEquals("\"line1\rline2\"", csvEscape("line1\rline2"))
    }

    // ========================================
    // csvRow
    // ========================================

    @Test
    fun csvRow_joinsAndEscapes() {
        assertEquals(
            "\"a, b\",c,\"d\"\"e\"",
            csvRow("a, b", "c", "d\"e")
        )
    }

    @Test
    fun csvRow_plainFields() {
        assertEquals("Wall Push-up,Dynamic,Step 1", csvRow("Wall Push-up", "Dynamic", "Step 1"))
    }

    // ========================================
    // parseCsv - basics
    // ========================================

    @Test
    fun parseCsv_simpleRow() {
        val result = parseCsv("a,b,c")
        assertEquals(1, result.size)
        assertEquals(listOf("a", "b", "c"), result[0])
    }

    @Test
    fun parseCsv_multipleRows() {
        val result = parseCsv("a,b\nc,d")
        assertEquals(2, result.size)
        assertEquals(listOf("a", "b"), result[0])
        assertEquals(listOf("c", "d"), result[1])
    }

    @Test
    fun parseCsv_emptyFields() {
        val result = parseCsv("a,,c")
        assertEquals(listOf("a", "", "c"), result[0])
    }

    @Test
    fun parseCsv_emptyString() {
        assertEquals(emptyList<List<String>>(), parseCsv(""))
    }

    @Test
    fun parseCsv_crlfLineEndings() {
        val result = parseCsv("a,b\r\nc,d")
        assertEquals(2, result.size)
        assertEquals(listOf("a", "b"), result[0])
        assertEquals(listOf("c", "d"), result[1])
    }

    // ========================================
    // parseCsv - quoting (the core of Issue #93)
    // ========================================

    @Test
    fun parseCsv_quotedFieldWithComma() {
        val result = parseCsv("\"Feet-Assisted Hang (18mm, Half-Crimp)\",Dynamic")
        assertEquals(listOf("Feet-Assisted Hang (18mm, Half-Crimp)", "Dynamic"), result[0])
    }

    @Test
    fun parseCsv_escapedDoubleQuoteInsideQuotedField() {
        // "He said ""hi""" -> He said "hi"
        val result = parseCsv("\"He said \"\"hi\"\"\",x")
        assertEquals(listOf("He said \"hi\"", "x"), result[0])
    }

    @Test
    fun parseCsv_quotedFieldWithEmbeddedNewline_staysOneRecord() {
        val result = parseCsv("\"line1\nline2\",b")
        assertEquals(1, result.size)
        assertEquals(listOf("line1\nline2", "b"), result[0])
    }

    @Test
    fun parseCsv_trailingEmptyField() {
        val result = parseCsv("a,b,")
        assertEquals(listOf("a", "b", ""), result[0])
    }

    // ========================================
    // parseCsv - backward compatibility (lenient)
    // ========================================

    @Test
    fun parseCsv_unquotedRow_matchesPlainSplit() {
        // 旧バージョンが吐いたクォートなし行は従来の split(",") と同じ結果になる
        val line = "Wall Push-up,Dynamic,Step 1 Pushups,1,Bilateral,3,50,false"
        assertEquals(line.split(","), parseCsv(line)[0])
    }

    @Test
    fun parseCsv_literalQuoteMidField_treatedAsLiteral() {
        // フィールド途中の " はリテラル（旧データのコメント中の " を誤読しない）
        val result = parseCsv("5'10\" reach,x")
        assertEquals(listOf("5'10\" reach", "x"), result[0])
    }

    // ========================================
    // parseCsvRecords - comment & blank filtering
    // ========================================

    @Test
    fun parseCsvRecords_skipsCommentAndBlankLines() {
        val csv = """
            # comment line
            name

            Step 1 Pushups
            # another, with comma
            Step 2 Squats
        """.trimIndent()

        val records = parseCsvRecords(csv)
        assertEquals(3, records.size)
        assertEquals("name", records[0][0])
        assertEquals("Step 1 Pushups", records[1][0])
        assertEquals("Step 2 Squats", records[2][0])
    }

    @Test
    fun parseCsvRecords_embeddedNewlineInComment_notSplit() {
        // コメント(8列目)にクォートされた改行 → 1レコードのまま保持
        val csv = "exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment\n" +
            "Pull-up,Dynamic,2025-11-09,10:00,1,5,,\"multi\nline note\""
        val records = parseCsvRecords(csv)
        assertEquals(2, records.size) // header + 1 data record
        assertEquals("multi\nline note", records[1][7])
    }
}
