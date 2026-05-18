package dev.tombit.homequest.utilities

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import java.lang.ref.WeakReference

/**
 * Central singleton for Firebase (Auth, Firestore with offline cache, Storage, Messaging).
 * Thread-safe double-checked locking; initialized from App only.
 */
class FirebaseManager private constructor(context: Context) {

    private val contextRef = WeakReference(context.applicationContext)

    // Firebase service references — initialize once, reuse everywhere
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance().also { db ->
        // Firestore offline persistence
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings
    }
    val storage: FirebaseStorage = FirebaseStorage.getInstance()
    val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()

    // ── Convenience helpers ────────────────────────────────────────────────

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUid: String?
        get() = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Refreshes the FCM token and writes it to users/{uid}.fcmToken.
     * Called from App.onCreate() each launch so tokens stay current.
     */
    fun refreshFcmToken() {
        val uid = currentUid ?: return
        messaging.token.addOnSuccessListener { token ->
            firestore
                .collection(Constants.Firestore.USERS_COLLECTION)
                .document(uid)
                .update("fcmToken", token)
        }
    }

    companion object {
        @Volatile
        private var instance: FirebaseManager? = null

        fun init(context: Context): FirebaseManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseManager(context).also { instance = it }
            }
        }

        fun getInstance(): FirebaseManager {
            return instance ?: throw IllegalStateException(
                "FirebaseManager must be initialized by calling init(context) before use."
            )
        }
    }
}
