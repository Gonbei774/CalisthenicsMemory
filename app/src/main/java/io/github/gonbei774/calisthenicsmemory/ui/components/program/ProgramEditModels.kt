package io.github.gonbei774.calisthenicsmemory.ui.components.program

import io.github.gonbei774.calisthenicsmemory.data.ProgramExercise
import io.github.gonbei774.calisthenicsmemory.data.ProgramLoop

/**
 * Sealed class to represent items in the program list.
 * Used for reorderable list in ProgramEditScreen.
 */
sealed class ProgramListItem {
    abstract val sortOrder: Int

    data class ExerciseItem(val programExercise: ProgramExercise) : ProgramListItem() {
        override val sortOrder: Int get() = programExercise.sortOrder
    }

    data class LoopItem(val loop: ProgramLoop) : ProgramListItem() {
        override val sortOrder: Int get() = loop.sortOrder
    }
}