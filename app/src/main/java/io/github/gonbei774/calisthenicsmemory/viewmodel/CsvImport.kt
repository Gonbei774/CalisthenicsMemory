package io.github.gonbei774.calisthenicsmemory.viewmodel

/**
 * CSVインポートのモード
 */
enum class CsvImportMode {
    MERGE      // 既存データに追加（マージ）
}

/**
 * CSV種類（型安全なenum）
 */
enum class CsvType {
    GROUPS,
    EXERCISES,
    RECORDS
}

/**
 * CSVインポートレポート用データクラス
 */
data class CsvImportReport(
    val type: CsvType,
    val successCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val skippedItems: List<String>,
    val errors: List<String>,
    val groupsCreatedCount: Int = 0  // 種目インポート時に自動作成されたグループ数
)