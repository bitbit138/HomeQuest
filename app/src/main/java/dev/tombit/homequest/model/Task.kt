package dev.tombit.homequest.model

import com.google.firebase.Timestamp
import dev.tombit.homequest.utilities.Constants

/**
 * Represents a household task/quest.
 * Schema source: Section 5.3 of architecture document.
 *
 * State machine: open → claimed → pending_verification → completed
 * Status values are defined in Constants.TaskStatus — never use raw strings.
 *
 * RULE: private constructor — all construction must go through Builder.
 */
data class Task private constructor(
    val taskId: String,
    val title: String,
    val description: String?,
    val status: String,
    val xpReward: Int,
    val coinReward: Int,
    val createdBy: String,
    val assignedTo: String?,
    val claimedBy: String?,
    val proofImageUrl: String?,
    val deadline: Timestamp?,
    val isRecurring: Boolean,
    val createdAt: Timestamp?,
    val completedAt: Timestamp?
) {
    class Builder(
        var taskId: String = "",
        var title: String = "",
        var description: String? = null,
        var status: String = Constants.TaskStatus.OPEN,
        var xpReward: Int = 0,
        var coinReward: Int = 0,
        var createdBy: String = "",
        var assignedTo: String? = null,
        var claimedBy: String? = null,
        var proofImageUrl: String? = null,
        var deadline: Timestamp? = null,
        var isRecurring: Boolean = false,
        var createdAt: Timestamp? = null,
        var completedAt: Timestamp? = null
    ) {
        fun taskId(id: String) = apply { this.taskId = id }
        fun title(title: String) = apply { this.title = title }
        fun description(desc: String?) = apply { this.description = desc }
        fun status(status: String) = apply { this.status = status }
        fun xpReward(xp: Int) = apply { this.xpReward = xp }
        fun coinReward(coins: Int) = apply { this.coinReward = coins }
        fun createdBy(uid: String) = apply { this.createdBy = uid }
        fun assignedTo(uid: String?) = apply { this.assignedTo = uid }
        fun claimedBy(uid: String?) = apply { this.claimedBy = uid }
        fun proofImageUrl(url: String?) = apply { this.proofImageUrl = url }
        fun deadline(ts: Timestamp?) = apply { this.deadline = ts }
        fun isRecurring(recurring: Boolean) = apply { this.isRecurring = recurring }
        fun createdAt(ts: Timestamp?) = apply { this.createdAt = ts }
        fun completedAt(ts: Timestamp?) = apply { this.completedAt = ts }

        fun build() = Task(taskId, title, description, status, xpReward, coinReward,
            createdBy, assignedTo, claimedBy, proofImageUrl, deadline, isRecurring,
            createdAt, completedAt)
    }

    /** Returns true if the current user (by uid) can claim this task. Creator cannot claim own quest. */
    fun isClaimableBy(uid: String): Boolean {
        return status == Constants.TaskStatus.OPEN &&
            claimedBy == null &&
            createdBy != uid &&
            (assignedTo == null || assignedTo == uid)
    }

    /** Returns true if proof can be submitted (task is claimed by this user). */
    fun isProofSubmittableBy(uid: String): Boolean {
        return status == Constants.TaskStatus.CLAIMED && claimedBy == uid
    }

    fun toCreationMap(): Map<String, Any?> = mapOf(
        "taskId" to taskId,
        "title" to title,
        "description" to description,
        "status" to Constants.TaskStatus.OPEN,
        "xpReward" to xpReward,
        "coinReward" to coinReward,
        "createdBy" to createdBy,
        "assignedTo" to assignedTo,
        "claimedBy" to null,
        "proofImageUrl" to null,
        "deadline" to deadline,
        "isRecurring" to isRecurring,
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        "completedAt" to null
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Task {
            return Builder()
                .taskId(map["taskId"] as? String ?: "")
                .title(map["title"] as? String ?: "")
                .description(map["description"] as? String)
                .status(map["status"] as? String ?: Constants.TaskStatus.OPEN)
                .xpReward((map["xpReward"] as? Long)?.toInt() ?: 0)
                .coinReward((map["coinReward"] as? Long)?.toInt() ?: 0)
                .createdBy(map["createdBy"] as? String ?: "")
                .assignedTo(map["assignedTo"] as? String)
                .claimedBy(map["claimedBy"] as? String)
                .proofImageUrl(map["proofImageUrl"] as? String)
                .deadline(map["deadline"] as? Timestamp)
                .isRecurring(map["isRecurring"] as? Boolean ?: false)
                .createdAt(map["createdAt"] as? Timestamp)
                .completedAt(map["completedAt"] as? Timestamp)
                .build()
        }
    }
}
