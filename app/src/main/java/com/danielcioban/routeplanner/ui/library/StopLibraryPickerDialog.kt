package com.danielcioban.routeplanner.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.StopLibraryEntity
import com.danielcioban.routeplanner.ui.components.IslandDialog
import com.danielcioban.routeplanner.ui.components.IslandListItem
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.util.LibraryTags

@Composable
fun StopLibraryPickerDialog(
    stops: List<StopLibraryEntity>,
    onDismiss: () -> Unit,
    onPick: (StopLibraryEntity) -> Unit,
    onManageLibrary: (() -> Unit)? = null,
    alreadyOnRouteLibraryIds: Set<Long> = emptySet(),
) {
    val sections = remember(stops) { libraryPickerSections(stops) }
    IslandDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.library_pick_title),
        confirmLabel = stringResource(R.string.action_close),
        onConfirm = onDismiss,
        dismissLabel = null,
    ) {
        Text(
            text = stringResource(R.string.library_pick_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = IslandColors.onSurfaceMuted,
        )
        if (stops.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = IslandColors.onSurface,
            )
        } else {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sections.forEach { section ->
                    Text(
                        text = stringResource(section.titleRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = IslandColors.onSurfaceMuted,
                    )
                    section.stops.forEach { stop ->
                        LibraryPickerRow(
                            stop = stop,
                            alreadyOnRoute = stop.id in alreadyOnRouteLibraryIds,
                            onPick = {
                                onPick(stop)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
        if (onManageLibrary != null) {
            TextButton(
                onClick = {
                    onDismiss()
                    onManageLibrary()
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.library_manage))
            }
        }
    }
}

@Composable
private fun LibraryPickerRow(
    stop: StopLibraryEntity,
    alreadyOnRoute: Boolean,
    onPick: () -> Unit,
) {
    IslandListItem(onClick = onPick) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = stop.addressHint.ifBlank { LibraryTags.join(LibraryTags.parse(stop.tags)) }
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = IslandColors.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (alreadyOnRoute) {
                Text(
                    text = stringResource(R.string.library_already_linked_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        TextButton(onClick = onPick) {
            Text(
                stringResource(
                    if (alreadyOnRoute) {
                        R.string.library_open_on_route
                    } else {
                        R.string.library_add_to_route
                    },
                ),
            )
        }
    }
}

private data class PickerSection(
    val titleRes: Int,
    val stops: List<StopLibraryEntity>,
)

private fun libraryPickerSections(stops: List<StopLibraryEntity>): List<PickerSection> {
    val favorites = stops.filter { it.isFavorite }.sortedBy { it.name.lowercase() }
    val favoriteIds = favorites.map { it.id }.toSet()
    val recent = stops.filter { it.lastUsedAtEpochMs > 0L && it.id !in favoriteIds }
        .sortedByDescending { it.lastUsedAtEpochMs }
        .take(5)
    val recentIds = recent.map { it.id }.toSet()
    val frequent = stops.filter { it.useCount > 0 && it.id !in favoriteIds && it.id !in recentIds }
        .sortedByDescending { it.useCount }
        .take(5)
    val featuredIds = favoriteIds + recentIds + frequent.map { it.id }
    val rest = stops.filter { it.id !in featuredIds }.sortedBy { it.name.lowercase() }
    return buildList {
        if (favorites.isNotEmpty()) add(PickerSection(R.string.library_pick_favorites, favorites))
        if (recent.isNotEmpty()) add(PickerSection(R.string.library_pick_recent, recent))
        if (frequent.isNotEmpty()) add(PickerSection(R.string.library_pick_frequent, frequent))
        if (rest.isNotEmpty()) {
            add(
                PickerSection(
                    titleRes = if (isEmpty()) R.string.library_pick_all else R.string.library_pick_az,
                    stops = rest,
                ),
            )
        }
    }
}
