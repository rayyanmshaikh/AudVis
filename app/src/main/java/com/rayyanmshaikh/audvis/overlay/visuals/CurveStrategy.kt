package com.rayyanmshaikh.audvis.overlay.visuals

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.Paint
import com.rayyanmshaikh.audvis.overlay.EdgeVisualizerView
import kotlin.math.pow

/**
 * Original curved fill style.
 */
class CurveStrategy : VisualizationStrategy {
    private val path = Path()

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
        path.reset()
        if (isVertical) drawVertical(canvas, paint, amplitudes, headIndex, width, height, edge)
        else drawHorizontal(canvas, paint, amplitudes, headIndex, width, height, edge)
    }

    private fun drawVertical(
        canvas: Canvas,
        paint: Paint,
        amplitudes: FloatArray,
        headIndex: Int,
        width: Float,
        height: Float,
        edge: EdgeVisualizerView.Edge
    ) {
        val historySize = amplitudes.size
        val step = height / (historySize - 1)

        when (edge) {
            EdgeVisualizerView.Edge.LEFT -> {
                path.moveTo(0f, 0f)
                for (i in 0 until historySize) {
                    val pos = (headIndex + i) % historySize
                    val amp = amplitudes[pos]
                    val y = i * step
                    val inward = amp.pow(0.6f) * (width * 0.9f)
                    path.lineTo(inward, y)
                }
                path.lineTo(0f, height)
            }
            EdgeVisualizerView.Edge.RIGHT -> {
                path.moveTo(width, 0f)
                for (i in 0 until historySize) {
                    val pos = (headIndex + i) % historySize
                    val amp = amplitudes[pos]
                    val y = i * step
                    val inward = amp.pow(0.6f) * (width * 0.9f)
                    path.lineTo(width - inward, y)
                }
                path.lineTo(width, height)
            }
            else -> {}
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawHorizontal(
        canvas: Canvas,
        paint: Paint,
        amplitudes: FloatArray,
        headIndex: Int,
        width: Float,
        height: Float,
        edge: EdgeVisualizerView.Edge
    ) {
        val historySize = amplitudes.size
        val step = width / (historySize - 1)

        when (edge) {
            EdgeVisualizerView.Edge.TOP -> {
                path.moveTo(0f, 0f)
                for (i in 0 until historySize) {
                    val pos = (headIndex + i) % historySize
                    val amp = amplitudes[pos]
                    val x = i * step
                    val inward = amp.pow(0.6f) * (height * 0.9f)
                    path.lineTo(x, inward)
                }
                path.lineTo(width, 0f)
            }
            EdgeVisualizerView.Edge.BOTTOM -> {
                path.moveTo(0f, height)
                for (i in 0 until historySize) {
                    val pos = (headIndex + i) % historySize
                    val amp = amplitudes[pos]
                    val x = i * step
                    val inward = amp.pow(0.6f) * (height * 0.9f)
                    path.lineTo(x, height - inward)
                }
                path.lineTo(width, height)
            }
            else -> {}
        }
        path.close()
        canvas.drawPath(path, paint)
    }
}
