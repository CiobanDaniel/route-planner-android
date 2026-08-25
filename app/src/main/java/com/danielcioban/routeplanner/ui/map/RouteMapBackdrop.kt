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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.danielcioban.routeplanner.BuildConfig
import com.danielcioban.routeplanner.RoutePlannerApplication
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.data.settings.SavedMapCamera
import com.danielcioban.routeplanner.data.local.StopEntity
import com.danielcioban.routeplanner.ui.theme.IslandColors
import org.json.JSONObject

private const val EMPTY_LINE_GEOJSON = """{"type":"FeatureCollection","features":[]}"""

data class LatLng(
    val latitude: Double,
    val longitude: Double,
    /** Degrees clockwise from north; null when unknown. */
    val bearingDegrees: Float? = null,
    /** Horizontal speed m/s when known — used to prefer GPS course while moving. */
    val speedMps: Float? = null,
    /** Horizontal accuracy in meters when known — used for poor-GPS hints. */
    val accuracyMeters: Float? = null,
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

    @Volatile
    var onMapViewChanged: ((SavedMapCamera) -> Unit)? = null

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

    @JavascriptInterface
    fun onMapViewChanged(lat: Double, lng: Double, zoom: Double) {
        mainHandler.post {
            onMapViewChanged?.invoke(SavedMapCamera(lat, lng, zoom))
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
    var lastThemeDark: Boolean? = null
    var lastDriveFollow: Boolean = false
    var lastNavLine: String? = null
    var lastNavFitToken: Int = 0
    var pendingResumeFollow: Boolean = false
    var lastFocus: LatLng? = null
    var lastFocusToken: Int = 0
    var pushedFocusToken: Int = -1

    var pushedStyleId: String? = null
    var pushedThemeDark: Boolean? = null
    var pushedDriveFollow: Boolean? = null
    var pushedRouteSignature: String? = null
    var fittedRouteSignature: String? = null
    var pushedUserKey: String? = null
    var pushedNavKey: String? = null
    var hasFlownToUser: Boolean = false
    var restoredCamera: Boolean = false
    var lastSavedCamera: SavedMapCamera? = null
    var lastClusterPins: Boolean = false
    var lastNorthUp: Boolean = false
    var lastImperialScale: Boolean = false
    var lastNavDoor: LatLng? = null
    var lastReloadEpoch: Long = 0L
    var lastHighContrastPins: Boolean = false
    var lastReduceMotion: Boolean = false
    var pushedNorthUp: Boolean? = null
    var pushedImperialScale: Boolean? = null
    var pushedHighContrastPins: Boolean? = null
    var pushedReduceMotion: Boolean? = null
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
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
    savedCamera: SavedMapCamera? = null,
    onCameraMoved: ((SavedMapCamera) -> Unit)? = null,
    clusterPins: Boolean = false,
    navDoor: LatLng? = null,
) {
    val app = LocalContext.current.applicationContext as RoutePlannerApplication
    val appSettings by app.settingsRepository.settings.collectAsState(initial = app.latestSettings)
    val reloadEpoch by MapAssetReload.epoch.collectAsState()
    val northUp = appSettings.northUpWhileDriving
    val imperialScale = appSettings.distanceUnit == DistanceUnit.IMPERIAL
    val highContrastPins = appSettings.highContrastPins
    val reduceMotion = appSettings.reduceMotion
    val pointsJson = remember(stops) { stopsToPointsGeoJson(stops) }
    val lineJson = remember(stops, showStraightStopLinks) {
        if (showStraightStopLinks) stopsToLineGeoJson(stops) else EMPTY_LINE_GEOJSON
    }
    val bridge = remember { MapJsBridge(Handler(Looper.getMainLooper())) }
    val webState = remember { MapWebState() }
    val darkBasemap = !IslandColors.useHighlightShadow
    webState.lastSavedCamera = savedCamera
    bridge.onMapLongClick = onMapLongClick
    bridge.onStopClick = onStopClick
    bridge.onFollowPaused = onFollowPaused
    bridge.onMapViewChanged = { camera ->
        LastKnownMapView.update(camera)
        onCameraMoved?.invoke(camera)
    }

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
                settings.userAgentString =
                    settings.userAgentString + " RoutePlannerAndroid/" + BuildConfig.VERSION_NAME
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
                        webState.pushedThemeDark = null
                        webState.pushedDriveFollow = null
                        webState.pushedNorthUp = null
                        webState.pushedImperialScale = null
                        webState.pushedHighContrastPins = null
                        webState.pushedReduceMotion = null
                        webState.pushedRouteSignature = null
                        webState.fittedRouteSignature = null
                        webState.pushedUserKey = null
                        webState.pushedNavKey = null
                        webState.pushedFocusToken = -1
                        restoreSavedCamera(view, webState)
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
            webState.lastThemeDark = darkBasemap
            webState.lastDriveFollow = driveFollow
            webState.lastNavLine = navRouteLineJson
            webState.lastNavFitToken = navRouteFitToken
            webState.lastFocus = focusTarget
            webState.lastFocusToken = focusToken
            webState.lastSavedCamera = savedCamera
            webState.lastClusterPins = clusterPins
            webState.lastNorthUp = northUp
            webState.lastImperialScale = imperialScale
            webState.lastHighContrastPins = highContrastPins
            webState.lastReduceMotion = reduceMotion
            webState.lastNavDoor = navDoor
            if (reloadEpoch > 0L && reloadEpoch != webState.lastReloadEpoch) {
                webState.lastReloadEpoch = reloadEpoch
                webState.pageReady = false
                webState.restoredCamera = false
                webState.hasFlownToUser = false
                webState.pushedStyleId = null
                webState.pushedThemeDark = null
                webState.pushedDriveFollow = null
                webState.pushedNorthUp = null
                webState.pushedImperialScale = null
                webState.pushedHighContrastPins = null
                webState.pushedReduceMotion = null
                webState.pushedRouteSignature = null
                webState.fittedRouteSignature = null
                webState.pushedUserKey = null
                webState.pushedNavKey = null
                webState.pushedFocusToken = -1
                webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                webView.clearCache(true)
                webView.loadUrl("file:///android_asset/map.html?v=$reloadEpoch")
                return@AndroidView
            }
            if (webState.pageReady) {
                pushAll(webView, webState, forceFlyToUser = shouldFly)
                webState.lastRecenterToken = recenterToken
            }
        },
        onRelease = { webView ->
            releaseMapWebView(webView)
        },
    )
}

private fun releaseMapWebView(webView: WebView) {
    webView.stopLoading()
    webView.loadUrl("about:blank")
    webView.removeJavascriptInterface("AndroidBridge")
    (webView.parent as? ViewGroup)?.removeView(webView)
    webView.destroy()
}

private fun pushAll(webView: WebView?, state: MapWebState, forceFlyToUser: Boolean) {
    if (webView == null || !state.pageReady) return

    if (state.pushedThemeDark != state.lastThemeDark) {
        val theme = if (state.lastThemeDark == true) "dark" else "light"
        webView.evaluateJavascript(
            "window.setMapTheme && setMapTheme(${JSONObject.quote(theme)});",
            null,
        )
        state.pushedThemeDark = state.lastThemeDark
        // Street tiles are rebuilt in JS; re-push style next.
        state.pushedStyleId = null
    }

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

    if (state.pushedNorthUp != state.lastNorthUp) {
        webView.evaluateJavascript(
            "window.setNorthUp && setNorthUp(${state.lastNorthUp});",
            null,
        )
        state.pushedNorthUp = state.lastNorthUp
    }

    if (state.pushedImperialScale != state.lastImperialScale) {
        webView.evaluateJavascript(
            "window.setImperialScale && setImperialScale(${state.lastImperialScale});",
            null,
        )
        state.pushedImperialScale = state.lastImperialScale
    }

    if (state.pushedReduceMotion != state.lastReduceMotion) {
        webView.evaluateJavascript(
            "window.setReduceMotion && setReduceMotion(${state.lastReduceMotion});",
            null,
        )
        state.pushedReduceMotion = state.lastReduceMotion
    }

    if (state.pushedHighContrastPins != state.lastHighContrastPins) {
        webView.evaluateJavascript(
            "window.setHighContrastPins && setHighContrastPins(${state.lastHighContrastPins});",
            null,
        )
        state.pushedHighContrastPins = state.lastHighContrastPins
    }

    val routeSignature = state.lastPoints + "\n" + state.lastLine + "\n" + state.lastClusterPins
    val routeChanged = state.pushedRouteSignature != routeSignature
    val shouldFit = state.lastFitRequested &&
        !state.lastDriveFollow &&
        routeChanged &&
        state.fittedRouteSignature != routeSignature

    if (routeChanged) {
        pushRoute(webView, state.lastPoints, state.lastLine, fitStops = shouldFit, clusterPins = state.lastClusterPins)
        state.pushedRouteSignature = routeSignature
        if (shouldFit) {
            state.fittedRouteSignature = routeSignature
        }
    }

    val door = state.lastNavDoor
    val navKey = "${state.lastNavLine.hashCode()}:${state.lastNavFitToken}:${door?.latitude}:${door?.longitude}"
    if (state.pushedNavKey != navKey) {
        val line = state.lastNavLine
        if (line.isNullOrBlank()) {
            webView.evaluateJavascript("window.clearNavRoute && clearNavRoute();", null)
        } else {
        val previousFit = state.pushedNavKey?.split(":")?.getOrNull(1)
        val shouldFitNav = previousFit != state.lastNavFitToken.toString()
        pushNavRoute(webView, line, fitStops = shouldFitNav && !state.lastDriveFollow, door = door)
        }
        state.pushedNavKey = navKey
    }

    state.lastUser?.let { user ->
        val bearing = user.bearingDegrees?.takeIf { it >= 0f }
        val userKey = "${user.latitude},${user.longitude},${bearing ?: -1},${forceFlyToUser},${state.lastDriveFollow}"
        if (state.pushedUserKey == userKey && !forceFlyToUser && !state.pendingResumeFollow) return@let
        // First GPS fly only when this WebView has no restored camera.
        val fly = forceFlyToUser || !state.hasFlownToUser
        pushUserLocation(webView, user, fly = fly, bearing = bearing)
        state.pushedUserKey = userKey
        if (fly) state.hasFlownToUser = true
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

private fun restoreSavedCamera(webView: WebView?, state: MapWebState) {
    val camera = state.lastSavedCamera ?: return
    if (state.restoredCamera || webView == null) return
    webView.evaluateJavascript(
        "window.restoreMapView && restoreMapView(${camera.latitude}, ${camera.longitude}, ${camera.zoom});",
        null,
    )
    state.restoredCamera = true
    state.hasFlownToUser = true
}

private fun pushRoute(
    webView: WebView,
    pointsJson: String,
    lineJson: String,
    fitStops: Boolean,
    clusterPins: Boolean = false,
) {
    val pointsArg = JSONObject.quote(pointsJson)
    val lineArg = JSONObject.quote(lineJson)
    webView.evaluateJavascript(
        "window.setRoute && setRoute($pointsArg, $lineArg, ${if (fitStops) "true" else "false"}, ${if (clusterPins) "true" else "false"});",
        null,
    )
}

private fun pushNavRoute(webView: WebView, lineJson: String, fitStops: Boolean, door: LatLng?) {
    val lineArg = JSONObject.quote(lineJson)
    val destArgs = if (door != null) {
        ", ${door.latitude}, ${door.longitude}"
    } else {
        ", null, null"
    }
    webView.evaluateJavascript(
        "window.setNavRoute && setNavRoute($lineArg, ${if (fitStops) "true" else "false"}$destArgs);",
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
