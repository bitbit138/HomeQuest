package dev.tombit.homequest

import android.app.Application
import dev.tombit.homequest.utilities.FirebaseManager
import dev.tombit.homequest.utilities.ImageLoader
import dev.tombit.homequest.utilities.SharedPreferencesManager
import dev.tombit.homequest.utilities.SignalManager

/**
 * Application subclass — the ONLY place singletons are initialized.
 * Order matters: FirebaseManager first, then utilities.
 *
 * RULE: No singleton may call init() from an Activity. Only from here.
 * Must be registered in AndroidManifest.xml: android:name=".App"
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize all singletons in dependency order
        FirebaseManager.init(this)
        ImageLoader.init(this)
        SignalManager.init(this)
        SharedPreferencesManager.init(this)

        // Refresh FCM token on every launch to prevent stale tokens (Risk Register: MED)
        FirebaseManager.getInstance().refreshFcmToken()
    }
}
