package dev.tombit.homequest.utilities

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import java.lang.ref.WeakReference

/**
 * Singleton SharedPreferences manager — V3 pattern from professor's L05.
 * Provides typed get/set helpers and JSON serialization via Gson.
 * Pattern: Thread-safe double-check locking singleton.
 */
class SharedPreferencesManager private constructor(context: Context) {

    private val contextRef = WeakReference(context.applicationContext)
    private val gson = Gson()

    private fun prefs(): SharedPreferences? {
        return contextRef.get()?.getSharedPreferences(
            Constants.SP_KEYS.USER_PREFS,
            Context.MODE_PRIVATE
        )
    }

    // ── Primitive helpers ──────────────────────────────────────────────────

    fun putString(key: String, value: String) {
        prefs()?.edit()?.putString(key, value)?.apply()
    }

    fun getString(key: String, default: String = ""): String {
        return prefs()?.getString(key, default) ?: default
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs()?.edit()?.putBoolean(key, value)?.apply()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return prefs()?.getBoolean(key, default) ?: default
    }

    fun remove(key: String) {
        prefs()?.edit()?.remove(key)?.apply()
    }

    fun clearAll() {
        prefs()?.edit()?.clear()?.apply()
    }

    // ── JSON object helpers ────────────────────────────────────────────────

    fun <T> putObject(key: String, obj: T) {
        putString(key, gson.toJson(obj))
    }

    fun <T> getObject(key: String, classOfT: Class<T>): T? {
        val json = getString(key)
        return if (json.isEmpty()) null else gson.fromJson(json, classOfT)
    }

    companion object {
        @Volatile
        private var instance: SharedPreferencesManager? = null

        fun init(context: Context): SharedPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: SharedPreferencesManager(context).also { instance = it }
            }
        }

        fun getInstance(): SharedPreferencesManager {
            return instance ?: throw IllegalStateException(
                "SharedPreferencesManager must be initialized by calling init(context) before use."
            )
        }
    }
}
