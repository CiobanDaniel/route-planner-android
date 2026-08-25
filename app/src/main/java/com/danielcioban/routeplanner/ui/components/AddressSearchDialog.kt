package com.danielcioban.routeplanner.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.RoutePlannerApplication
import com.danielcioban.routeplanner.data.geocoding.NominatimGeocodingClient
import com.danielcioban.routeplanner.data.geocoding.PlaceSearchResult
import com.danielcioban.routeplanner.data.local.SavedSearchEntity
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.ui.places.ContactPlace
import com.danielcioban.routeplanner.ui.theme.IslandColors
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun AddressSearchDialog(
    onDismissRequest: () -> Unit,
    onPlaceSelected: (PlaceSearchResult) -> Unit,
    near: LatLng? = null,
    geocoder: NominatimGeocodingClient = remember { NominatimGeocodingClient() },
) {
    val context = LocalContext.current
    val app = context.applicationContext as? RoutePlannerApplication
    val repository = app?.repository
    val languageTag = LocalConfiguration.current.locales[0]?.toLanguageTag()
    var query by remember { mutableStateOf("") }
    var nearMeOnly by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val minCharsHint = stringResource(R.string.search_min_chars)
    val noResultsText = stringResource(R.string.search_no_results)
    val failedText = stringResource(R.string.search_failed)
    val savedSearches by remember(repository) {
        repository?.observeSavedSearches() ?: flowOf(emptyList())
    }.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var pendingContactUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingContactPhone by remember { mutableStateOf("") }
    var pendingContactName by remember { mutableStateOf("") }

    fun applyContact(uri: android.net.Uri) {
        val contact = ContactPlace.read(context, uri) ?: return
        pendingContactPhone = contact.phone
        pendingContactName = contact.name
        query = contact.address.ifBlank { contact.name }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val uri = pendingContactUri ?: return@rememberLauncherForActivityResult
        pendingContactUri = null
        if (granted) applyContact(uri)
    }
    val pickContact = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            applyContact(uri)
        } else {
            pendingContactUri = uri
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(query, near, nearMeOnly, languageTag) {
        val trimmed = query.trim()
        if (trimmed.length < 3 && !com.danielcioban.routeplanner.util.OpenLocationCode.isFullCode(trimmed)) {
            results = emptyList()
            isSearching = false
            errorMessage = if (trimmed.isEmpty()) null else minCharsHint
            return@LaunchedEffect
        }
        errorMessage = null
        isSearching = true
        delay(650)
        val outcome = geocoder.search(
            query = trimmed,
            languageTag = languageTag,
            near = near,
            nearMeOnly = nearMeOnly && near != null,
        )
        isSearching = false
        outcome.fold(
            onSuccess = { list ->
                results = list.map { place ->
                    if (pendingContactPhone.isBlank()) place
                    else place.copy(
                        phone = pendingContactPhone,
                        shortName = place.shortName.ifBlank { pendingContactName }.ifBlank { place.shortName },
                    )
                }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = nearMeOnly,
                onClick = { nearMeOnly = !nearMeOnly },
                enabled = near != null,
                label = { Text(stringResource(R.string.search_near_me)) },
            )
            TextButton(onClick = { pickContact.launch(null) }) {
                Text(stringResource(R.string.search_from_contact))
            }
        }
        if (query.trim().length >= 3 || com.danielcioban.routeplanner.util.OpenLocationCode.isFullCode(query)) {
            TextButton(
                onClick = {
                    scope.launch {
                        repository?.saveSearch(query, nearMeOnly && near != null)
                    }
                },
            ) {
                Text(stringResource(R.string.search_save))
            }
        }
        if (savedSearches.isNotEmpty()) {
            Text(
                text = stringResource(R.string.search_saved),
                style = MaterialTheme.typography.labelMedium,
                color = IslandColors.onSurfaceMuted,
            )
            savedSearches.take(8).forEach { saved ->
                SavedSearchRow(
                    search = saved,
                    onPick = {
                        nearMeOnly = saved.nearMeOnly
                        query = saved.query
                    },
                    onDelete = {
                        scope.launch { repository?.deleteSavedSearch(saved.id) }
                    },
                )
            }
        }

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
private fun SavedSearchRow(
    search: SavedSearchEntity,
    onPick: () -> Unit,
    onDelete: () -> Unit,
) {
    IslandListItem(onClick = onPick) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = search.query,
                style = MaterialTheme.typography.bodyMedium,
                color = IslandColors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (search.nearMeOnly) {
                Text(
                    text = stringResource(R.string.search_near_me),
                    style = MaterialTheme.typography.labelSmall,
                    color = IslandColors.onSurfaceMuted,
                )
            }
        }
        TextButton(onClick = onDelete) {
            Text(stringResource(R.string.action_delete))
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
