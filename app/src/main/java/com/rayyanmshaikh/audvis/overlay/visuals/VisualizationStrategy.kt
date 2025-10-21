package com.rayyanmshaikh.audvis.overlay.visuals

import android.graphics.Canvas
import android.graphics.Paint
import com.rayyanmshaikh.audvis.overlay.EdgeVisualizerView

/**
 * Strategy interface for rendering different visualization styles.
 */
interface VisualizationStrategy {
    fun draw(
        canvas: Canvas,
        paint: Paint,
        amplitudes: FloatArray,
        headIndex: Int,
        width: Float,
        height: Float,
        isVertical: Boolean,
        edge: EdgeVisualizerView.Edge,
    )
}
