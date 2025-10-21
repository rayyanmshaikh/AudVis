package com.rayyanmshaikh.audvis.overlay.visuals

import android.graphics.Canvas
import android.graphics.Paint
import com.rayyanmshaikh.audvis.overlay.EdgeVisualizerView

/**
 * Dots visualization that places dots that move inward with amplitude.
 */
class DotsStrategy : VisualizationStrategy {
    override fun draw(
        canvas: Canvas,
        paint: Paint,
        amplitudes: FloatArray,
        headIndex: Int,
        width: Float,
        height: Float,
        isVertical: Boolean,
        edge: EdgeVisualizerView.Edge,
    ) {
        val historySize = amplitudes.size
        val dotCount = 32
        val step = if (isVertical) height / (dotCount + 1) else width / (dotCount + 1)
        val radius = if (isVertical) (width * 0.25f) else (height * 0.25f)

        for (i in 0 until dotCount) {
            val pos = (headIndex + i * historySize / dotCount) % historySize
            val amp = amplitudes[pos]
            if (isVertical) {
                val y = (i + 1) * step
                val inward = amp * (width * 0.9f)
                val cx = when (edge) {
                    EdgeVisualizerView.Edge.LEFT -> inward
                    EdgeVisualizerView.Edge.RIGHT -> width - inward
                    else -> 0f
                }
                canvas.drawCircle(cx, y, radius, paint)
            } else {
                val x = (i + 1) * step
                val inward = amp * (height * 0.9f)
                val cy = when (edge) {
                    EdgeVisualizerView.Edge.TOP -> inward
                    EdgeVisualizerView.Edge.BOTTOM -> height - inward
                    else -> 0f
                }
                canvas.drawCircle(x, cy, radius, paint)
            }
        }
    }
}
