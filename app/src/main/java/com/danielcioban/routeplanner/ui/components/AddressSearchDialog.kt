package com.danielcioban.routeplanner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.geocoding.NominatimGeocodingClient
import com.danielcioban.routeplanner.data.geocoding.PlaceSearchResult
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.ui.theme.IslandColors
import kotlinx.coroutines.delay

@Composable
fun AddressSearchDialog(
    onDismissRequest: () -> Unit,
    onPlaceSelected: (PlaceSearchResult) -> Unit,
    near: LatLng? = null,
    geocoder: NominatimGeocodingClient = remember { NominatimGeocodingClient() },
) {
    val languageTag = LocalConfiguration.current.locales[0]?.toLanguageTag()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val minCharsHint = stringResource(R.string.search_min_chars)
    val noResultsText = stringResource(R.string.search_no_results)
    val failedText = stringResource(R.string.search_failed)

    LaunchedEffect(query, near, languageTag) {
        val trimmed = query.trim()
        if (trimmed.length < 3) {
            results = emptyList()
            isSearching = false
            errorMessage = if (trimmed.isEmpty()) null else minCharsHint
            return@LaunchedEffect
        }
        errorMessage = null
        isSearching = true
        delay(450)
        val outcome = geocoder.search(
            query = trimmed,
            languageTag = languageTag,
            near = near,
        )
        isSearching = false
        outcome.fold(
            onSuccess = { list ->
                results = list
                errorMessage = if (list.isEmpty()) noResultsText else null
            },
            onFailure = {
                results = emptyList()
                errorMessage = failedText
            },
        )
    }

    IslandDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.search_title),
        dismissLabel = stringResource(R.string.action_close),
    ) {
        SoftOutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = stringResource(R.string.search_query_label),
            placeholder = stringResource(R.string.search_query_hint),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        when {
            isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }

            errorMessage != null && results.isEmpty() -> {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = IslandColors.onSurfaceMuted,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            results.isNotEmpty() -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEach { place ->
                        PlaceResultRow(
                            place = place,
                            onClick = { onPlaceSelected(place) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceResultRow(
    place: PlaceSearchResult,
    onClick: () -> Unit,
) {
    IslandListItem(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.shortName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = IslandColors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = place.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
