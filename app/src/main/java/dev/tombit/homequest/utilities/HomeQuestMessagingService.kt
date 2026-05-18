package dev.tombit.homequest.utilities

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Foreground FCM handling and token rotation.
 * Token updates are written to users/{uid}.fcmToken so the backend can target this device.
 * Background notification display is handled by the system; onMessageReceived runs when the app is in the foreground.
 */
class HomeQuestMessagingService : FirebaseMessagingService() {

    /**
     * Called when Firebase rotates the FCM registration token.
     * Writes the fresh token to Firestore so Cloud Functions can reach this device.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseManager.getInstance().currentUid ?: return
        FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.USERS_COLLECTION)
            .document(uid)
            .update("fcmToken", token)
    }

    /**
     * Foreground-only handling for notification payloads (system shows notifications when backgrounded).
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Optional: surface notification body while app is open
        message.notification?.body?.let { body ->
            SignalManager.getInstance().toast(body)
        }
    }
}
