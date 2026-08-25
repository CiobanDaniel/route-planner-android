package com.danielcioban.routeplanner.ui.trip

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.RoutePlannerApplication
import com.danielcioban.routeplanner.data.StopCompletionResult
import com.danielcioban.routeplanner.data.delivery.DeliverySession
import com.danielcioban.routeplanner.data.delivery.DriveKind
import com.danielcioban.routeplanner.data.local.TripStatus
import com.danielcioban.routeplanner.data.routing.DrivingRoute
import com.danielcioban.routeplanner.data.routing.ManeuverFormatter
import com.danielcioban.routeplanner.data.routing.NavGuidance
import com.danielcioban.routeplanner.data.routing.NavigationProgress
import com.danielcioban.routeplanner.data.routing.OffRouteTracker
import com.danielcioban.routeplanner.data.routing.OsrmRoutingClient
import com.danielcioban.routeplanner.data.settings.AppSettings
import com.danielcioban.routeplanner.data.settings.DistanceUnit
import com.danielcioban.routeplanner.data.settings.GeofenceAction
import com.danielcioban.routeplanner.data.settings.GeofenceDwellTracker
import com.danielcioban.routeplanner.data.settings.RouteGeofenceMode
import com.danielcioban.routeplanner.data.settings.StopGeofence
import com.danielcioban.routeplanner.data.settings.osrmOptions
import com.danielcioban.routeplanner.ui.dev.DevLocationSim
import com.danielcioban.routeplanner.ui.location.hasLocationPermission
import com.danielcioban.routeplanner.ui.location.requestLocationUpdates
import com.danielcioban.routeplanner.ui.location.toBearingOrNull
import com.danielcioban.routeplanner.ui.map.LatLng
import com.danielcioban.routeplanner.ui.nav.ManeuverSpeaker
import com.danielcioban.routeplanner.ui.widget.TripWidgetProvider
import com.danielcioban.routeplanner.util.CallAudio
import com.danielcioban.routeplanner.util.CourierHaptics
import com.danielcioban.routeplanner.util.NetworkStatus
import com.danielcioban.routeplanner.util.ShareNextStop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keeps an active trip alive in the background: GPS, next-stop notification,
 * Android 16 status-bar chip, and spoken turns.
 */
class TripGuidanceService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val routingClient = OsrmRoutingClient()
    private lateinit var speaker: ManeuverSpeaker

    private var stopLocation: (() -> Unit)? = null
    private var routeJob: Job? = null
    private var lastFix: LatLng? = null
    private val offRouteTracker = OffRouteTracker()
    private var driving: DrivingRoute? = null
    private var guidance: NavGuidance? = null
    private var spokenKey: String? = null
    private var dest: LatLng? = null
    private var destName: String = ""
    private var destPhone: String = ""
    private var session: DeliverySession = DeliverySession()
    private var settings: AppSettings = AppSettings()
    private var completed = 0
    private var total = 1
    private var remainingStops = 1
    private var tasksBlocked = false
    private var geofenceBusy = false
    private var arrived = false
    private var lastHapticStopId: Long? = null
    private val geofenceDwell = GeofenceDwellTracker()
    private var screenReceiverRegistered = false

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_SCREEN_OFF) return
            if (!session.isActive || session.paused) return
            if (!settings.keepScreenAwake(true, lastFix?.speedMps)) return
            runCatching { LockScreenHudActivity.show(this@TripGuidanceService) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running.set(true)
        speaker = ManeuverSpeaker(this)
        TripNotification.ensureChannel(this)
        startAsForeground()
        registerScreenOff()
        scope.launch { observeSession() }
        scope.launch { observeSettings() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        when (intent?.action) {
            ACTION_MARK_DONE -> scope.launch { markDone() }
            ACTION_END -> scope.launch { endTrip() }
            ACTION_SKIP -> scope.launch { skipCurrent() }
            ACTION_CALL -> callNext()
            ACTION_STOP -> stopSelf()
            else -> startLocation()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running.set(false)
        unregisterScreenOff()
        stopLocation?.invoke()
        stopLocation = null
        routeJob?.cancel()
        speaker.shutdown()
        TripHudStore.clear()
        TripWidgetProvider.updateAll(this, TripHudSnapshot())
        TripNotification.cancel(this)
        scope.cancel()
        super.onDestroy()
    }

    private fun registerScreenOff() {
        if (screenReceiverRegistered) return
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        screenReceiverRegistered = true
    }

    private fun unregisterScreenOff() {
        if (!screenReceiverRegistered) return
        runCatching { unregisterReceiver(screenOffReceiver) }
        screenReceiverRegistered = false
    }

    private fun startAsForeground() {
        publish()
    }

    private suspend fun observeSession() {
        val app = application as RoutePlannerApplication
        app.deliverySessionStore.session.collectLatest { next ->
            session = next
            if (!next.isActive) {
                stopSelf()
                return@collectLatest
            }
            if (next.kind == DriveKind.ROUTE && next.activeRouteId != null) {
                val routeId = next.activeRouteId
                var lastNextId: Long? = null
                var first = true
                app.repository.observeRoute(routeId).collect { snapshot ->
                    val nextId = snapshot?.nextIncompleteStop?.id
                    val destChanged = first || nextId != lastNextId
                    first = false
                    lastNextId = nextId
                    if (destChanged) {
                        refreshDestination()
                        driving = null
                        spokenKey = null
                        guidance = null
                        lastFix?.let { ensureRoute(it, fit = true) }
                    }
                    publish()
                }
            } else {
                refreshDestination()
                lastFix?.let { ensureRoute(it, fit = driving == null) }
                publish()
                kotlinx.coroutines.awaitCancellation()
            }
        }
    }

    private suspend fun observeSettings() {
        val app = application as RoutePlannerApplication
        app.settingsRepository.settings.collectLatest { next ->
            settings = next
            speaker.setLanguage(next.language.tag)
            publish()
        }
    }

    private suspend fun refreshDestination() {
        val app = application as RoutePlannerApplication
        tasksBlocked = false
        when (session.kind) {
            DriveKind.QUICK -> {
                val lat = session.quickLatitude
                val lng = session.quickLongitude
                dest = if (lat != null && lng != null) LatLng(lat, lng) else null
                destName = session.quickName.orEmpty().ifBlank {
                    getString(R.string.drive_here_unnamed)
                }
                destPhone = ""
                completed = 0
                total = 1
                remainingStops = if (arrived) 0 else 1
            }
            DriveKind.ROUTE -> {
                val routeId = session.activeRouteId ?: return
                val snapshot = app.repository.getRoute(routeId) ?: return
                val next = snapshot.nextIncompleteStop
                destName = next?.name ?: snapshot.route.name
                destPhone = next?.phone.orEmpty()
                dest = next?.let { stop ->
                    val lat = stop.latitude ?: return@let null
                    val lng = stop.longitude ?: return@let null
                    LatLng(lat, lng)
                }
                completed = snapshot.completedCount
                total = snapshot.deliveryStops.size.coerceAtLeast(1)
                remainingStops = snapshot.remainingDeliveryStops.size
                arrived = remainingStops == 0
            }
            DriveKind.NONE -> Unit
        }
    }

    private fun startLocation() {
        if (stopLocation != null) return
        if (!hasLocationPermission() && !DevLocationSim.isActive()) return
        stopLocation = requestLocationUpdates(highFrequency = true) { location ->
            onLocation(location)
        }
    }

    private fun onLocation(location: Location) {
        val fix = location.toNavFix()
        lastFix = fix
        scope.launch {
            maybeGeofence(fix)
            val target = dest
            val route = driving
            if (target == null) {
                refreshDestination()
                publish()
                return@launch
            }
            if (route == null) {
                ensureRoute(fix, fit = true)
                return@launch
            }
            val radius = arrivalRadius()
            val nextGuidance = NavigationProgress.evaluate(route, fix, target, radius)
            guidance = nextGuidance
            arrived = nextGuidance.arrived
            if (!nextGuidance.arrived &&
                offRouteTracker.shouldRecalc(
                    nextGuidance.distanceToRouteMeters,
                    settings.rerouteAggressiveness,
                )
            ) {
                ensureRoute(fix, fit = false)
                return@launch
            }
            maybeSpeak()
            publish()
        }
    }

    private fun ensureRoute(from: LatLng, fit: Boolean) {
        val to = dest ?: return
        routeJob?.cancel()
        routeJob = scope.launch {
            val osrm = settings.osrmOptions()
            val result = routingClient.routeForNavigation(
                from,
                to,
                profile = osrm.profile,
                exclude = osrm.exclude,
                walkLastMile = settings.walkLastMile,
            )
            val next = result.getOrElse {
                DrivingRoute.recoverAfterFailure(driving, from, to)
            }
            driving = next
            offRouteTracker.markRecalc()
            guidance = NavigationProgress.evaluate(next, from, to, arrivalRadius())
            arrived = guidance?.arrived == true
            maybeSpeak()
            publish()
        }
    }

    private fun maybeSpeak() {
        if (!settings.speakManeuvers) return
        if (settings.muteTtsDuringCalls && CallAudio.isInVoiceCall(this)) return
        if (driving?.isApproximate == true) return
        val key = guidance?.let {
            ManeuverSpeaker.spokenKey(this, it, settings.ttsVerbosity)
        } ?: return
        if (key == spokenKey) return
        spokenKey = key
        speaker.speak(key)
    }

    private suspend fun maybeGeofence(user: LatLng) {
        if (geofenceBusy) return
        if (session.paused) {
            geofenceDwell.reset()
            return
        }
        val app = application as RoutePlannerApplication
        val dwellMs = settings.geofenceDwellSeconds * 1000L
        val now = System.currentTimeMillis()
        when (session.kind) {
            DriveKind.QUICK -> {
                val target = dest ?: return
                val radius = settings.geofenceRadiusMeters.toDouble()
                    .coerceAtLeast(StopGeofence.DEFAULT_RADIUS_METERS.toDouble())
                val inside = StopGeofence.isInside(
                    user.latitude,
                    user.longitude,
                    target.latitude,
                    target.longitude,
                    radius.toInt(),
                    user.accuracyMeters,
                )
                if (!geofenceDwell.ready(-1L, inside, now, dwellMs)) return
                arrived = true
                if (lastHapticStopId != -1L) {
                    lastHapticStopId = -1L
                    CourierHaptics.geofenceArrive(this@TripGuidanceService, settings)
                }
                publish()
            }
            DriveKind.ROUTE -> {
                val routeId = session.activeRouteId ?: return
                val snapshot = app.repository.getRoute(routeId) ?: return
                val action = StopGeofence.effectiveAction(
                    RouteGeofenceMode.fromStored(snapshot.route.geofenceMode),
                    settings.geofenceAction,
                )
                if (action == GeofenceAction.OFF) return
                geofenceBusy = true
                try {
                    val nextId = snapshot.nextIncompleteStop?.id
                    for (stop in snapshot.deliveryStops) {
                        if (stop.isCompleted) continue
                        val lat = stop.latitude ?: continue
                        val lng = stop.longitude ?: continue
                        val radius = StopGeofence.effectiveRadiusMeters(
                            stop.geofenceRadiusMeters,
                            snapshot.route.geofenceRadiusMeters,
                            settings.geofenceRadiusMeters,
                        )
                        val inside = StopGeofence.isInside(
                            user.latitude,
                            user.longitude,
                            lat,
                            lng,
                            radius,
                            user.accuracyMeters,
                        )
                        if (!geofenceDwell.ready(stop.id, inside, now, dwellMs)) {
                            continue
                        }
                        if (!stop.isVisited) {
                            app.repository.markStopVisited(stop.id)
                        }
                        if (lastHapticStopId != stop.id) {
                            lastHapticStopId = stop.id
                            CourierHaptics.geofenceArrive(this@TripGuidanceService, settings)
                        }
                        if (action != GeofenceAction.COMPLETE || stop.id != nextId) continue
                        when (app.repository.setStopCompleted(stop.id, true)) {
                            StopCompletionResult.Updated -> {
                                syncTripProgress()
                                refreshDestination()
                                driving = null
                                spokenKey = null
                                lastFix?.let { ensureRoute(it, fit = true) }
                            }
                            is StopCompletionResult.BlockedByRequiredTasks -> {
                                tasksBlocked = true
                            }
                            StopCompletionResult.StopMissing -> Unit
                        }
                    }
                } finally {
                    geofenceBusy = false
                }
            }
            DriveKind.NONE -> Unit
        }
    }

    private suspend fun markDone() {
        val app = application as RoutePlannerApplication
        when (session.kind) {
            DriveKind.QUICK -> {
                val tripId = session.tripHistoryId
                if (tripId != null) {
                    app.repository.finishTrip(tripId, TripStatus.COMPLETED, 1, 1)
                }
                app.deliverySessionStore.clear()
            }
            DriveKind.ROUTE -> {
                val routeId = session.activeRouteId ?: return
                val snapshot = app.repository.getRoute(routeId) ?: return
                val next = snapshot.nextIncompleteStop ?: return
                when (app.repository.setStopCompleted(next.id, true)) {
                    StopCompletionResult.Updated -> {
                        tasksBlocked = false
                        syncTripProgress()
                        refreshDestination()
                        driving = null
                        spokenKey = null
                        lastFix?.let { ensureRoute(it, fit = true) }
                        publish()
                    }
                    is StopCompletionResult.BlockedByRequiredTasks -> {
                        tasksBlocked = true
                        publish()
                    }
                    StopCompletionResult.StopMissing -> Unit
                }
            }
            DriveKind.NONE -> Unit
        }
    }

    private suspend fun skipCurrent() {
        val app = application as RoutePlannerApplication
        if (session.kind != DriveKind.ROUTE) return
        val routeId = session.activeRouteId ?: return
        val snapshot = app.repository.getRoute(routeId) ?: return
        val next = snapshot.nextIncompleteStop ?: return
        if (snapshot.remainingDeliveryStops.size < 2) return
        app.repository.moveStopToEdge(routeId, next.id, toStart = false)
        driving = null
        spokenKey = null
        refreshDestination()
        lastFix?.let { ensureRoute(it, fit = true) }
        publish()
    }

    private fun callNext() {
        val phone = destPhone
        if (phone.isNotBlank()) {
            ShareNextStop.dial(this, phone)
        }
    }

    private suspend fun endTrip() {
        val app = application as RoutePlannerApplication
        val tripId = session.tripHistoryId
        if (tripId != null) {
            val remaining = remainingStops
            app.repository.finishTrip(
                tripId,
                if (remaining > 0 && session.kind == DriveKind.ROUTE) {
                    TripStatus.CANCELLED
                } else {
                    TripStatus.COMPLETED
                },
                completed,
                total,
            )
        }
        app.deliverySessionStore.clear()
    }

    private suspend fun syncTripProgress() {
        val app = application as RoutePlannerApplication
        val tripId = session.tripHistoryId ?: return
        val routeId = session.activeRouteId ?: return
        val snapshot = app.repository.getRoute(routeId) ?: return
        app.repository.updateTripProgress(tripId, snapshot.completedCount, snapshot.stops.size)
    }

    private fun arrivalRadius(): Double {
        return settings.geofenceRadiusMeters.toDouble()
            .coerceAtLeast(StopGeofence.DEFAULT_RADIUS_METERS.toDouble())
    }

    private fun currentNotice(): TripNotice {
        val unit = settings.distanceUnit
        val remainingMeters = guidance?.remainingDistanceMeters
            ?: dest?.let { target ->
                lastFix?.let { fix ->
                    com.danielcioban.routeplanner.util.GeoUtils.distanceMeters(
                        fix.latitude,
                        fix.longitude,
                        target.latitude,
                        target.longitude,
                    )
                }
            }
        val allDone = session.kind == DriveKind.ROUTE && remainingStops <= 0
        val title = when {
            allDone -> getString(R.string.trip_notification_all_done)
            arrived -> getString(R.string.trip_notification_arrived, destName)
            destName.isNotBlank() -> destName
            else -> getString(R.string.trip_notification_starting)
        }
        val text = when {
            tasksBlocked -> getString(R.string.trip_notification_tasks)
            allDone -> getString(R.string.trip_notification_all_done_body)
            remainingMeters != null -> {
                val dist = TripNotification.bodyDistance(remainingMeters, unit)
                val etaMin = guidance?.remainingDurationSeconds?.div(60.0)?.roundToIntSafe()
                if (etaMin != null && etaMin > 0) {
                    getString(R.string.trip_notification_eta, dist, etaMin)
                } else {
                    getString(R.string.trip_notification_distance, dist)
                }
            }
            else -> getString(R.string.trip_notification_waiting_gps)
        }
        val chip = when {
            allDone -> getString(R.string.trip_notification_chip_done)
            arrived -> getString(R.string.trip_notification_chip_here)
            remainingMeters != null -> TripNotification.chipDistance(remainingMeters, unit)
            else -> "GPS"
        }
        val sub = when (session.kind) {
            DriveKind.ROUTE -> getString(
                R.string.trip_notification_progress,
                completed,
                total,
            )
            DriveKind.QUICK -> getString(R.string.trip_notification_one_off)
            DriveKind.NONE -> getString(R.string.app_name)
        }
        val etaEpoch = guidance?.remainingDurationSeconds?.takeIf { it > 15 }?.let { seconds ->
            System.currentTimeMillis() + (seconds * 1000).toLong()
        }
        val showDone = !allDone && (arrived || dest != null)
        return TripNotice(
            title = title,
            text = text,
            subText = sub,
            chipText = chip,
            progress = completed.coerceAtMost(total),
            progressMax = total.coerceAtLeast(1),
            etaEpochMs = etaEpoch,
            arrived = arrived,
            canMarkDone = showDone && !tasksBlocked,
            allDone = allDone,
            canSkip = session.kind == DriveKind.ROUTE && remainingStops > 1 && !allDone,
            phone = destPhone.takeIf { it.isNotBlank() },
        )
    }

    private fun publish() {
        val notice = currentNotice()
        val unit = settings.distanceUnit
        val maneuver = guidance?.currentStep?.let { ManeuverFormatter.formatStep(resources, it) }.orEmpty()
        val thenManeuver = guidance?.thenStep?.let {
            getString(R.string.nav_then, ManeuverFormatter.formatStep(resources, it))
        }.orEmpty()
        val distance = when {
            notice.allDone || notice.arrived -> ""
            guidance != null -> TripNotification.bodyDistance(guidance!!.distanceToManeuverMeters, unit)
            else -> ""
        }
        TripHudStore.publish(
            TripHudSnapshot(
                active = running.get() && session.isActive,
                title = destName.ifBlank { notice.title },
                maneuver = maneuver.ifBlank { notice.text },
                thenManeuver = thenManeuver,
                distance = distance,
                eta = notice.text,
                progressLabel = notice.subText,
                arrived = notice.arrived,
                canMarkDone = !notice.allDone && dest != null,
                allDone = notice.allDone,
                tasksBlocked = tasksBlocked,
                keepScreenOn = settings.keepScreenAwake(
                    session.isActive && !session.paused,
                    lastFix?.speedMps,
                ),
                paused = session.paused,
                speedMps = lastFix?.speedMps,
            ),
        )
        TripWidgetProvider.updateAll(this, TripHudStore.snapshot.value)
        val notification = TripNotification.buildForeground(this, notice)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(this, TripNotification.NOTIFICATION_ID, notification, type)
        TripNotification.postLiveUpdate(this, notice)
    }

    companion object {
        const val ACTION_MARK_DONE = "com.danielcioban.routeplanner.action.MARK_DONE"
        const val ACTION_END = "com.danielcioban.routeplanner.action.END_TRIP"
        const val ACTION_STOP = "com.danielcioban.routeplanner.action.STOP_GUIDANCE"
        const val ACTION_SKIP = "com.danielcioban.routeplanner.action.SKIP_STOP"
        const val ACTION_CALL = "com.danielcioban.routeplanner.action.CALL_STOP"

        private val running = AtomicBoolean(false)
        val isRunning: Boolean get() = running.get()

        fun start(context: Context) {
            if (!context.hasLocationPermission()) return
            val intent = Intent(context, TripGuidanceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, TripGuidanceService::class.java).setAction(action)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TripGuidanceService::class.java))
        }
    }
}

private fun Location.toNavFix(): LatLng = LatLng(
    latitude = latitude,
    longitude = longitude,
    bearingDegrees = toBearingOrNull(),
    speedMps = speed.takeIf { hasSpeed() && it >= 0f },
    accuracyMeters = accuracy.takeIf { hasAccuracy() },
)

private fun Double.roundToIntSafe(): Int = kotlin.math.round(this).toInt()
