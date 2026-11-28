package com.danielcioban.routeplanner.data.telemetry

import android.content.Context
import android.os.Bundle
import com.danielcioban.routeplanner.data.google.GoogleServices
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * The only place feature code should talk to Firebase telemetry. It deliberately
 * accepts no route names, addresses, coordinates, e-mails, or account IDs.
 */
class AppTelemetry(private val context: Context) {
    private var collectionEnabled = false

    fun applyConsent(usageAnalyticsEnabled: Boolean, crashReportsEnabled: Boolean) {
        if (!GoogleServices.isFirebaseReady(context)) return

        runCatching {
            FirebaseAnalytics.getInstance(context).apply {
                setAnalyticsCollectionEnabled(usageAnalyticsEnabled)
                setConsent(
                    mapOf(
                        FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to consent(usageAnalyticsEnabled),
                        FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
                        FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
                        FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
                    ),
                )
            }
            collectionEnabled = usageAnalyticsEnabled
        }
        runCatching {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(crashReportsEnabled)
        }
    }

    fun screenViewed(screenName: String) {
        if (!collectionEnabled || !GoogleServices.isFirebaseReady(context)) return
        runCatching {
            FirebaseAnalytics.getInstance(context).logEvent(
                FirebaseAnalytics.Event.SCREEN_VIEW,
                Bundle().apply { putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName) },
            )
        }
    }

    fun event(name: String) {
        if (!collectionEnabled || !GoogleServices.isFirebaseReady(context)) return
        runCatching { FirebaseAnalytics.getInstance(context).logEvent(name, null) }
    }

    private fun consent(enabled: Boolean): FirebaseAnalytics.ConsentStatus =
        if (enabled) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED
}
