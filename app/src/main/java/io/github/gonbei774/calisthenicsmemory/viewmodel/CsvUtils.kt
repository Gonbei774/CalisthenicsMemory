package io.github.gonbei774.calisthenicsmemory.viewmodel

/**
 * RFC 4180 準拠の CSV ヘルパー（Android 非依存の純粋関数）。
 *
 * - エクスポート: カンマ・ダブルクォート・改行を含むフィールドのみクォートし、
 *   内部のダブルクォートは "" にエスケープする。
 * - インポート: クォートを考慮してフィールドを分割し、クォート内のカンマ・改行・
 *   "" エスケープを正しく扱う。
 *
 * 後方互換のため「寛容（lenient）」にパースする:
 * クォートはフィールド先頭にある場合のみ開始扱いとし、フィールド途中に現れた " は
 * リテラル文字として扱う。これにより、旧バージョンが出力したクォートなし CSV
 * （コメント中の " などを含む）も誤読せずに読める。
 */

/**
 * フィールドを RFC 4180 でエスケープする（特殊文字を含む場合のみクォート）。
 */
fun csvEscape(field: String): String =
    if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + field.replace("\"", "\"\"") + "\""
    } else {
        field
    }

/**
 * 複数フィールドを 1 行の CSV 文字列にまとめる（各フィールドをエスケープ）。
 */
fun csvRow(vararg fields: String): String = fields.joinToString(",") { csvEscape(it) }

/**
 * CSV 文字列全体をレコード（= フィールドのリスト）の列へ分解する。
 *
 * クォート内のカンマ・改行・"" エスケープを処理する。レコード境界はクォート外の
 * 改行（\n / \r\n / \r）。空文字列は空のリストを返す。
 */
fun parseCsv(text: String): List<List<String>> {
    val records = mutableListOf<List<String>>()
    var fields = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    // 現在のフィールドがクォートで始まったか（途中の " をリテラル扱いするための判定用）
    var fieldStartedWithQuote = false
    var fieldIsEmpty = true  // まだ何も文字を積んでいない = フィールド先頭

    var i = 0
    val n = text.length

    fun endField() {
        fields.add(field.toString())
        field.setLength(0)
        inQuotes = false
        fieldStartedWithQuote = false
        fieldIsEmpty = true
    }

    fun endRecord() {
        endField()
        records.add(fields)
        fields = mutableListOf()
    }

    while (i < n) {
        val c = text[i]

        if (inQuotes) {
            if (c == '"') {
                // "" はエスケープされたダブルクォート、それ以外はクォート終了
                if (i + 1 < n && text[i + 1] == '"') {
                    field.append('"')
                    i += 2
                    continue
                } else {
                    inQuotes = false
                    i++
                    continue
                }
            } else {
                field.append(c)
                i++
                continue
            }
        }

        when (c) {
            '"' -> {
                if (fieldIsEmpty) {
                    // フィールド先頭の " のみクォート開始扱い
                    inQuotes = true
                    fieldStartedWithQuote = true
                    fieldIsEmpty = false
                } else {
                    // 途中の " はリテラル（寛容パース）
                    field.append(c)
                }
                i++
            }
            ',' -> {
                endField()
                i++
            }
            '\r' -> {
                // \r\n は 1 つの改行として扱う
                endRecord()
                if (i + 1 < n && text[i + 1] == '\n') i += 2 else i++
            }
            '\n' -> {
                endRecord()
                i++
            }
            else -> {
                field.append(c)
                fieldIsEmpty = false
                i++
            }
        }
    }

    // 末尾の残り（最後の改行がない場合）
    if (field.isNotEmpty() || fields.isNotEmpty() || fieldStartedWithQuote) {
        endRecord()
    }

    return records
}

/**
 * コメント行（# 始まり）と空行を除外したデータレコードを返す。
 *
 * 既存の `lines().filter { it.isNotBlank() && !it.startsWith("#") }` と意味的に一致させる。
 * 旧 CSV では「レコード = 物理行」なので挙動は同一。新 CSV ではクォート内改行を
 * 1 レコードとして保持できる。
 */
fun parseCsvRecords(text: String): List<List<String>> =
    parseCsv(text).filter { record ->
        // 実質空（フィールドが 1 つだけで空文字）のレコードを除外
        val isBlank = record.all { it.isBlank() }
        // 先頭フィールドが # で始まるコメントレコードを除外
        val isComment = record.firstOrNull()?.startsWith("#") == true
        !isBlank && !isComment
    }
