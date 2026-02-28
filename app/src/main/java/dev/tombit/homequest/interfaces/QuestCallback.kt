package dev.tombit.homequest.interfaces

import dev.tombit.homequest.model.Task

/**
 * Callback interface for quest/task list interactions.
 * RULE: Never use lambdas for cross-component communication — always a named interface.
 * Pattern: Professor's L07/L08 interface/callback pattern.
 */
interface QuestCallback {
    fun onQuestClaimed(task: Task, position: Int)
    fun onQuestCompleted(task: Task, position: Int)
    fun onQuestTapped(task: Task, position: Int)
}
