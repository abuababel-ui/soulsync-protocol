package com.soulsync.protocol

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class SoulSyncService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var wakeLock: PowerManager.WakeLock? = null

    private var filteredValue = 0f
    private val alpha = 0.9f
    private val deadZone = 0.02f

    private val learningSamples = mutableListOf<Float>()
    private var learningStartTime: Long = 0
    private val learningDuration = 60L * 60L * 1000L

    companion object {
        private const val CHANNEL_ID = "soulsync_channel"
        private const val NOTIF_ID = 1001
        private const val TAG = "SoulSyncService"
        private const val PREFS = "soulsync_prefs"
        private const val KEY_LEARNING_START = "learning_start"
        private const val KEY_BASELINE = "baseline_tremor"
        private const val KEY_LEARNING_DONE = "learning_done"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SoulSync::Lock")
        wakeLock?.setReferenceCounted(false)
        wakeLock?.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Silent learning... (1 hour)"))

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (prefs.getLong(KEY_LEARNING_START, 0L) == 0L) {
            prefs.edit().putLong(KEY_LEARNING_START, System.currentTimeMillis()).apply()
        }
        learningStartTime = prefs.getLong(KEY_LEARNING_START, System.currentTimeMillis())

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (sensor == null) {
            Log.e(TAG, "Linear Acceleration sensor not available")
        } else {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt(x * x + y * y + z * z)
        val cleaned = if (magnitude < deadZone) 0f else magnitude
        filteredValue = alpha * filteredValue + (1 - alpha) * cleaned

        val now = System.currentTimeMillis()
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        if (!prefs.getBoolean(KEY_LEARNING_DONE, false)) {
            if (now - learningStartTime < learningDuration) {
                if (filteredValue > deadZone) {
                    learningSamples.add(filteredValue)
                }
            } else {
                finalizeLearning(prefs)
            }
        }
    }

    private fun finalizeLearning(prefs: android.content.SharedPreferences) {
        if (learningSamples.isEmpty()) {
            prefs.edit().putLong(KEY_LEARNING_START, System.currentTimeMillis()).apply()
            learningStartTime = System.currentTimeMillis()
            return
        }
        val avg = learningSamples.average().toFloat()
        prefs.edit()
            .putFloat(KEY_BASELINE, avg)
            .putBoolean(KEY_LEARNING_DONE, true)
            .apply()
        learningSamples.clear()
        updateNotification("Baseline learned. Monitoring active.")
        Log.i(TAG, "Baseline learned: $avg")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "SoulSync Protocol",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Bio-behavioral monitoring"
                setShowBadge(false)
            }
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SoulSync Protocol")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        try { sensorManager.unregisterListener(this) } catch (_: Exception) {}
        wakeLock?.let { if (it.isHeld) it.release() }

        try {
            val restartIntent = Intent(applicationContext, SoulSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartIntent)
            } else {
                applicationContext.startService(restartIntent)
            }
        } catch (_: Exception) {}
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            val restartIntent = Intent(applicationContext, SoulSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartIntent)
            }
        } catch (_: Exception) {}
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
