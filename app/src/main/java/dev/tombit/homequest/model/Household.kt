package dev.tombit.homequest.model

import com.google.firebase.Timestamp

/**
 * Represents a HomeQuest household group.
 * Schema source: Section 5.2 of architecture document.
 *
 * RULE: inviteCode must be globally unique — checked before write (Risk Register: HIGH).
 * RULE: private constructor — all construction must go through Builder.
 */
data class Household private constructor(
    val householdId: String,
    val name: String,
    val members: List<String>,
    val inviteCode: String,
    val createdAt: Timestamp?,
    val createdBy: String
) {
    class Builder(
        var householdId: String = "",
        var name: String = "",
        var members: List<String> = emptyList(),
        var inviteCode: String = "",
        var createdAt: Timestamp? = null,
        var createdBy: String = ""
    ) {
        fun householdId(id: String) = apply { this.householdId = id }
        fun name(name: String) = apply { this.name = name }
        fun members(members: List<String>) = apply { this.members = members }
        fun inviteCode(code: String) = apply { this.inviteCode = code }
        fun createdAt(ts: Timestamp?) = apply { this.createdAt = ts }
        fun createdBy(uid: String) = apply { this.createdBy = uid }

        fun build() = Household(householdId, name, members, inviteCode, createdAt, createdBy)
    }

    fun isMember(uid: String): Boolean = members.contains(uid)

    fun toCreationMap(): Map<String, Any?> = mapOf(
        "householdId" to householdId,
        "name" to name,
        "members" to members,
        "inviteCode" to inviteCode,
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        "createdBy" to createdBy
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Household {
            @Suppress("UNCHECKED_CAST")
            return Builder()
                .householdId(map["householdId"] as? String ?: "")
                .name(map["name"] as? String ?: "")
                .members((map["members"] as? List<String>) ?: emptyList())
                .inviteCode(map["inviteCode"] as? String ?: "")
                .createdAt(map["createdAt"] as? Timestamp)
                .createdBy(map["createdBy"] as? String ?: "")
                .build()
        }
    }
}
