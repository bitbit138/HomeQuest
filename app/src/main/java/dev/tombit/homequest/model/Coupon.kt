package dev.tombit.homequest.model

import com.google.firebase.Timestamp

/**
 * Represents a reward coupon in the household marketplace.
 * Schema source: Section 5.4 of architecture document.
 *
 * RULE: buyerId must only be set via Firestore Transaction (Section 7.1).
 *       Never write buyerId directly from the client.
 * RULE: private constructor — all construction must go through Builder.
 */
data class Coupon private constructor(
    val couponId: String,
    val title: String,
    val cost: Int,
    val sellerId: String,
    val buyerId: String?,
    val isRedeemed: Boolean,
    val createdAt: Timestamp?,
    val purchasedAt: Timestamp?
) {
    class Builder(
        var couponId: String = "",
        var title: String = "",
        var cost: Int = 0,
        var sellerId: String = "",
        var buyerId: String? = null,
        var isRedeemed: Boolean = false,
        var createdAt: Timestamp? = null,
        var purchasedAt: Timestamp? = null
    ) {
        fun couponId(id: String) = apply { this.couponId = id }
        fun title(title: String) = apply { this.title = title }
        fun cost(cost: Int) = apply { this.cost = cost }
        fun sellerId(uid: String) = apply { this.sellerId = uid }
        fun buyerId(uid: String?) = apply { this.buyerId = uid }
        fun isRedeemed(redeemed: Boolean) = apply { this.isRedeemed = redeemed }
        fun createdAt(ts: Timestamp?) = apply { this.createdAt = ts }
        fun purchasedAt(ts: Timestamp?) = apply { this.purchasedAt = ts }

        fun build() = Coupon(couponId, title, cost, sellerId, buyerId, isRedeemed,
            createdAt, purchasedAt)
    }

    val isAvailable: Boolean get() = buyerId == null
    val isPurchased: Boolean get() = buyerId != null
    val isPendingRedemption: Boolean get() = buyerId != null && !isRedeemed

    fun toCreationMap(): Map<String, Any?> = mapOf(
        "couponId" to couponId,
        "title" to title,
        "cost" to cost,
        "sellerId" to sellerId,
        "buyerId" to null,
        "isRedeemed" to false,
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        "purchasedAt" to null
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Coupon {
            return Builder()
                .couponId(map["couponId"] as? String ?: "")
                .title(map["title"] as? String ?: "")
                .cost((map["cost"] as? Long)?.toInt() ?: 0)
                .sellerId(map["sellerId"] as? String ?: "")
                .buyerId(map["buyerId"] as? String)
                .isRedeemed(map["isRedeemed"] as? Boolean ?: false)
                .createdAt(map["createdAt"] as? Timestamp)
                .purchasedAt(map["purchasedAt"] as? Timestamp)
                .build()
        }
    }
}
