package com.danielcioban.routeplanner.data.google

import android.content.Context
import com.danielcioban.routeplanner.BuildConfig

/**
 * Runtime gate for Firebase / Google Sign-In. The app compiles and runs with or
 * without `app/google-services.json` and without a web client id.
 *
 * Real keys stay out of git. See `docs/GOOGLE_SERVICES.md`.
 */
object GoogleServices {
    const val PLACEHOLDER_PROJECT = "routeplanner-placeholder"

    fun isPlaceholderValue(value: String): Boolean {
        val v = value.trim()
        if (v.isEmpty()) return true
        val lower = v.lowercase()
        return lower.contains("placeholder") ||
            lower.contains("your_") ||
            lower.contains("replace") ||
            lower.contains("xxxx") ||
            v.contains(PLACEHOLDER_PROJECT)
    }

    fun isPlaceholderGoogleServicesJson(text: String): Boolean {
        if (text.isBlank()) return true
        return text.contains(PLACEHOLDER_PROJECT) ||
            text.contains("YOUR_PROJECT_ID") ||
            text.contains("REPLACE_ME") ||
            text.contains("AIzaSyXXXXXXXX")
    }

    fun isFirebaseReady(context: Context): Boolean {
        if (BuildConfig.GOOGLE_SERVICES_JSON) {
            val appId = stringResourceOrEmpty(context, "google_app_id")
            return appId.isNotEmpty() && !isPlaceholderValue(appId)
        }
        return false
    }

    fun webClientId(context: Context): String {
        val fromBuild = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (fromBuild.isNotEmpty() && !isPlaceholderValue(fromBuild)) return fromBuild
        val fromPlugin = stringResourceOrEmpty(context, "default_web_client_id")
        if (fromPlugin.isNotEmpty() && !isPlaceholderValue(fromPlugin)) return fromPlugin
        return ""
    }

    fun isSignInAvailable(context: Context): Boolean = webClientId(context).isNotEmpty()

    private fun stringResourceOrEmpty(context: Context, name: String): String {
        val id = context.resources.getIdentifier(name, "string", context.packageName)
        if (id == 0) return ""
        return runCatching { context.getString(id) }.getOrDefault("").trim()
    }
}
