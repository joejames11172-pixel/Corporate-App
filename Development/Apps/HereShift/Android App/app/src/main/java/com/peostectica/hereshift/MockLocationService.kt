package com.peostectica.hereshift

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.provider.Settings

/** Explicit, user-initiated Android Developer Options test-provider service.
 * No bypasses of mock-location indicators or third-party integrity controls.
 * Only the stationary coordinates entered by the user are emitted.
 */
@Suppress("DEPRECATION")
class MockLocationService : Service() {
    companion object {
        const val ACTION_START = "com.peostectica.hereshift.START_TEST"
        const val ACTION_STOP = "com.peostectica.hereshift.STOP_TEST"
        const val EXTRA_LAT = "latitude"
        const val EXTRA_LON = "longitude"
        const val PREF_STATUS = "sim_status"
        private const val CHANNEL = "hereshift_test_location"
        private const val NOTE_ID = 7104
    }
    private val handler = Handler(Looper.getMainLooper())
    private val manager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }
    private var active = false
    // Never remove a provider that this service did not successfully register.
    private var ownsTestProvider = false
    private var lat = 0.0
    private var lon = 0.0
    private val heartbeat = object : Runnable {
        override fun run() {
            if (!active) return
            try {
                val point = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = lat; longitude = lon
                    accuracy = 5f; speed = 0f
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
                manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, point)
                handler.postDelayed(this, 1_500L)
            } catch (ex: Exception) {
                setStatus("Simulation stopped: ${ex.javaClass.simpleName}")
                stopSelf()
            }
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun setStatus(status: String) = getSharedPreferences("hereShift", MODE_PRIVATE)
        .edit().putString(PREF_STATUS, status).apply()

    override fun onCreate() {
        super.onCreate()
        val n = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        n.createNotificationChannel(NotificationChannel(CHANNEL, "HereShift test-location status", NotificationManager.IMPORTANCE_LOW))
    }
    private fun notification(message: String): Notification {
        val stop = Intent(this, MockLocationService::class.java).setAction(ACTION_STOP)
        val stopIntent = PendingIntent.getService(this, 400, stop, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = PendingIntent.getActivity(this, 401, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("HereShift — simulated location ACTIVE")
            .setContentText(message)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Stop simulation", stopIntent).build())
            .build()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if (intent?.action != ACTION_START) { stopSelf(); return START_NOT_STICKY }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            setStatus("Permission missing: precise location required by Android foreground-service policy")
            stopSelf(); return START_NOT_STICKY
        }
        val newLat = intent.getDoubleExtra(EXTRA_LAT, Double.NaN)
        val newLon = intent.getDoubleExtra(EXTRA_LON, Double.NaN)
        if (!newLat.isFinite() || !newLon.isFinite() || newLat !in -90.0..90.0 || newLon !in -180.0..180.0) {
            setStatus("Invalid coordinates. No simulation started")
            stopSelf(); return START_NOT_STICKY
        }
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTE_ID, notification("Stationary test point · Stop from notification"), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTE_ID, notification("Stationary test point · Stop from notification"))
            }
            stopProvider()
            lat = newLat; lon = newLon
            manager.addTestProvider(LocationManager.GPS_PROVIDER,
                false, true, false, false, false, true, true, 1, 1)
            ownsTestProvider = true
            manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
            active = true
            setStatus("ACTIVE · Simulated GPS test point ($lat, $lon). This is detectable as mock data.")
            handler.post(heartbeat)
        } catch (ex: SecurityException) {
            setStatus("Mock-location authorization missing. Select HereShift as the mock-location app in Android Developer Options.")
            stopSelf()
        } catch (ex: Exception) {
            setStatus("Simulation failed: ${ex.javaClass.simpleName}. Check Android Developer Options.")
            stopSelf()
        }
        return START_NOT_STICKY // Never silently resume after reboot or system termination.
    }
    private fun stopProvider(): Boolean {
        active = false
        handler.removeCallbacks(heartbeat)
        if (!ownsTestProvider) return true
        return try {
            manager.removeTestProvider(LocationManager.GPS_PROVIDER)
            ownsTestProvider = false
            true
        } catch (_: Exception) { false }
    }
    override fun onDestroy() {
        val wasActive = active
        val removed = stopProvider()
        if (wasActive) setStatus(if (removed) "Stopped. Android test provider removed."
            else "Stopped; provider cleanup could not be confirmed. Review Android Developer Options.")
        super.onDestroy()
    }
}