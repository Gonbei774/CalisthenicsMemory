package io.github.gonbei774.calisthenicsmemory.util

import io.github.gonbei774.calisthenicsmemory.data.Exercise

/**
 * Search utility functions for ranking and filtering exercises
 */
object SearchUtils {

    /**
     * Search exercises with ranking by relevance (best to worst match)
     *
     * Ranking priorities:
     * 1. Exact match (highest)
     * 2. Starts with query (high)
     * 3. Word boundary match (medium)
     * 4. Contains substring (low)
     */
    fun searchExercises(exercises: List<Exercise>, query: String): List<Exercise> {
        if (query.isBlank()) return emptyList()

        val queryLower = query.lowercase()

        // Calculate relevance score for each exercise
        val scoredExercises = exercises.mapNotNull { exercise ->
            val nameLower = exercise.name.lowercase()
            val score = calculateRelevanceScore(nameLower, queryLower)

            if (score > 0) {
                ScoredExercise(exercise, score)
            } else {
                null
            }
        }

        // Sort by score (descending) then by name (ascending) for consistent ordering
        return scoredExercises
            .sortedWith(compareByDescending<ScoredExercise> { it.score }.thenBy { it.exercise.name })
            .map { it.exercise }
    }

    /**
     * Search hierarchical data (groups with exercises) with ranking by relevance
     */
    fun searchHierarchicalExercises(
        hierarchicalData: List<io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel.GroupWithExercises>,
        query: String
    ): List<io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel.GroupWithExercises> {
        if (query.isBlank()) return hierarchicalData

        val queryLower = query.lowercase()

        return hierarchicalData.map { group ->
            val filteredExercises = group.exercises.mapNotNull { exercise ->
                val nameLower = exercise.name.lowercase()
                val score = calculateRelevanceScore(nameLower, queryLower)

                if (score > 0) {
                    ScoredExercise(exercise, score)
                } else {
                    null
                }
            }.sortedWith(compareByDescending<ScoredExercise> { it.score }.thenBy { it.exercise.name })
             .map { it.exercise }

            io.github.gonbei774.calisthenicsmemory.viewmodel.TrainingViewModel.GroupWithExercises(
                groupName = group.groupName,
                exercises = filteredExercises
            )
        }.filter { it.exercises.isNotEmpty() }
    }

    /**
     * Calculate relevance score for an exercise name against a search query
     */
    private fun calculateRelevanceScore(exerciseName: String, query: String): Int {
        // Exact match - highest priority
        if (exerciseName == query) {
            return 1000
        }

        // Starts with query - high priority
        if (exerciseName.startsWith(query)) {
            return 800
        }

        // Word boundary match - medium priority
        // Check if query matches at word boundaries (after space, dash, underscore, etc.)
        val wordBoundaryRegex = Regex("\\b${Regex.escape(query)}\\b", RegexOption.IGNORE_CASE)
        if (wordBoundaryRegex.containsMatchIn(exerciseName)) {
            return 600
        }

        // Contains substring - low priority
        if (exerciseName.contains(query)) {
            return 400
        }

        return 0 // No match
    }

    /**
     * Data class to hold exercise with its relevance score
     */
    private data class ScoredExercise(
        val exercise: Exercise,
        val score: Int
    )
}
