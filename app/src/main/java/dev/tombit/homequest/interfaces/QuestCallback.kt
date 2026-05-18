package dev.tombit.homequest.interfaces

import dev.tombit.homequest.model.Task

/**
 * Quest list row callbacks (claim / complete / tap).
 */
interface QuestCallback {
    fun onQuestClaimed(task: Task, position: Int)
    fun onQuestCompleted(task: Task, position: Int)
    fun onQuestTapped(task: Task, position: Int)
}
