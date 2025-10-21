package com.rayyanmshaikh.audvis.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.rayyanmshaikh.audvis.overlay.visuals.VisualizationStrategy
import com.rayyanmshaikh.audvis.overlay.visuals.CurveStrategy

/**
 * Custom view for audio edge visualization.
 * Delegates drawing to a pluggable VisualizationStrategy
 */
class EdgeVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val isVertical: Boolean = true,
    private val edge: Edge = Edge.LEFT,
    private val strategy: VisualizationStrategy = CurveStrategy()
) : View(context, attrs) {

    /**
     * Enum representing which screen edge this view is on
     */
    enum class Edge {
        LEFT, RIGHT, TOP, BOTTOM
    }

    private val historySize = 60
    private val amplitudes = FloatArray(historySize) { 0f }
    private var index = 0

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

        // Color shift by latest amplitude (simple mapping blue -> magenta)
        val a = lastAmplitude.coerceIn(0f, 1f)
        val r = (255 * a).toInt()
        val g = (100 * (1 - a)).toInt()
        val b = 200
        paint.color = Color.rgb(r, g, b)

        strategy.draw(
            canvas = canvas,
            paint = paint,
            amplitudes = amplitudes,
            headIndex = index,
            width = width.toFloat(),
            height = height.toFloat(),
            isVertical = isVertical,
            edge = edge
        )
    }
}
