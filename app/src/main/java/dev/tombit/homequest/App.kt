package dev.tombit.homequest

import android.app.Application
import dev.tombit.homequest.utilities.FirebaseManager
import dev.tombit.homequest.utilities.ImageLoader
import dev.tombit.homequest.utilities.SharedPreferencesManager
import dev.tombit.homequest.utilities.SignalManager

/**
 * Application entry: initializes shared singletons (FirebaseManager first, then helpers).
 * Registered in the manifest as android:name=".App"; avoid calling init from Activities.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize all singletons in dependency order
        FirebaseManager.init(this)
        ImageLoader.init(this)
        SignalManager.init(this)
        SharedPreferencesManager.init(this)

        // Refresh FCM token each launch so Firestore has a current device token
        FirebaseManager.getInstance().refreshFcmToken()
    }
}
