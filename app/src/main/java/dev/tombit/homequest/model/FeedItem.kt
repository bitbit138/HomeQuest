package dev.tombit.homequest.model

import com.google.firebase.Timestamp
import dev.tombit.homequest.utilities.Constants

/**
 * Represents a single entry in the household activity feed.
 * Schema source: Section 5.5 of architecture document.
 *
 * Write-optimized denormalized log — message is pre-formatted for direct display.
 * RULE: Feed entries are IMMUTABLE after write (no update, no delete from client).
 * RULE: private constructor — all construction must go through Builder.
 */
data class FeedItem private constructor(
    val entryId: String,
    val type: String,
    val actorId: String,
    val actorName: String,
    val message: String,
    val relatedEntityId: String?,
    val timestamp: Timestamp?
) {
    class Builder(
        var entryId: String = "",
        var type: String = "",
        var actorId: String = "",
        var actorName: String = "",
        var message: String = "",
        var relatedEntityId: String? = null,
        var timestamp: Timestamp? = null
    ) {
        fun entryId(id: String) = apply { this.entryId = id }
        fun type(type: String) = apply { this.type = type }
        fun actorId(uid: String) = apply { this.actorId = uid }
        fun actorName(name: String) = apply { this.actorName = name }
        fun message(msg: String) = apply { this.message = msg }
        fun relatedEntityId(id: String?) = apply { this.relatedEntityId = id }
        fun timestamp(ts: Timestamp?) = apply { this.timestamp = ts }

        fun build() = FeedItem(entryId, type, actorId, actorName, message,
            relatedEntityId, timestamp)
    }

    /** Returns an emoji icon appropriate for this feed event type. */
    fun typeIcon(): String = when (type) {
        Constants.Feed.TYPE_TASK_COMPLETED -> "✅"
        Constants.Feed.TYPE_TASK_CREATED -> "📋"
        Constants.Feed.TYPE_COUPON_PURCHASED -> "🎟️"
        Constants.Feed.TYPE_COUPON_REDEEMED -> "✨"
        Constants.Feed.TYPE_LEVEL_UP -> "🆙"
        Constants.Feed.TYPE_MEMBER_JOINED -> "👋"
        else -> "📣"
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): FeedItem {
            return Builder()
                .entryId(map["entryId"] as? String ?: "")
                .type(map["type"] as? String ?: "")
                .actorId(map["actorId"] as? String ?: "")
                .actorName(map["actorName"] as? String ?: "")
                .message(map["message"] as? String ?: "")
                .relatedEntityId(map["relatedEntityId"] as? String)
                .timestamp(map["timestamp"] as? Timestamp)
                .build()
        }
    }
}
