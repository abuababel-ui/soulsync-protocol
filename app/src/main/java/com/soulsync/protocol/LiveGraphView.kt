package com.soulsync.protocol

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class LiveGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maxPoints = 200
    private val data = FloatArray(maxPoints) { 0f }
    private var writeIndex = 0

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D4FF")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A2A4A")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#0F1F3A")
    }

    private val path = Path()

    fun addPoint(value: Float) {
        data[writeIndex] = value
        writeIndex = (writeIndex + 1) % maxPoints
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // الخلفية
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // الشبكة
        val gridStep = width / 10f
        for (i in 1 until 10) {
            canvas.drawLine(i * gridStep, 0f, i * gridStep, height.toFloat(), gridPaint)
        }
        val hStep = height / 6f
        for (i in 1 until 6) {
            canvas.drawLine(0f, i * hStep, width.toFloat(), i * hStep, gridPaint)
        }

        // إيجاد القيمة القصوى
        var maxVal = 0.5f
        for (v in data) {
            if (v > maxVal) maxVal = v
        }

        // رسم الخط
        path.reset()
        val xStep = width.toFloat() / (maxPoints - 1)
        var first = true
        for (i in 0 until maxPoints) {
            val idx = (writeIndex + i) % maxPoints
            val value = data[idx]
            val x = i * xStep
            val y = height - (value / maxVal) * height * 0.9f - height * 0.05f
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, linePaint)
    }
}
