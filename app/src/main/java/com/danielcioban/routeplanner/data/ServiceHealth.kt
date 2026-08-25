package com.danielcioban.routeplanner.data

import com.danielcioban.routeplanner.data.geocoding.NominatimGeocodingClient
import com.danielcioban.routeplanner.data.routing.OsrmRoutingClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ServiceHealthSnapshot(
    val routingOk: Boolean,
    val routingUsedFallback: Boolean,
    val routingHost: String,
    val searchOk: Boolean,
    val searchUsedFallback: Boolean,
    val searchHost: String,
)

object ServiceHealth {
    suspend fun probe(
        routing: OsrmRoutingClient = OsrmRoutingClient(),
        search: NominatimGeocodingClient = NominatimGeocodingClient(),
    ): ServiceHealthSnapshot = withContext(Dispatchers.IO) {
        val osrm = routing.probe()
        val nominatim = search.probe()
        ServiceHealthSnapshot(
            routingOk = osrm.ok,
            routingUsedFallback = osrm.usedFallback,
            routingHost = ServiceEndpoints.hostLabel(osrm.host),
            searchOk = nominatim.ok,
            searchUsedFallback = nominatim.usedFallback,
            searchHost = ServiceEndpoints.hostLabel(nominatim.host),
        )
    }
}

data class EndpointProbe(
    val ok: Boolean,
    val usedFallback: Boolean,
    val host: String,
)
