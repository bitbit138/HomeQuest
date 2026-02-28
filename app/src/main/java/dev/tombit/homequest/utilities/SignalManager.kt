package dev.tombit.homequest.utilities

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import java.lang.ref.WeakReference

/**
 * Singleton manager for all user feedback signals (Toast, vibration).
 * RULE: Never call Toast.makeText() directly anywhere in the app. Always use this class.
 * Pattern: Thread-safe double-check locking singleton (professor's L05 standard).
 */
class SignalManager private constructor(context: Context) {

    private val contextRef = WeakReference(context.applicationContext)

    enum class ToastLength(val value: Int) {
        SHORT(Toast.LENGTH_SHORT),
        LONG(Toast.LENGTH_LONG)
    }

    fun toast(message: String, length: ToastLength = ToastLength.SHORT) {
        contextRef.get()?.let { ctx ->
            Toast.makeText(ctx, message, length.value).show()
        }
    }

    fun vibrate(durationMs: Long = 100L) {
        contextRef.get()?.let { ctx ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    companion object {
        @Volatile
        private var instance: SignalManager? = null

        fun init(context: Context): SignalManager {
            return instance ?: synchronized(this) {
                instance ?: SignalManager(context).also { instance = it }
            }
        }

        fun getInstance(): SignalManager {
            return instance ?: throw IllegalStateException(
                "SignalManager must be initialized by calling init(context) before use."
            )
        }
    }
}
