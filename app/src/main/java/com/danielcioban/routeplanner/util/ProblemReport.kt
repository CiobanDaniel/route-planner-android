package com.danielcioban.routeplanner.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.danielcioban.routeplanner.BuildConfig
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.routing.LastRoutingErrorStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Builds the “report a problem” mailto from app version + last OSRM error. */
object ProblemReport {
    data class DeviceInfo(
        val versionName: String,
        val versionCode: Int,
        val sdk: Int,
        val release: String,
        val manufacturer: String,
        val model: String,
    )

    fun body(
        device: DeviceInfo,
        error: LastRoutingErrorStore.Snapshot?,
        noneLabel: String,
    ): String = buildString {
        appendLine("Route Planner ${device.versionName} (${device.versionCode})")
        appendLine("Android ${device.release} (SDK ${device.sdk})")
        appendLine("${device.manufacturer} ${device.model}".trim())
        appendLine()
        appendLine("Last routing error:")
        if (error == null) {
            appendLine(noneLabel)
        } else {
            appendLine(error.summary)
            if (error.host.isNotBlank()) {
                appendLine("Host: ${error.host}")
            }
            appendLine("When (UTC): ${formatUtc(error.atEpochMs)}")
        }
        appendLine()
        appendLine("(Describe what you were doing.)")
    }

    fun start(context: Context) {
        val info = DeviceInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            sdk = android.os.Build.VERSION.SDK_INT,
            release = android.os.Build.VERSION.RELEASE,
            manufacturer = android.os.Build.MANUFACTURER,
            model = android.os.Build.MODEL,
        )
        val body = body(
            device = info,
            error = LastRoutingErrorStore.snapshot(),
            noneLabel = context.getString(R.string.report_problem_none),
        )
        val to = BuildConfig.SUPPORT_EMAIL.trim()
        val send = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            if (to.isNotEmpty()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            }
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.report_problem_subject))
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            context.startActivity(
                Intent.createChooser(send, context.getString(R.string.report_problem_chooser)),
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.report_problem_no_mail, Toast.LENGTH_LONG).show()
        }
    }

    private fun formatUtc(epochMs: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(epochMs))
    }
}
