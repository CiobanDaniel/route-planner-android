package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.RoutePlannerApplication
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.util.GeoUtils
import com.danielcioban.routeplanner.util.RouteEta

@Composable
internal fun rememberUse24HourClock(): Boolean {
    val context = LocalContext.current
    val app = context.applicationContext as RoutePlannerApplication
    val settings by app.settingsRepository.settings.collectAsState(initial = app.latestSettings)
    return settings.clockFormat.is24Hour(context)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArriveByControls(
    arriveByMinutes: Int?,
    serviceMinutes: Int,
    enabled: Boolean,
    onArriveByChange: (Int?) -> Unit,
    onServiceMinutesChange: (Int) -> Unit,
    resetKey: Any = Unit,
    arriveByEpochMs: Long? = null,
    onArriveByEpochChange: (Long?) -> Unit = {},
) {
    var showTimePicker by remember(resetKey) { mutableStateOf(false) }
    var showDatePicker by remember(resetKey) { mutableStateOf(false) }
    var serviceText by remember(resetKey) {
        mutableStateOf(if (serviceMinutes > 0) serviceMinutes.toString() else "")
    }
    val use24Hour = rememberUse24HourClock()
    val label = when {
        arriveByEpochMs != null -> stringResource(
            R.string.stop_arrive_by_date,
            GeoUtils.formatDateTime(arriveByEpochMs, use24Hour),
        )
        arriveByMinutes != null -> stringResource(
            R.string.stop_arrive_by_set,
            GeoUtils.formatClockMinutes(arriveByMinutes, use24Hour),
        )
        else -> stringResource(R.string.stop_arrive_by_none)
    }
    TextButton(
        onClick = { if (enabled) showTimePicker = true },
        enabled = enabled,
    ) {
        Text(text = label)
    }
    if (enabled) {
        TextButton(onClick = { showDatePicker = true }) {
            Text(stringResource(R.string.reschedule_pick_date))
        }
    }
    if ((arriveByMinutes != null || arriveByEpochMs != null) && enabled) {
        TextButton(
            onClick = {
                onArriveByChange(null)
                onArriveByEpochChange(null)
            },
        ) {
            Text(stringResource(R.string.stop_arrive_by_clear))
        }
    }
    SoftOutlinedTextField(
        value = serviceText,
        onValueChange = { value ->
            val digits = value.filter { it.isDigit() }.take(3)
            serviceText = digits
            onServiceMinutesChange(digits.toIntOrNull()?.coerceIn(0, 180) ?: 0)
        },
        label = stringResource(R.string.stop_service_minutes),
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = (arriveByMinutes ?: 12) / 60,
            initialMinute = (arriveByMinutes ?: 0) % 60,
            is24Hour = use24Hour,
        )
        IslandDialog(
            onDismissRequest = { showTimePicker = false },
            title = stringResource(R.string.stop_arrive_by),
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = {
                val minutes = pickerState.hour * 60 + pickerState.minute
                onArriveByChange(minutes)
                arriveByEpochMs?.let { epoch ->
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = epoch }
                    onArriveByEpochChange(
                        RouteEta.epochFromLocalDateTime(
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH),
                            cal.get(java.util.Calendar.DAY_OF_MONTH),
                            minutes,
                        ),
                    )
                }
                showTimePicker = false
            },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {
            TimePicker(state = pickerState)
        }
    }
    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = arriveByEpochMs ?: System.currentTimeMillis(),
        )
        var pendingDateMs by remember { mutableStateOf<Long?>(null) }
        if (pendingDateMs == null) {
            IslandDialog(
                onDismissRequest = { showDatePicker = false },
                title = stringResource(R.string.reschedule_pick_date),
                confirmLabel = stringResource(R.string.action_save),
                onConfirm = {
                    pendingDateMs = dateState.selectedDateMillis ?: System.currentTimeMillis()
                },
                dismissLabel = stringResource(R.string.action_cancel),
            ) {
                DatePicker(state = dateState)
            }
        } else {
            val initial = arriveByMinutes ?: RouteEta.minutesFromMidnight(System.currentTimeMillis())
            val timeState = rememberTimePickerState(
                initialHour = initial / 60,
                initialMinute = initial % 60,
                is24Hour = use24Hour,
            )
            IslandDialog(
                onDismissRequest = { showDatePicker = false },
                title = stringResource(R.string.stop_arrive_by),
                confirmLabel = stringResource(R.string.action_save),
                onConfirm = {
                    val minutes = timeState.hour * 60 + timeState.minute
                    val day = java.util.Calendar.getInstance().apply {
                        timeInMillis = pendingDateMs!!
                    }
                    val epoch = RouteEta.epochFromLocalDateTime(
                        day.get(java.util.Calendar.YEAR),
                        day.get(java.util.Calendar.MONTH),
                        day.get(java.util.Calendar.DAY_OF_MONTH),
                        minutes,
                    )
                    onArriveByChange(minutes)
                    onArriveByEpochChange(epoch)
                    showDatePicker = false
                },
                dismissLabel = stringResource(R.string.action_cancel),
            ) {
                TimePicker(state = timeState)
            }
        }
    }
}
