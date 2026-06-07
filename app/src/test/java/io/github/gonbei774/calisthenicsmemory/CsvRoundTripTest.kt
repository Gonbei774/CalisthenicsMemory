package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.viewmodel.csvRow
import io.github.gonbei774.calisthenicsmemory.viewmodel.parseCsvRecords
import org.junit.Assert.*
import org.junit.Test

/**
 * エクスポート（csvRow）→ インポート（parseCsvRecords）の round-trip テスト。
 * Issue #93: カンマ・クォート・改行を含むフィールドが正しく復元されることを保証する。
 *
 * 実際の export/import 関数は ViewModel/DAO 依存のため、ここでは export 側が使う csvRow と
 * import 側が使う parseCsvRecords を直接組み合わせて round-trip を検証する。
 */
class CsvRoundTripTest {

    @Test
    fun roundTrip_exerciseNameWithComma() {
        // Issue #93 の再現ケース
        val name = "Feet-Assisted Hang (18mm, Half-Crimp)"
        val header = "name,type,group,sortOrder,laterality,targetSets,targetValue,isFavorite"
        val row = csvRow(name, "Dynamic", "", "0", "Bilateral", "", "", "false")

        val csv = "$header\n$row"
        val records = parseCsvRecords(csv)

        assertEquals(2, records.size) // header + 1 data record
        val data = records[1]
        // 1列目がカンマで分割されず元の名前のまま復元される
        assertEquals(name, data[0])
        assertEquals("Dynamic", data[1])
    }

    @Test
    fun roundTrip_recordWithCommaInExerciseNameAndComment() {
        val name = "Feet-Assisted Hang (18mm, Half-Crimp)"
        val comment = "felt strong, kept tension"
        val header = "exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment,distanceCm,weightG,assistanceG"
        val row = csvRow(name, "Isometric", "2025-11-09", "10:00", "1", "30", "", comment, "", "", "")

        val csv = "$header\n$row"
        val records = parseCsvRecords(csv)

        val data = records[1]
        assertEquals(name, data[0])
        assertEquals("Isometric", data[1])
        assertEquals("2025-11-09", data[2])
        assertEquals(comment, data[7])
    }

    @Test
    fun roundTrip_commentWithNewline() {
        val comment = "set 1: good\nset 2: failed"
        val header = "exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment"
        val row = csvRow("Pull-up", "Dynamic", "2025-11-09", "10:00", "1", "5", "", comment)

        val csv = "$header\n$row"
        val records = parseCsvRecords(csv)

        assertEquals(2, records.size) // 改行を含んでも 1 データレコード
        assertEquals(comment, records[1][7])
    }

    @Test
    fun roundTrip_commentWithDoubleQuote() {
        val comment = "reached the \"high\" hold"
        val header = "exerciseName,exerciseType,date,time,setNumber,valueRight,valueLeft,comment"
        val row = csvRow("Front Lever", "Isometric", "2025-11-09", "10:00", "1", "10", "", comment)

        val records = parseCsvRecords("$header\n$row")
        assertEquals(comment, records[1][7])
    }

    @Test
    fun roundTrip_plainValuesUnaffected() {
        // 特殊文字なしのフィールドはクォートされず、従来フォーマットと同一
        val header = "name"
        val row = csvRow("Step 1 Pushups")
        assertEquals("Step 1 Pushups", row) // クォートされない

        val records = parseCsvRecords("$header\n$row")
        assertEquals("Step 1 Pushups", records[1][0])
    }
}
