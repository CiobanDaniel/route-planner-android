package com.danielcioban.routeplanner.ui.map

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.danielcioban.routeplanner.data.local.StopEntity
import org.json.JSONObject

private const val EMPTY_LINE_GEOJSON = """{"type":"FeatureCollection","features":[]}"""

data class LatLng(
    val latitude: Double,
    val longitude: Double,
    /** Degrees clockwise from north; null when unknown. */
    val bearingDegrees: Float? = null,
)

private class MapJsBridge(
    private val mainHandler: Handler,
) {
    @Volatile
    var onMapLongClick: ((LatLng) -> Unit)? = null

    @Volatile
    var onStopClick: ((Long) -> Unit)? = null

    @Volatile
    var onFollowPaused: ((Boolean) -> Unit)? = null

    @JavascriptInterface
    fun onMapLongClick(lat: Double, lon: Double) {
        mainHandler.post {
            onMapLongClick?.invoke(LatLng(latitude = lat, longitude = lon))
        }
    }

    @JavascriptInterface
    fun onStopClick(id: Double) {
        mainHandler.post {
            onStopClick?.invoke(id.toLong())
        }
    }

    @JavascriptInterface
    fun onFollowPaused(paused: Boolean) {
        mainHandler.post {
            onFollowPaused?.invoke(paused)
        }
    }
}

private class MapWebState {
    var pageReady: Boolean = false
    var lastPoints: String = ""
    var lastLine: String = ""
    var lastFitRequested: Boolean = false
    var lastUser: LatLng? = null
    var lastRecenterToken: Int = -1
    var lastStyleId: String = MapViewMode.MAP.id
    var lastDriveFollow: Boolean = false
    var lastNavLine: String? = null
    var lastNavFitToken: Int = 0
    var pendingResumeFollow: Boolean = false
    var lastFocus: LatLng? = null
    var lastFocusToken: Int = 0
    var pushedFocusToken: Int = -1

    var pushedStyleId: String? = null
    var pushedDriveFollow: Boolean? = null
    var pushedRouteSignature: String? = null
    var fittedRouteSignature: String? = null
    var pushedUserKey: String? = null
    var pushedNavKey: String? = null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RouteMapBackdrop(
    modifier: Modifier = Modifier,
    stops: List<StopEntity> = emptyList(),
    fitStops: Boolean = false,
    userLocation: LatLng? = null,
    recenterToken: Int = 0,
    mapViewMode: MapViewMode = MapViewMode.MAP,
    driveFollow: Boolean = false,
    navRouteLineJson: String? = null,
    navRouteFitToken: Int = 0,
    /** Straight stop-to-stop sketch; hide while turn-by-turn road route is active. */
    showStraightStopLinks: Boolean = true,
    onMapLongClick: ((LatLng) -> Unit)? = null,
    onStopClick: ((Long) -> Unit)? = null,
    onFollowPaused: ((Boolean) -> Unit)? = null,
    focusTarget: LatLng? = null,
    focusToken: Int = 0,
) {
    val pointsJson = remember(stops) { stopsToPointsGeoJson(stops) }
    val lineJson = remember(stops, showStraightStopLinks) {
        if (showStraightStopLinks) stopsToLineGeoJson(stops) else EMPTY_LINE_GEOJSON
    }
    val bridge = remember { MapJsBridge(Handler(Looper.getMainLooper())) }
    val webState = remember { MapWebState() }
    bridge.onMapLongClick = onMapLongClick
    bridge.onStopClick = onStopClick
    bridge.onFollowPaused = onFollowPaused

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.allowFileAccess = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    settings.safeBrowsingEnabled = true
                }
                setInitialScale(100)
                webChromeClient = WebChromeClient()
                addJavascriptInterface(bridge, "AndroidBridge")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        webState.pageReady = true
                        webState.pushedStyleId = null
                        webState.pushedDriveFollow = null
                        webState.pushedRouteSignature = null
                        webState.fittedRouteSignature = null
                        webState.pushedUserKey = null
                        webState.pushedNavKey = null
                        webState.pushedFocusToken = -1
                        pushAll(
                            view,
                            webState,
                            forceFlyToUser = recenterToken != webState.lastRecenterToken,
                        )
                        webState.lastRecenterToken = recenterToken
                    }
                }
                loadUrl("file:///android_asset/map.html")
            }
        },
        update = { webView ->
            val shouldFly = recenterToken != webState.lastRecenterToken
            if (shouldFly) {
                webState.pendingResumeFollow = true
            }
            webState.lastPoints = pointsJson
            webState.lastLine = lineJson
            webState.lastFitRequested = fitStops
            webState.lastUser = userLocation
            webState.lastStyleId = mapViewMode.id
            webState.lastDriveFollow = driveFollow
            webState.lastNavLine = navRouteLineJson
            webState.lastNavFitToken = navRouteFitToken
            webState.lastFocus = focusTarget
            webState.lastFocusToken = focusToken
            if (webState.pageReady) {
                pushAll(webView, webState, forceFlyToUser = shouldFly)
                webState.lastRecenterToken = recenterToken
            }
        },
    )
}

private fun pushAll(webView: WebView?, state: MapWebState, forceFlyToUser: Boolean) {
    if (webView == null || !state.pageReady) return

    if (state.pushedStyleId != state.lastStyleId) {
        webView.evaluateJavascript(
            "window.setMapStyle && setMapStyle(${JSONObject.quote(state.lastStyleId)});",
            null,
        )
        state.pushedStyleId = state.lastStyleId
    }

    if (state.pushedDriveFollow != state.lastDriveFollow) {
        webView.evaluateJavascript(
            "window.setDriveFollow && setDriveFollow(${state.lastDriveFollow});",
            null,
        )
        state.pushedDriveFollow = state.lastDriveFollow
    }

    val routeSignature = state.lastPoints + "\n" + state.lastLine
    val routeChanged = state.pushedRouteSignature != routeSignature
    val shouldFit = state.lastFitRequested &&
        !state.lastDriveFollow &&
        routeChanged &&
        state.fittedRouteSignature != routeSignature

    if (routeChanged || shouldFit) {
        pushRoute(webView, state.lastPoints, state.lastLine, fitStops = shouldFit)
        state.pushedRouteSignature = routeSignature
        if (shouldFit) {
            state.fittedRouteSignature = routeSignature
        }
    }

    val navKey = "${state.lastNavLine.hashCode()}:${state.lastNavFitToken}"
    if (state.pushedNavKey != navKey) {
        val line = state.lastNavLine
        if (line.isNullOrBlank()) {
            webView.evaluateJavascript("window.clearNavRoute && clearNavRoute();", null)
        } else {
            val shouldFitNav = state.pushedNavKey?.substringAfterLast(':') != state.lastNavFitToken.toString()
            pushNavRoute(webView, line, fitStops = shouldFitNav && !state.lastDriveFollow)
        }
        state.pushedNavKey = navKey
    }

    state.lastUser?.let { user ->
        val bearing = user.bearingDegrees?.takeIf { it >= 0f }
        val userKey = "${user.latitude},${user.longitude},${bearing ?: -1},${forceFlyToUser},${state.lastDriveFollow}"
        if (state.pushedUserKey == userKey && !forceFlyToUser && !state.pendingResumeFollow) return@let
        val firstCenter = state.lastRecenterToken < 0 && !forceFlyToUser
        val fly = forceFlyToUser || firstCenter
        pushUserLocation(webView, user, fly = fly, bearing = bearing)
        state.pushedUserKey = userKey
    }

    if (state.pendingResumeFollow) {
        webView.evaluateJavascript("window.resumeDriveFollow && resumeDriveFollow();", null)
        state.pendingResumeFollow = false
    }

    val focus = state.lastFocus
    if (focus != null && state.pushedFocusToken != state.lastFocusToken) {
        webView.evaluateJavascript(
            "window.flyToPlace && flyToPlace(${focus.latitude}, ${focus.longitude}, 16);",
            null,
        )
        state.pushedFocusToken = state.lastFocusToken
    }
}

private fun pushRoute(
    webView: WebView,
    pointsJson: String,
    lineJson: String,
    fitStops: Boolean,
) {
    val pointsArg = JSONObject.quote(pointsJson)
    val lineArg = JSONObject.quote(lineJson)
    webView.evaluateJavascript(
        "window.setRoute && setRoute($pointsArg, $lineArg, ${if (fitStops) "true" else "false"});",
        null,
    )
}

private fun pushNavRoute(webView: WebView, lineJson: String, fitStops: Boolean) {
    val lineArg = JSONObject.quote(lineJson)
    webView.evaluateJavascript(
        "window.setNavRoute && setNavRoute($lineArg, ${if (fitStops) "true" else "false"});",
        null,
    )
}

private fun pushUserLocation(
    webView: WebView,
    user: LatLng,
    fly: Boolean,
    bearing: Float?,
) {
    val bearingArg = bearing?.toDouble()?.toString() ?: "null"
    webView.evaluateJavascript(
        "window.setUserLocation && setUserLocation(${user.latitude}, ${user.longitude}, ${if (fly) "true" else "false"}, $bearingArg);",
        null,
    )
}
