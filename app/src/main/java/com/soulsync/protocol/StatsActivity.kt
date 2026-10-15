package com.soulsync.protocol

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class StatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        val currentStateText = findViewById<TextView>(R.id.currentStateText)
        val currentStateDesc = findViewById<TextView>(R.id.currentStateDesc)
        val stablePercent = findViewById<TextView>(R.id.stablePercent)
        val inactivePercent = findViewById<TextView>(R.id.inactivePercent)
        val moderatePercent = findViewById<TextView>(R.id.moderatePercent)
        val highPercent = findViewById<TextView>(R.id.highPercent)

        val prefs = getSharedPreferences("soulsync_prefs", MODE_PRIVATE)
        val learningDone = prefs.getBoolean("learning_done", false)

        if (!learningDone) {
            currentStateText.text = "قيد التعلم"
            currentStateText.setTextColor(0xFF8899AA.toInt())
            currentStateDesc.text = "SoulSync is still learning your behavioral baseline"
        } else {
            val baseline = prefs.getFloat("baseline_tremor", 0f)
            val lastStress = prefs.getInt("last_stress_index", 0)

            val state = when {
                lastStress < 30 -> "مستقر"
                lastStress < 60 -> "خامل"
                lastStress < 80 -> "مضطرب"
                else -> "منزعج"
            }
            val desc = when {
                lastStress < 30 -> "Stable - Your baseline is within normal range"
                lastStress < 60 -> "Inactive - Lower activity than usual"
                lastStress < 80 -> "Moderate Stress - Noticeable deviation detected"
                else -> "High Stress - Strong deviation from baseline"
            }
            val color = when {
                lastStress < 30 -> 0xFF00D4FF.toInt()
                lastStress < 60 -> 0xFF8899AA.toInt()
                lastStress < 80 -> 0xFFFFB300.toInt()
                else -> 0xFFFF4444.toInt()
            }

            currentStateText.text = state
            currentStateText.setTextColor(color)
            currentStateDesc.text = desc
        }

        // إحصائيات افتراضية (يمكن ربطها بقاعدة بيانات لاحقاً)
        stablePercent.text = "0%"
        inactivePercent.text = "0%"
        moderatePercent.text = "0%"
        highPercent.text = "0%"
    }
}
