package dev.tombit.homequest.utilities

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles incoming FCM messages and token refresh events.
 *
 * Token refresh: when Firebase rotates the FCM token, onNewToken() is called.
 * We write the new token to users/{uid}.fcmToken immediately (Risk Register: MED).
 *
 * Message handling: notifications sent by the Cloud Function (index.ts)
 * are display notifications — the system tray handles rendering automatically
 * when the app is in the background. onMessageReceived() is only invoked
 * when the app is in the foreground.
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
     * Called when a data message arrives while the app is in the foreground.
     * Notification messages (sent by Cloud Function) are handled by the system tray
     * when the app is backgrounded — no handling needed here for MVP.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Foreground notification handling — V2 feature (in-app banner via SignalManager)
        message.notification?.body?.let { body ->
            SignalManager.getInstance().toast(body)
        }
    }
}
