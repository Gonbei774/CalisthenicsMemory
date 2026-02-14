package io.github.gonbei774.calisthenicsmemory.ui

/**
 * ViewModel から UI へのメッセージを表現する Sealed Class
 *
 * ViewModelはApplication Contextを使用するため、言語変更に追従しない問題がある。
 * このクラスを使用することで、メッセージの「意図」をViewModelからUIに渡し、
 * UI層（Activity/Compose）で現在の言語に応じた文字列リソースを取得できる。
 *
 * 利点:
 * - タイプセーフ: コンパイル時にエラー検出
 * - 言語追従: UI層で毎回リソースから取得するため、言語変更に即座に対応
 * - テスト容易: モックしやすい
 * - 拡張性: 将来的にアクション付きSnackbarにも対応可能
 */
sealed class UiMessage {

    // ===== Exercise 関連 =====

    /** 種目が追加された */
    object ExerciseAdded : UiMessage()

    /** 種目が更新された */
    object ExerciseUpdated : UiMessage()

    /** 種目が削除された */
    object ExerciseDeleted : UiMessage()

    /** 種目が既に存在する */
    object ExerciseAlreadyExists : UiMessage()

    /** 種目名とタイプの組み合わせが既に登録されている */
    data class AlreadyRegistered(val name: String, val type: String) : UiMessage()

    /** 種目名とタイプの組み合わせが既に使用されている */
    data class AlreadyInUse(val name: String, val type: String) : UiMessage()

    // ===== Record 関連 =====

    /** セットが記録された */
    data class SetsRecorded(val count: Int) : UiMessage()

    /** 記録が更新された */
    object RecordUpdated : UiMessage()

    /** 記録が削除された */
    object RecordDeleted : UiMessage()

    // ===== Group 関連 =====

    /** グループが作成された */
    object GroupCreated : UiMessage()

    /** グループがリネームされた */
    object GroupRenamed : UiMessage()

    /** グループが削除された */
    object GroupDeleted : UiMessage()

    /** グループが既に存在する */
    object GroupAlreadyExists : UiMessage()

    // ===== Export/Import 関連 =====

    /** エクスポート完了 */
    data class ExportComplete(
        val groupCount: Int,
        val exerciseCount: Int,
        val recordCount: Int
    ) : UiMessage()

    /** インポート完了 */
    data class ImportComplete(
        val groupCount: Int,
        val exerciseCount: Int,
        val recordCount: Int
    ) : UiMessage()

    /** エクスポートエラー */
    data class ExportError(val errorMessage: String) : UiMessage()

    /** インポートエラー */
    data class ImportError(val errorMessage: String) : UiMessage()

    // ===== CSV 関連 =====

    /** CSVエクスポート成功（グループ/種目） */
    data class CsvExportSuccess(val type: String, val count: Int) : UiMessage()

    /** CSVテンプレートがエクスポートされた */
    data class CsvTemplateExported(val exerciseCount: Int) : UiMessage()

    /** CSVが空 */
    object CsvEmpty : UiMessage()

    /** CSVインポート成功 */
    data class CsvImportSuccess(val successCount: Int) : UiMessage()

    /** CSVインポート部分的成功 */
    data class CsvImportPartial(val successCount: Int, val errorCount: Int) : UiMessage()

    // ===== バックアップ関連 =====

    /** バックアップ保存成功 */
    object BackupSaved : UiMessage()

    /** バックアップ失敗 */
    object BackupFailed : UiMessage()

    // ===== クリップボード =====

    /** クリップボードにコピーされた */
    object CopiedToClipboard : UiMessage()

    // ===== Program 関連 =====

    /** プログラムが複製された */
    object ProgramDuplicated : UiMessage()

    // ===== コミュニティシェア関連 =====

    /** コミュニティシェア エクスポート完了 */
    data class CommunityShareExportComplete(
        val exerciseCount: Int,
        val programCount: Int,
        val intervalProgramCount: Int
    ) : UiMessage()

    /** コミュニティシェア インポート完了 */
    data class CommunityShareImportComplete(
        val report: io.github.gonbei774.calisthenicsmemory.viewmodel.CommunityShareImportReport
    ) : UiMessage()

    /** コミュニティシェア インポートエラー */
    data class CommunityShareImportError(val errorMessage: String) : UiMessage()

    /** ファイル種別の誤り */
    data class WrongFileType(val detected: String, val expected: String) : UiMessage()

    // ===== エラー =====

    /** 一般的なエラー */
    object ErrorOccurred : UiMessage()
}
