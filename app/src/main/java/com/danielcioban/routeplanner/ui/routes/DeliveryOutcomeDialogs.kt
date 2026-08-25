package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.StopFailureReason
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.RouteEta
import java.util.Calendar

@Composable
fun FailStopDialog(
    stopName: String,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.fail_stop_title),
        confirmLabel = null,
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.fail_stop_body, stopName),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        FailReason.entries.forEach { reason ->
            OutlinedButton(
                onClick = { onConfirm(reason.code) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(reason.labelRes))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleStopDialog(
    stopName: String,
    currentArriveByMinutes: Int?,
    onDismiss: () -> Unit,
    onEndOfRoute: () -> Unit,
    onPlusOneHour: () -> Unit,
    onPickArriveBy: (Int) -> Unit,
    onPickDateTime: (Long) -> Unit = {},
) {
    var showPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.reschedule_stop_title),
        confirmLabel = null,
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.reschedule_stop_body, stopName),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        OutlinedButton(
            onClick = onEndOfRoute,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.reschedule_end_of_route))
        }
        OutlinedButton(
            onClick = onPlusOneHour,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.reschedule_plus_one_hour))
        }
        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.reschedule_pick_arrive_by))
        }
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.reschedule_pick_date))
        }
    }
    if (showPicker) {
        val now = remember {
            val cal = Calendar.getInstance()
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        }
        val initial = currentArriveByMinutes ?: now
        val use24Hour = rememberUse24HourClock()
        val pickerState = rememberTimePickerState(
            initialHour = initial / 60,
            initialMinute = initial % 60,
            is24Hour = use24Hour,
        )
        IslandDialog(
            onDismissRequest = { showPicker = false },
            title = stringResource(R.string.stop_arrive_by),
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = {
                onPickArriveBy(pickerState.hour * 60 + pickerState.minute)
                showPicker = false
            },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {
            TimePicker(state = pickerState)
        }
    }
    if (showDatePicker) {
        ArriveByDateTimePicker(
            currentArriveByMinutes = currentArriveByMinutes,
            onDismiss = { showDatePicker = false },
            onConfirmEpoch = {
                onPickDateTime(it)
                showDatePicker = false
            },
        )
    }
}

private enum class FailReason(val code: String, val labelRes: Int) {
    NOT_HOME(StopFailureReason.NOT_HOME, R.string.fail_reason_not_home),
    REFUSED(StopFailureReason.REFUSED, R.string.fail_reason_refused),
    CLOSED(StopFailureReason.CLOSED, R.string.fail_reason_closed),
    OTHER(StopFailureReason.OTHER, R.string.fail_reason_other),
}

@Composable
fun FailEvidenceDialog(
    photoAttached: Boolean,
    signed: Boolean,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onSign: () -> Unit,
    onSkip: () -> Unit,
    onSave: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.fail_evidence_title),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = onSave,
        dismissLabel = stringResource(R.string.fail_evidence_skip),
        onDismissButton = onSkip,
    ) {
        Text(
            text = stringResource(R.string.fail_evidence_body),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        if (photoAttached) {
            Text(
                text = stringResource(R.string.fail_evidence_photo_attached),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (signed) {
            Text(
                text = stringResource(R.string.fail_evidence_signed),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        OutlinedButton(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.fail_evidence_photo))
        }
        OutlinedButton(
            onClick = onSign,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.fail_evidence_sign))
        }
    }
}

@Composable
fun DeferTasksDialog(
    stopName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var note by remember { mutableStateOf("") }
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.defer_tasks_title),
        confirmLabel = stringResource(R.string.nav_mark_done),
        onConfirm = { onConfirm(note) },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.defer_tasks_body, stopName),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        SoftOutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = stringResource(R.string.defer_tasks_note),
            singleLine = false,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun BulkCompleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.bulk_complete_title),
        confirmLabel = stringResource(R.string.nav_bulk_complete),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(
            text = stringResource(R.string.bulk_complete_body),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArriveByDateTimePicker(
    currentArriveByMinutes: Int?,
    onDismiss: () -> Unit,
    onConfirmEpoch: (Long) -> Unit,
) {
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
    )
    var pendingDateMs by remember { mutableStateOf<Long?>(null) }
    if (pendingDateMs == null) {
        IslandDialog(
            onDismissRequest = onDismiss,
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
        val initial = currentArriveByMinutes
            ?: RouteEta.minutesFromMidnight(System.currentTimeMillis())
        val use24Hour = rememberUse24HourClock()
        val timeState = rememberTimePickerState(
            initialHour = initial / 60,
            initialMinute = initial % 60,
            is24Hour = use24Hour,
        )
        IslandDialog(
            onDismissRequest = onDismiss,
            title = stringResource(R.string.stop_arrive_by),
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = {
                val minutes = timeState.hour * 60 + timeState.minute
                val day = Calendar.getInstance().apply { timeInMillis = pendingDateMs!! }
                onConfirmEpoch(
                    RouteEta.epochFromLocalDateTime(
                        day.get(Calendar.YEAR),
                        day.get(Calendar.MONTH),
                        day.get(Calendar.DAY_OF_MONTH),
                        minutes,
                    ),
                )
            },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {
            TimePicker(state = timeState)
        }
    }
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSave: (android.graphics.Bitmap) -> Unit,
) {
    val strokes = remember { mutableStateOf(listOf<List<Offset>>()) }
    var current by remember { mutableStateOf(listOf<Offset>()) }
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.fail_evidence_sign),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = {
            val image = androidx.compose.ui.graphics.ImageBitmap(600, 240)
            val canvas = androidx.compose.ui.graphics.Canvas(image)
            val paint = androidx.compose.ui.graphics.Paint().apply {
                color = Color.Black
                strokeWidth = 6f
                style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            }
            (strokes.value + listOf(current).filter { it.size > 1 }).forEach { stroke ->
                if (stroke.size < 2) return@forEach
                val path = Path()
                path.moveTo(stroke.first().x, stroke.first().y)
                stroke.drop(1).forEach { path.lineTo(it.x, it.y) }
                canvas.drawPath(path, paint)
            }
            onSave(image.asAndroidBitmap())
        },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> current = listOf(offset) },
                        onDragEnd = {
                            strokes.value = strokes.value + listOf(current)
                            current = emptyList()
                        },
                        onDrag = { change, _ ->
                            current = current + change.position
                        },
                    )
                },
        ) {
            val drawStroke = Stroke(width = 5f)
            fun drawPoints(points: List<Offset>) {
                if (points.size < 2) return
                val path = Path()
                path.moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { path.lineTo(it.x, it.y) }
                drawPath(path, Color.Black, style = drawStroke)
            }
            strokes.value.forEach { drawPoints(it) }
            drawPoints(current)
        }
    }
}
