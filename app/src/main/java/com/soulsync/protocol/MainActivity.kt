package com.soulsync.protocol

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var reboundText: TextView
    private lateinit var stressText: TextView
    private lateinit var statusText: TextView
    private lateinit var countdownText: TextView
    private lateinit var learningDescText: TextView

    private var filteredValue = 0f
    private val alpha = 0.9f
    private val deadZone = 0.02f

    private val uiHandler = Handler(Looper.getMainLooper())
    private val LEARNING_DURATION_MS = 60L * 60L * 1000L // ساعة واحدة

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        reboundText = findViewById(R.id.reboundText)
        stressText = findViewById(R.id.stressText)
        statusText = findViewById(R.id.statusText)
        countdownText = findViewById(R.id.countdownText)
        learningDescText = findViewById(R.id.learningDescText)
        val startButton = findViewById<Button>(R.id.startButton)

        // صلاحيات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101
                )
            }
        }

        requestBatteryExemption()

        // المستشعرات
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        startButton.setOnClickListener {
            val intent = Intent(this, SoulSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            startButton.text = "MONITORING ACTIVE"
            startButton.isEnabled = false
        }

        uiHandler.post(statusUpdater)
    }

    private val statusUpdater = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences("soulsync_prefs", MODE_PRIVATE)
            val learningStart = prefs.getLong("learning_start", 0L)
            val learningDone = prefs.getBoolean("learning_done", false)
            val baseTremor = prefs.getFloat("baseline_tremor", 0f)

            if (learningStart == 0L) {
                // لم يبدأ بعد
                statusText.text = "Ready to learn your language"
                countdownText.visibility = TextView.GONE
                learningDescText.visibility = TextView.GONE
            } else if (!learningDone) {
                // قيد التعلم
                val elapsed = System.currentTimeMillis() - learningStart
                val remaining = (LEARNING_DURATION_MS - elapsed).coerceAtLeast(0L)

                val minutes = remaining / 60000
                val seconds = (remaining % 60000) / 1000
                countdownText.text = String.format("%02d:%02d", minutes, seconds)
                countdownText.visibility = TextView.VISIBLE
                learningDescText.visibility = TextView.VISIBLE

                statusText.text = "🧠 Learning your unique behavioral language..."
            } else {
                // انتهى التعلم
                statusText.text = "✅ Baseline learned. SoulSync understands you."
                countdownText.text = "READY"
                countdownText.textSize = 32f
                countdownText.visibility = TextView.VISIBLE
                learningDescText.visibility = TextView.GONE
            }
            uiHandler.postDelayed(this, 1000)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt(x * x + y * y + z * z)
        val cleaned = if (magnitude < deadZone) 0f else magnitude
        filteredValue = alpha * filteredValue + (1 - alpha) * cleaned

        reboundText.text = String.format("%.2f", filteredValue)

        val prefs = getSharedPreferences("soulsync_prefs", MODE_PRIVATE)
        val base = prefs.getFloat("baseline_tremor", 0f)
        val stdDev = prefs.getFloat("baseline_stddev", 0.1f).coerceAtLeast(0.01f)

        if (base > 0f) {
            val deviation = abs(filteredValue - base) / stdDev
            val stressIndex = (deviation * 20).toInt().coerceIn(0, 100)
            val level = when {
                stressIndex < 30 -> "Calm"
                stressIndex < 60 -> "Mild"
                stressIndex < 80 -> "Moderate"
                else -> "High"
            }
            stressText.text = "Stress Index: $stressIndex ($level)"
            stressText.setTextColor(
                when (level) {
                    "Calm" -> 0xFF00D4FF.toInt()
                    "Mild" -> 0xFF7FDB3A.toInt()
                    "Moderate" -> 0xFFFFB300.toInt()
                    else -> 0xFFFF4444.toInt()
                }
            )
        } else {
            stressText.text = "Stress Index: learning..."
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        uiHandler.removeCallbacks(statusUpdater)
    }

    private fun requestBatteryExemption() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
