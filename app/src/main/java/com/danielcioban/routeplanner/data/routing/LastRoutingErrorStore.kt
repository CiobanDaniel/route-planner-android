package com.danielcioban.routeplanner.data.routing

import android.app.Application
import android.content.Context

/**
 * Last OSRM failure for the “report a problem” email.
 * Recorded only when every host in a request fails. A later successful
 * route or table call clears it. Survives process death via prefs.
 */
object LastRoutingErrorStore {
    data class Snapshot(
        val summary: String,
        val host: String,
        val atEpochMs: Long,
    )

    private const val PREFS = "routing_diagnostics"
    private const val KEY_SUMMARY = "last_summary"
    private const val KEY_HOST = "last_host"
    private const val KEY_AT = "last_at"

    @Volatile
    private var memory: Snapshot? = null

    @Volatile
    private var app: Application? = null

    fun install(application: Application) {
        app = application
        memory = load(application)
    }

    fun snapshot(): Snapshot? = memory

    fun record(summary: String, host: String = "") {
        val trimmed = summary.trim().ifBlank { "routing_failed" }.take(240)
        val hostTrim = host.trim().take(200)
        val item = Snapshot(trimmed, hostTrim, System.currentTimeMillis())
        memory = item
        app?.let { persist(it, item) }
    }

    fun clear() {
        memory = null
        app?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
    }

    private fun persist(context: Context, item: Snapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_SUMMARY, item.summary)
            .putString(KEY_HOST, item.host)
            .putLong(KEY_AT, item.atEpochMs)
            .apply()
    }

    private fun load(context: Context): Snapshot? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val summary = prefs.getString(KEY_SUMMARY, null)?.trim().orEmpty()
        if (summary.isEmpty()) return null
        val at = prefs.getLong(KEY_AT, 0L)
        if (at <= 0L) return null
        return Snapshot(
            summary = summary,
            host = prefs.getString(KEY_HOST, "").orEmpty(),
            atEpochMs = at,
        )
    }
}
