package com.rayyanmshaikh.audvis.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.pow

class EdgeVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val historySize = 60
    private val amplitudes = FloatArray(historySize) { 0f }
    private var index = 0

    private val path = Path()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.CYAN
    }

    private var lastAmplitude = 0f

    fun updateAmplitude(value: Float) {
        val smoothed = 0.4f * value + 0.6f * lastAmplitude
        lastAmplitude = smoothed
        amplitudes[index] = smoothed
        index = (index + 1) % historySize

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        
        path.reset()

        val step = h / (historySize - 1)
        for (i in 0 until historySize) {
            val pos = (index + i) % historySize
            val amp = amplitudes[pos]
            val y = i * step
            val inward = (amp.pow(0.6f)) * (w * 0.9f) // curve inward

            if (i == 0) path.moveTo(w, 0f)

            path.lineTo(w - inward, y)
        }

        path.lineTo(w, h)
        path.close()

        // Color shift by latest amplitude (simple mapping blue -> magenta)
        val a = lastAmplitude.coerceIn(0f, 1f)
        val r = (255 * a).toInt()
        val g = (100 * (1 - a)).toInt()
        val b = 200
        paint.color = Color.rgb(r, g, b)

        canvas.drawPath(path, paint)
    }
}
