package com.danielcioban.routeplanner.ui.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.ui.components.FloatingCircleButton
import com.danielcioban.routeplanner.ui.components.FloatingIsland
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.layout.AdaptiveSheetSlot
import com.danielcioban.routeplanner.ui.layout.AppPanes
import com.danielcioban.routeplanner.ui.map.MapViewMode
import com.danielcioban.routeplanner.ui.map.RouteMapBackdrop
import com.danielcioban.routeplanner.ui.menu.ScreenMenuButton
import com.danielcioban.routeplanner.ui.theme.IslandColors
import java.text.DateFormat
import java.util.Date

@Composable
fun FuelLogScreen(
    viewModel: FuelLogViewModel,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    val dateFormat = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMapBackdrop(
            stops = emptyList(),
            mapViewMode = MapViewMode.MAP,
            showStraightStopLinks = false,
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            val wide = AppPanes.isWide(maxWidth)
            Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FloatingCircleButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = IslandColors.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                ScreenMenuButton(onClick = onOpenMenu, embedded = false)
                Spacer(modifier = Modifier.width(12.dp))
                FloatingIsland(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = AppPanes.TitleMaxWidth),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = 16.dp,
                ) {
                    Text(
                        text = stringResource(R.string.fuel_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = IslandColors.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                FloatingCircleButton(onClick = { showAdd = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.fuel_add),
                        tint = IslandColors.onSurface,
                    )
                }
            }
            AdaptiveSheetSlot(
                wide = wide,
                maxExpandedHeight = 520.dp,
                contentPadding = 8.dp,
                fillHeight = entries.isNotEmpty(),
            ) {
                if (entries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.fuel_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = IslandColors.onSurfaceMuted,
                        modifier = Modifier.padding(12.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            IslandListItem {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dateFormat.format(Date(entry.loggedAtEpochMs)),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = IslandColors.onSurface,
                                    )
                                    val bits = buildList {
                                        entry.odometerKm?.let { add("%.0f km".format(it)) }
                                        entry.liters?.let { add("%.1f L".format(it)) }
                                        entry.amount?.let { add("%.2f".format(it)) }
                                        if (entry.notes.isNotBlank()) add(entry.notes)
                                    }.joinToString(" · ")
                                    if (bits.isNotBlank()) {
                                        Text(
                                            text = bits,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = IslandColors.onSurfaceMuted,
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.delete(entry.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
        if (showAdd) {
            AddFuelDialog(
                onDismiss = { showAdd = false },
                onSave = { odo, liters, amount, notes ->
                    viewModel.add(odo, liters, amount, notes)
                    showAdd = false
                },
            )
        }
    }
}

@Composable
private fun AddFuelDialog(
    onDismiss: () -> Unit,
    onSave: (Double?, Double?, Double?, String) -> Unit,
) {
    var odo by remember { mutableStateOf("") }
    var liters by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.fuel_add),
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = {
            onSave(parseDecimal(odo), parseDecimal(liters), parseDecimal(amount), notes)
        },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        SoftOutlinedTextField(
            value = odo,
            onValueChange = { odo = it },
            label = stringResource(R.string.fuel_odometer),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = liters,
            onValueChange = { liters = it },
            label = stringResource(R.string.fuel_liters),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = stringResource(R.string.fuel_amount),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SoftOutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = stringResource(R.string.fuel_notes),
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun parseDecimal(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()
