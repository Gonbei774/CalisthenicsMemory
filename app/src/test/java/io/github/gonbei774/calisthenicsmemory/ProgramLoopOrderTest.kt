package io.github.gonbei774.calisthenicsmemory

import io.github.gonbei774.calisthenicsmemory.data.Exercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramWorkoutSet
import io.github.gonbei774.calisthenicsmemory.util.buildChallengeValueSets
import io.github.gonbei774.calisthenicsmemory.util.buildProgramValueSets
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ループ実行順の回帰テスト（GitHub Issue #18）。
 *
 * Loop×3 に A,B,C 各1セットを入れた場合、実行順は「ラウンド優先」でなければならない:
 *   round1: A,B,C / round2: A,B,C / round3: A,B,C
 * 過去のバグでは再構築時に「種目優先」(A×3, B×3, C×3) になっていた。
 */
class ProgramLoopOrderTest {

    private val loopId = 10L

    /** 種目A,B,C（すべて両側種目）と、3ラウンドのループを表す originalSets を構築する。 */
    private fun buildFixture(): Pair<List<Pair<ProgramExercise, Exercise>>, List<ProgramWorkoutSet>> {
        val exercises = (0..2).map { i ->
            val exId = (i + 1).toLong()
            val pe = ProgramExercise(
                id = exId,
                programId = 1L,
                exerciseId = exId,
                sortOrder = i,
                sets = 1,
                targetValue = 10,
                loopId = loopId
            )
            val ex = Exercise(id = exId, name = "Ex$i", type = "Reps")
            pe to ex
        }

        // 初期構築と同じ「ラウンド優先」順で originalSets を組む
        val originalSets = mutableListOf<ProgramWorkoutSet>()
        for (round in 1..3) {
            for (index in 0..2) {
                originalSets.add(
                    ProgramWorkoutSet(
                        exerciseIndex = index,
                        setNumber = 1,
                        side = null,
                        targetValue = 10,
                        intervalSeconds = 60,
                        loopId = loopId,
                        roundNumber = round,
                        totalRounds = 3
                    )
                )
            }
        }
        return exercises to originalSets
    }

    private val expectedRoundMajor = listOf(
        0 to 1, 1 to 1, 2 to 1,
        0 to 2, 1 to 2, 2 to 2,
        0 to 3, 1 to 3, 2 to 3
    )

    @Test
    fun buildProgramValueSets_keepsRoundMajorOrder() {
        val (exercises, originalSets) = buildFixture()
        val result = buildProgramValueSets(exercises, originalSets)
        val order = result.map { it.exerciseIndex to it.roundNumber }
        assertEquals(expectedRoundMajor, order)
    }

    @Test
    fun buildChallengeValueSets_keepsRoundMajorOrder() {
        val (exercises, originalSets) = buildFixture()
        val result = buildChallengeValueSets(exercises, originalSets)
        val order = result.map { it.exerciseIndex to it.roundNumber }
        assertEquals(expectedRoundMajor, order)
    }
}