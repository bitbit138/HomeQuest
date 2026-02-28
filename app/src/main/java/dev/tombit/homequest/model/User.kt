package dev.tombit.homequest.model

import com.google.firebase.Timestamp
import dev.tombit.homequest.utilities.Constants

/**
 * Represents a HomeQuest user.
 * Schema source: Section 5.1 of architecture document.
 *
 * RULE: private constructor — all construction must go through Builder.
 * coinBalance, currentXp, and level are read-only on the client.
 * They are only written by Cloud Functions via Admin SDK.
 */
data class User private constructor(
    val uid: String,
    val displayName: String,
    val email: String,
    val householdId: String,
    val level: Int,
    val currentXp: Int,
    val coinBalance: Int,
    val fcmToken: String,
    val createdAt: Timestamp?,
    val avatarUrl: String?
) {
    class Builder(
        var uid: String = "",
        var displayName: String = "",
        var email: String = "",
        var householdId: String = "",
        var level: Int = Constants.User.DEFAULT_LEVEL,
        var currentXp: Int = Constants.User.DEFAULT_XP,
        var coinBalance: Int = Constants.User.DEFAULT_COIN_BALANCE,
        var fcmToken: String = "",
        var createdAt: Timestamp? = null,
        var avatarUrl: String? = null
    ) {
        fun uid(uid: String) = apply { this.uid = uid }
        fun displayName(name: String) = apply { this.displayName = name }
        fun email(email: String) = apply { this.email = email }
        fun householdId(id: String) = apply { this.householdId = id }
        fun level(level: Int) = apply { this.level = level }
        fun currentXp(xp: Int) = apply { this.currentXp = xp }
        fun coinBalance(coins: Int) = apply { this.coinBalance = coins }
        fun fcmToken(token: String) = apply { this.fcmToken = token }
        fun createdAt(ts: Timestamp?) = apply { this.createdAt = ts }
        fun avatarUrl(url: String?) = apply { this.avatarUrl = url }

        fun build() = User(uid, displayName, email, householdId, level, currentXp,
            coinBalance, fcmToken, createdAt, avatarUrl)
    }

    /**
     * Returns a Map suitable for writing to Firestore at account creation.
     * Does NOT include coinBalance/currentXp/level — those are set by Cloud Function.
     */
    fun toCreationMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "displayName" to displayName,
        "email" to email,
        "householdId" to householdId,
        "level" to Constants.User.DEFAULT_LEVEL,
        "currentXp" to Constants.User.DEFAULT_XP,
        "coinBalance" to Constants.User.DEFAULT_COIN_BALANCE,
        "fcmToken" to fcmToken,
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        "avatarUrl" to avatarUrl
    )

    companion object {
        /** Deserializes a Firestore document snapshot into a User. */
        fun fromMap(map: Map<String, Any?>): User {
            return Builder()
                .uid(map["uid"] as? String ?: "")
                .displayName(map["displayName"] as? String ?: "")
                .email(map["email"] as? String ?: "")
                .householdId(map["householdId"] as? String ?: "")
                .level((map["level"] as? Long)?.toInt() ?: Constants.User.DEFAULT_LEVEL)
                .currentXp((map["currentXp"] as? Long)?.toInt() ?: Constants.User.DEFAULT_XP)
                .coinBalance((map["coinBalance"] as? Long)?.toInt() ?: Constants.User.DEFAULT_COIN_BALANCE)
                .fcmToken(map["fcmToken"] as? String ?: "")
                .createdAt(map["createdAt"] as? Timestamp)
                .avatarUrl(map["avatarUrl"] as? String)
                .build()
        }
    }
}
