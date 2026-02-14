package io.github.gonbei774.calisthenicsmemory.viewmodel

import kotlinx.serialization.Serializable

// ===== コミュニティ共有用シリアライゼーションモデル =====

@Serializable
data class CommunityShareData(
    val formatVersion: Int,
    val exportType: String,
    val exportDate: String,
    val exportId: String,
    val appVersion: String,
    val data: CommunityShareContent
)

@Serializable
data class CommunityShareContent(
    val groups: List<ShareGroup> = emptyList(),
    val exercises: List<ShareExercise> = emptyList(),
    val programs: List<ShareProgram> = emptyList(),
    val intervalPrograms: List<ShareIntervalProgram> = emptyList()
)

@Serializable
data class ShareGroup(
    val name: String
)

@Serializable
data class ShareExercise(
    val name: String,
    val type: String,
    val group: String? = null,
    val sortOrder: Int = 0,
    val laterality: String = "Bilateral",
    val targetSets: Int? = null,
    val targetValue: Int? = null,
    val restInterval: Int? = null,
    val repDuration: Int? = null,
    val distanceTrackingEnabled: Boolean = false,
    val weightTrackingEnabled: Boolean = false,
    val assistanceTrackingEnabled: Boolean = false,
    val description: String? = null
)

@Serializable
data class ShareProgram(
    val name: String,
    val exercises: List<ShareProgramExercise> = emptyList(),
    val loops: List<ShareProgramLoop> = emptyList()
)

@Serializable
data class ShareProgramExercise(
    val exerciseName: String,
    val exerciseType: String,
    val sortOrder: Int,
    val sets: Int = 1,
    val targetValue: Int = 0,
    val intervalSeconds: Int = 60,
    val loopId: Int? = null
)

@Serializable
data class ShareProgramLoop(
    val id: Int,
    val sortOrder: Int,
    val rounds: Int,
    val restBetweenRounds: Int
)

@Serializable
data class ShareIntervalProgram(
    val name: String,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val roundRestSeconds: Int,
    val exercises: List<ShareIntervalProgramExercise> = emptyList()
)

@Serializable
data class ShareIntervalProgramExercise(
    val exerciseName: String,
    val exerciseType: String,
    val sortOrder: Int
)

// ===== インポートレポート =====

data class CommunityShareImportReport(
    val groupsAdded: Int = 0,
    val groupsReused: Int = 0,
    val exercisesAdded: Int = 0,
    val exercisesSkipped: Int = 0,
    val programsAdded: Int = 0,
    val programsSkipped: Int = 0,
    val intervalProgramsAdded: Int = 0,
    val intervalProgramsSkipped: Int = 0,
    val errors: List<String> = emptyList()
)

// ===== バリデーション =====

private const val CURRENT_FORMAT_VERSION = 1
private const val MAX_NAME_LENGTH = 100
private const val MAX_DESCRIPTION_LENGTH = 60
private const val MAX_NUMERIC_VALUE = 999
private val VALID_EXERCISE_TYPES = setOf("Dynamic", "Isometric")
private val VALID_LATERALITY = setOf("Bilateral", "Unilateral")

fun validateCommunityShareContent(data: CommunityShareData): List<String> {
    val errors = mutableListOf<String>()

    // フォーマットバージョンチェック
    if (data.formatVersion > CURRENT_FORMAT_VERSION) {
        errors.add("Unsupported format version: ${data.formatVersion}")
        return errors
    }

    // exportType チェック
    if (data.exportType != "share") {
        errors.add("Invalid exportType: ${data.exportType}")
        return errors
    }

    val content = data.data

    // グループのバリデーション
    content.groups.forEachIndexed { index, group ->
        if (group.name.isBlank()) {
            errors.add("groups[$index]: name is empty")
        }
        if (group.name.length > MAX_NAME_LENGTH) {
            errors.add("groups[$index]: name exceeds $MAX_NAME_LENGTH characters")
        }
    }

    // 種目のバリデーション
    val exerciseKeys = mutableSetOf<String>()
    content.exercises.forEachIndexed { index, exercise ->
        if (exercise.name.isBlank()) {
            errors.add("exercises[$index]: name is empty")
        }
        if (exercise.name.length > MAX_NAME_LENGTH) {
            errors.add("exercises[$index]: name exceeds $MAX_NAME_LENGTH characters")
        }
        if (exercise.type !in VALID_EXERCISE_TYPES) {
            errors.add("exercises[$index]: invalid type '${exercise.type}'")
        }
        if (exercise.laterality !in VALID_LATERALITY) {
            errors.add("exercises[$index]: invalid laterality '${exercise.laterality}'")
        }
        exercise.targetSets?.let {
            if (it <= 0 || it > MAX_NUMERIC_VALUE) {
                errors.add("exercises[$index]: targetSets out of range (1-$MAX_NUMERIC_VALUE)")
            }
        }
        exercise.targetValue?.let {
            if (it <= 0 || it > MAX_NUMERIC_VALUE) {
                errors.add("exercises[$index]: targetValue out of range (1-$MAX_NUMERIC_VALUE)")
            }
        }
        exercise.restInterval?.let {
            if (it <= 0 || it > MAX_NUMERIC_VALUE) {
                errors.add("exercises[$index]: restInterval out of range (1-$MAX_NUMERIC_VALUE)")
            }
        }
        exercise.repDuration?.let {
            if (it <= 0 || it > MAX_NUMERIC_VALUE) {
                errors.add("exercises[$index]: repDuration out of range (1-$MAX_NUMERIC_VALUE)")
            }
        }
        // グループ参照チェック
        exercise.group?.let { groupName ->
            if (content.groups.none { it.name == groupName }) {
                errors.add("exercises[$index]: references unknown group '$groupName'")
            }
        }
        // 重複チェック（先勝ち）
        val key = "${exercise.name}|${exercise.type}"
        if (!exerciseKeys.add(key)) {
            errors.add("exercises[$index]: duplicate exercise '${exercise.name}' (${exercise.type})")
        }
    }

    // プログラムのバリデーション
    val programNames = mutableSetOf<String>()
    content.programs.forEachIndexed { pIndex, program ->
        if (program.name.isBlank()) {
            errors.add("programs[$pIndex]: name is empty")
        }
        if (program.name.length > MAX_NAME_LENGTH) {
            errors.add("programs[$pIndex]: name exceeds $MAX_NAME_LENGTH characters")
        }
        if (!programNames.add(program.name)) {
            errors.add("programs[$pIndex]: duplicate program name '${program.name}'")
        }

        // ループのバリデーション
        val loopIds = mutableSetOf<Int>()
        program.loops.forEachIndexed { lIndex, loop ->
            if (!loopIds.add(loop.id)) {
                errors.add("programs[$pIndex].loops[$lIndex]: duplicate loop id ${loop.id}")
            }
            if (loop.rounds <= 0 || loop.rounds > MAX_NUMERIC_VALUE) {
                errors.add("programs[$pIndex].loops[$lIndex]: rounds out of range (1-$MAX_NUMERIC_VALUE)")
            }
            if (loop.restBetweenRounds < 0 || loop.restBetweenRounds > MAX_NUMERIC_VALUE) {
                errors.add("programs[$pIndex].loops[$lIndex]: restBetweenRounds out of range (0-$MAX_NUMERIC_VALUE)")
            }
        }

        // プログラム内種目のバリデーション
        program.exercises.forEachIndexed { eIndex, pe ->
            val key = "${pe.exerciseName}|${pe.exerciseType}"
            if (key !in exerciseKeys) {
                errors.add("programs[$pIndex].exercises[$eIndex]: references unknown exercise '${pe.exerciseName}' (${pe.exerciseType})")
            }
            if (pe.sets <= 0 || pe.sets > MAX_NUMERIC_VALUE) {
                errors.add("programs[$pIndex].exercises[$eIndex]: sets out of range (1-$MAX_NUMERIC_VALUE)")
            }
            if (pe.targetValue < 0 || pe.targetValue > MAX_NUMERIC_VALUE) {
                errors.add("programs[$pIndex].exercises[$eIndex]: targetValue out of range (0-$MAX_NUMERIC_VALUE)")
            }
            if (pe.intervalSeconds < 0 || pe.intervalSeconds > MAX_NUMERIC_VALUE) {
                errors.add("programs[$pIndex].exercises[$eIndex]: intervalSeconds out of range (0-$MAX_NUMERIC_VALUE)")
            }
            pe.loopId?.let { lid ->
                if (lid !in loopIds) {
                    errors.add("programs[$pIndex].exercises[$eIndex]: references unknown loopId $lid")
                }
            }
        }
    }

    // インターバルプログラムのバリデーション
    val intervalNames = mutableSetOf<String>()
    content.intervalPrograms.forEachIndexed { iIndex, interval ->
        if (interval.name.isBlank()) {
            errors.add("intervalPrograms[$iIndex]: name is empty")
        }
        if (interval.name.length > MAX_NAME_LENGTH) {
            errors.add("intervalPrograms[$iIndex]: name exceeds $MAX_NAME_LENGTH characters")
        }
        if (!intervalNames.add(interval.name)) {
            errors.add("intervalPrograms[$iIndex]: duplicate interval program name '${interval.name}'")
        }
        if (interval.workSeconds <= 0 || interval.workSeconds > MAX_NUMERIC_VALUE) {
            errors.add("intervalPrograms[$iIndex]: workSeconds out of range (1-$MAX_NUMERIC_VALUE)")
        }
        if (interval.restSeconds < 0 || interval.restSeconds > MAX_NUMERIC_VALUE) {
            errors.add("intervalPrograms[$iIndex]: restSeconds out of range (0-$MAX_NUMERIC_VALUE)")
        }
        if (interval.rounds <= 0 || interval.rounds > MAX_NUMERIC_VALUE) {
            errors.add("intervalPrograms[$iIndex]: rounds out of range (1-$MAX_NUMERIC_VALUE)")
        }
        if (interval.roundRestSeconds < 0 || interval.roundRestSeconds > MAX_NUMERIC_VALUE) {
            errors.add("intervalPrograms[$iIndex]: roundRestSeconds out of range (0-$MAX_NUMERIC_VALUE)")
        }

        // インターバル内種目のバリデーション
        interval.exercises.forEachIndexed { eIndex, ie ->
            val key = "${ie.exerciseName}|${ie.exerciseType}"
            if (key !in exerciseKeys) {
                errors.add("intervalPrograms[$iIndex].exercises[$eIndex]: references unknown exercise '${ie.exerciseName}' (${ie.exerciseType})")
            }
        }
    }

    return errors
}
