package com.rayyanmshaikh.audvis.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.pow
//CHANGE THIS SO RATHER THAN A LINE THAT CURVES ITS A LINE THAT MAPS TO CERTAIN PITCH? NOTES? AND USE THAT AREA TO BUMP UP
/**
 * Custom view for audio edge visualization.
 * Supports all four screen edges with proper inward curve direction.
 */
class EdgeVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val isVertical: Boolean = true,
    private val edge: Edge = Edge.LEFT
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
        
        if (isVertical) {
            drawVerticalEdge()
        } else {
            drawHorizontalEdge()
        }
        
        // Color shift by latest amplitude (simple mapping blue -> magenta)
        val a = lastAmplitude.coerceIn(0f, 1f)
        val r = (255 * a).toInt()
        val g = (100 * (1 - a)).toInt()
        val b = 200
        paint.color = Color.rgb(r, g, b)

        canvas.drawPath(path, paint)
    }

    /**
     * Draw visualization for vertical edges (left/right)
     * Curves inward toward the center of the screen
     */
    private fun drawVerticalEdge() {
        val w = width.toFloat()
        val h = height.toFloat()
        
        path.reset()

        val step = h / (historySize - 1)
        
        when (edge) {
            Edge.LEFT -> {
                // Start from left edge, curve right (inward)
                path.moveTo(0f, 0f)
                
                for (i in 0 until historySize) {
                    val pos = (index + i) % historySize
                    val amp = amplitudes[pos]
                    val y = i * step
                    val inward = (amp.pow(0.6f)) * (w * 0.9f) // Curve right

                    path.lineTo(inward, y)
                }

                path.lineTo(0f, h)
            }
            Edge.RIGHT -> {
                // Start from right edge, curve left (inward)
                path.moveTo(w, 0f)
                
                for (i in 0 until historySize) {
                    val pos = (index + i) % historySize
                    val amp = amplitudes[pos]
                    val y = i * step
                    val inward = (amp.pow(0.6f)) * (w * 0.9f) // Curve left

                    path.lineTo(w - inward, y)
                }

                path.lineTo(w, h)
            }
            else -> {} // Should not happen for vertical edges
        }
        
        path.close()
    }

    /**
     * Draw visualization for horizontal edges (top/bottom)
     * Curves inward toward the center of the screen
     */
    private fun drawHorizontalEdge() {
        val w = width.toFloat()
        val h = height.toFloat()
        
        path.reset()

        val step = w / (historySize - 1)
        
        when (edge) {
            Edge.TOP -> {
                // Start from top edge, curve down (inward)
                path.moveTo(0f, 0f)
                
                for (i in 0 until historySize) {
                    val pos = (index + i) % historySize
                    val amp = amplitudes[pos]
                    val x = i * step
                    val inward = (amp.pow(0.6f)) * (h * 0.9f) // Curve down

                    path.lineTo(x, inward)
                }

                path.lineTo(w, 0f)
            }
            Edge.BOTTOM -> {
                // Start from bottom edge, curve up (inward)
                path.moveTo(0f, h)
                
                for (i in 0 until historySize) {
                    val pos = (index + i) % historySize
                    val amp = amplitudes[pos]
                    val x = i * step
                    val inward = (amp.pow(0.6f)) * (h * 0.9f) // Curve up

                    path.lineTo(x, h - inward)
                }

                path.lineTo(w, h)
            }
            else -> {} // Should not happen for horizontal edges
        }
        
        path.close()
    }
}
