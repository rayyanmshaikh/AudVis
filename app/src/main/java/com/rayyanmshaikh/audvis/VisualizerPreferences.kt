package com.rayyanmshaikh.audvis

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user preferences for the audio visualizer.
 */
object VisualizerPreferences {
    private const val PREFS_NAME = "audvis_preferences"
    private const val KEY_EDGE_LEFT = "edge_left"
    private const val KEY_EDGE_RIGHT = "edge_right"
    private const val KEY_EDGE_TOP = "edge_top"
    private const val KEY_EDGE_BOTTOM = "edge_bottom"
    private const val KEY_STYLE = "style"
    private const val KEY_THICKNESS_DP = "thickness_dp"
    private const val KEY_RUNNING = "running_state"

    // Thickness bounds in dp
    const val MIN_THICKNESS_DP = 12
    const val MAX_THICKNESS_DP = 96
    private const val DEFAULT_THICKNESS_DP = 32

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Edge configuration data class
     */
    data class EdgeConfig(
        val left: Boolean = true,
        val right: Boolean = false,
        val top: Boolean = false,
        val bottom: Boolean = false
    ) {
        /**
         * Returns true if at least one edge is enabled
         */
        private fun hasAtLeastOneEdge(): Boolean = left || right || top || bottom

        /**
         * Ensures at least one edge is enabled, defaults to left if none are
         */
        fun withAtLeastOneEdge(): EdgeConfig {
            return if (hasAtLeastOneEdge()) this else copy(left = true)
        }
    }

    /**
     * Save edge configuration
     */
    fun saveEdgeConfig(context: Context, config: EdgeConfig) {
        val validated = config.withAtLeastOneEdge()
        getPrefs(context).edit().apply {
            putBoolean(KEY_EDGE_LEFT, validated.left)
            putBoolean(KEY_EDGE_RIGHT, validated.right)
            putBoolean(KEY_EDGE_TOP, validated.top)
            putBoolean(KEY_EDGE_BOTTOM, validated.bottom)
            apply()
        }
    }

    /**
     * Load edge configuration
     */
    fun loadEdgeConfig(context: Context): EdgeConfig {
        val prefs = getPrefs(context)
        return EdgeConfig(
            left = prefs.getBoolean(KEY_EDGE_LEFT, true),
            right = prefs.getBoolean(KEY_EDGE_RIGHT, false),
            top = prefs.getBoolean(KEY_EDGE_TOP, false),
            bottom = prefs.getBoolean(KEY_EDGE_BOTTOM, false)
        ).withAtLeastOneEdge()
    }

    enum class Style { CURVE, BARS, DOTS }

    fun saveStyle(context: Context, style: Style) {
        getPrefs(context).edit().putString(KEY_STYLE, style.name).apply()
    }

    fun loadStyle(context: Context): Style {
        val name = getPrefs(context).getString(KEY_STYLE, Style.CURVE.name)
        return runCatching { Style.valueOf(name!!) }.getOrDefault(Style.CURVE)
    }

    fun saveThicknessDp(context: Context, dp: Int) {
        val clamped = dp.coerceIn(MIN_THICKNESS_DP, MAX_THICKNESS_DP)
        getPrefs(context).edit().putInt(KEY_THICKNESS_DP, clamped).apply()
    }

    fun loadThicknessDp(context: Context): Int {
        return getPrefs(context).getInt(KEY_THICKNESS_DP, DEFAULT_THICKNESS_DP)
            .coerceIn(MIN_THICKNESS_DP, MAX_THICKNESS_DP)
    }

    /** Persist whether the visualizer service is running */
    fun saveRunning(context: Context, running: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_RUNNING, running).apply()
    }

    /** Load running state with default false */
    fun loadRunning(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_RUNNING, false)
    }
}
