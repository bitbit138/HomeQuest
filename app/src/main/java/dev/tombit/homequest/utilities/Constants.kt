package dev.tombit.homequest.utilities

/**
 * All application constants grouped into nested objects.
 * No string/int literals are permitted in any Activity, Adapter, or Manager class.
 * Add to the appropriate nested object; never create loose top-level constants.
 */
class Constants {

    object Firestore {
        const val USERS_COLLECTION: String = "users"
        const val HOUSEHOLDS_COLLECTION: String = "households"
        const val TASKS_SUB_COLLECTION: String = "tasks"
        const val COUPONS_SUB_COLLECTION: String = "coupons"
        const val FEED_SUB_COLLECTION: String = "activity_feed"
    }

    object SP_KEYS {
        const val USER_PREFS: String = "USER_PREFS"
        const val LAST_HOUSEHOLD_ID: String = "LAST_HOUSEHOLD_ID"
        const val CURRENT_USER_JSON: String = "CURRENT_USER_JSON"
    }

    object TaskStatus {
        const val OPEN: String = "open"
        const val CLAIMED: String = "claimed"
        const val PENDING_VERIFICATION: String = "pending_verification"
        const val COMPLETED: String = "completed"
    }

    object Feed {
        const val PAGE_SIZE: Int = 20
        const val MAX_ENTRIES: Int = 500

        // Feed event type strings — must match Cloud Function constants exactly
        const val TYPE_TASK_COMPLETED: String = "task_completed"
        const val TYPE_TASK_CREATED: String = "task_created"
        const val TYPE_COUPON_PURCHASED: String = "coupon_purchased"
        const val TYPE_COUPON_REDEEMED: String = "coupon_redeemed"
        const val TYPE_LEVEL_UP: String = "level_up"
        const val TYPE_MEMBER_JOINED: String = "member_joined"
    }

    object Economy {
        const val MAX_XP_REWARD: Int = 500
        const val MIN_XP_REWARD: Int = 10
        const val MAX_COIN_REWARD: Int = 200
        const val MIN_COIN_REWARD: Int = 5
        const val LEADERBOARD_CACHE_MS: Long = 60_000L // 60 seconds
    }

    object Storage {
        const val PROOFS_PATH: String = "proofs"
        const val AVATARS_PATH: String = "avatars"
        const val PROOF_FILE_EXTENSION: String = ".jpg"
        const val AVATAR_FILE_EXTENSION: String = ".jpg"
        const val MAX_IMAGE_DIMENSION: Int = 1280
        const val JPEG_QUALITY_PRIMARY: Int = 80
        const val JPEG_QUALITY_FALLBACK: Int = 60
        const val MAX_FILE_SIZE_BYTES: Int = 200 * 1024 // 200 KB
    }

    object Household {
        const val INVITE_CODE_LENGTH: Int = 6
        const val INVITE_CODE_CHARS: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        const val MAX_NAME_LENGTH: Int = 50
    }

    object User {
        const val MAX_DISPLAY_NAME_LENGTH: Int = 32
        const val DEFAULT_LEVEL: Int = 1
        const val DEFAULT_XP: Int = 0
        const val DEFAULT_COIN_BALANCE: Int = 0
    }

    object Task {
        const val MAX_TITLE_LENGTH: Int = 80
        const val MAX_DESCRIPTION_LENGTH: Int = 500
    }

    object Coupon {
        const val MAX_TITLE_LENGTH: Int = 60
    }

    object XpThresholds {
        // Index = level, value = total XP required to reach that level
        val THRESHOLDS: IntArray = intArrayOf(
            0,      // Level 1
            200,    // Level 2
            500,    // Level 3
            1_000,  // Level 4
            2_000,  // Level 5
            3_500,  // Level 6
            6_000,  // Level 7
            9_000,  // Level 8
            13_000, // Level 9
            18_000  // Level 10 (max)
        )
        const val MAX_LEVEL: Int = 10
    }
}
