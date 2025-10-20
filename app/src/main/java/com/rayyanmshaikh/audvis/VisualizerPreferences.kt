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
}
