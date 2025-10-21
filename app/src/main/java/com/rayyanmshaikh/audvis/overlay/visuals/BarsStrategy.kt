package com.rayyanmshaikh.audvis.overlay.visuals

import android.graphics.Canvas
import android.graphics.Paint
import com.rayyanmshaikh.audvis.overlay.EdgeVisualizerView
import kotlin.math.max

/**
 * Bar visualization similar to equalizer bars along the edge.
 */
class BarsStrategy : VisualizationStrategy {
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
        val barCount = 24
        val spacing = if (isVertical) height / barCount else width / barCount
        if (isVertical) max(1f, width * 0.8f) else max(1f, height * 0.8f)

        for (i in 0 until barCount) {
            val pos = (headIndex + i * historySize / barCount) % historySize
            val amp = amplitudes[pos]
            if (isVertical) {
                val y = i * spacing
                val len = amp * (if (edge == EdgeVisualizerView.Edge.LEFT) width else width)
                if (edge == EdgeVisualizerView.Edge.LEFT) {
                    canvas.drawRect(0f, y, len, y + spacing * 0.8f, paint)
                } else {
                    canvas.drawRect(width - len, y, width, y + spacing * 0.8f, paint)
                }
            } else {
                val x = i * spacing
                val len = amp * (if (edge == EdgeVisualizerView.Edge.TOP) height else height)
                if (edge == EdgeVisualizerView.Edge.TOP) {
                    canvas.drawRect(x, 0f, x + spacing * 0.8f, len, paint)
                } else {
                    canvas.drawRect(x, height - len, x + spacing * 0.8f, height, paint)
                }
            }
        }
    }
}
