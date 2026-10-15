package com.soulsync.protocol

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


class NeuralNetworkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#00D4FF")
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2000D4FF")
    }

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#00D4FF")
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#4000D4FF")
    }

    private var phase = 0f

    private data class Particle(
        val orbitRadius: Float,
        val angle: Float,
        val speed: Float,
        val size: Float,
        val orbitTilt: Float
    )

    private val particles = mutableListOf<Particle>()
    private var initialized = false

  
private fun initParticles() {
    if (initialized) return
    initialized = true
    val rand = Random(System.currentTimeMillis())
    for (i in 0 until 14) {
        particles.add(
            Particle(
                orbitRadius = 0.35f + rand.nextFloat() * 0.55f,
                angle = rand.nextFloat() * 360f,
                speed = 8f + rand.nextFloat() * 18f,
                size = 4f + rand.nextFloat() * 6f,
                orbitTilt = -0.3f + rand.nextFloat() * 0.6f
            )
        )
    }
}

private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
    duration = 20000
    repeatCount = ValueAnimator.INFINITE
    interpolator = LinearInterpolator()
    addUpdateListener {
        phase = it.animatedValue as Float
        invalidate()
    }
}

override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    initParticles()
    animator.start()
}

override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    animator.cancel()
}


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val maxR = (width.coerceAtMost(height) / 2f) - 30f

        // 3 دوائر نابضة
        for (i in 0 until 3) {
            val offset = i * 0.33f
            val p = ((phase / 360f) + offset) % 1f
            val radius = maxR * (0.35f + p * 0.65f)
            val alpha = ((1f - p) * 180).toInt().coerceIn(0, 255)

            ringPaint.alpha = alpha
            canvas.drawCircle(cx, cy, radius, ringPaint)
        }

        // دوائر ثابتة (إطار تقني)
        ringPaint.alpha = 100
        canvas.drawCircle(cx, cy, maxR * 0.55f, ringPaint)
        ringPaint.alpha = 60
        canvas.drawCircle(cx, cy, maxR * 0.85f, ringPaint)

        // توهج مركزي
        glowPaint.alpha = 40
        canvas.drawCircle(cx, cy, maxR * 0.35f, glowPaint)

        // الجزيئات
        val particlePositions = mutableListOf<Pair<Float, Float>>()
        for (p in particles) {
            val angleRad = Math.toRadians((p.angle + phase * p.speed / 20f).toDouble())
            val r = maxR * p.orbitRadius
            val x = cx + (r * cos(angleRad)).toFloat()
            val y = cy + (r * sin(angleRad)).toFloat() + (r * p.orbitTilt)

            particlePositions.add(x to y)

            particlePaint.alpha = 200
            canvas.drawCircle(x, y, p.size, particlePaint)

            glowPaint.alpha = 60
            canvas.drawCircle(x, y, p.size * 2.5f, glowPaint)
        }

        // خطوط تربط الجزيئات المتقاربة
        for (i in particlePositions.indices) {
            for (j in i + 1 until particlePositions.size) {
                val (x1, y1) = particlePositions[i]
                val (x2, y2) = particlePositions[j]
                val dx = x1 - x2
                val dy = y1 - y2
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist < maxR * 0.5f) {
                    val alpha = ((1f - dist / (maxR * 0.5f)) * 100).toInt()
                    linePaint.alpha = alpha.coerceIn(0, 255)
                    canvas.drawLine(x1, y1, x2, y2, linePaint)
                }
            }
        }
    }
}
