package com.soulsync.protocol

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val titleText = findViewById<TextView>(R.id.titleText)
        val arabicTagline = findViewById<TextView>(R.id.arabicTagline)
        val progressBar = findViewById<ProgressBar>(R.id.initProgress)

        // تأثير Fade In للعنوان
        titleText.alpha = 0f
        titleText.animate().alpha(1f).setDuration(1500).start()

        // تأثير Fade In للشعار العربي (متأخر قليلاً)
        arabicTagline.alpha = 0f
        Handler(Looper.getMainLooper()).postDelayed({
            arabicTagline.animate().alpha(1f).setDuration(1200).start()
        }, 800)

        // شريط التحميل يتحرك من 0 إلى 100 خلال 3 ثوان
        ObjectAnimator.ofInt(progressBar, "progress", 0, 100).apply {
            duration = 3000
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // الانتقال للتطبيق الرئيسي بعد 3.5 ثوان
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3500)
    }
}
