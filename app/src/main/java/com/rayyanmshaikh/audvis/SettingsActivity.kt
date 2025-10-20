package com.rayyanmshaikh.audvis

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Provides user-configurable options for the audio visualizer.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var cbLeft: CheckBox
    private lateinit var cbRight: CheckBox
    private lateinit var cbTop: CheckBox
    private lateinit var cbBottom: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize checkboxes
        cbLeft = findViewById(R.id.cbEdgeLeft)
        cbRight = findViewById(R.id.cbEdgeRight)
        cbTop = findViewById(R.id.cbEdgeTop)
        cbBottom = findViewById(R.id.cbEdgeBottom)

        // Load current settings
        loadSettings()

        // Set up listeners to enforce at least one edge selected
        val checkboxes = listOf(cbLeft, cbRight, cbTop, cbBottom)
        checkboxes.forEach { checkbox ->
            checkbox.setOnCheckedChangeListener { _, _ ->
                enforceAtLeastOneEdge()
            }
        }

        // Save button
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveSettings()
        }

        // Back button
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    /**
     * Load current edge settings from preferences
     */
    private fun loadSettings() {
        val config = VisualizerPreferences.loadEdgeConfig(this)
        cbLeft.isChecked = config.left
        cbRight.isChecked = config.right
        cbTop.isChecked = config.top
        cbBottom.isChecked = config.bottom
    }

    /**
     * Save edge settings to preferences
     */
    private fun saveSettings() {
        val config = VisualizerPreferences.EdgeConfig(
            left = cbLeft.isChecked,
            right = cbRight.isChecked,
            top = cbTop.isChecked,
            bottom = cbBottom.isChecked
        )
        VisualizerPreferences.saveEdgeConfig(this, config)
        Toast.makeText(this, "Settings saved. Restart visualizer to apply.", Toast.LENGTH_SHORT).show()
        finish()
    }

    /**
     * Ensure at least one edge is always selected
     */
    private fun enforceAtLeastOneEdge() {
        val hasAtLeastOne = cbLeft.isChecked || cbRight.isChecked || 
                           cbTop.isChecked || cbBottom.isChecked
        
        if (!hasAtLeastOne) {
            // If trying to uncheck the last one, prevent it
            Toast.makeText(this, "At least one edge must be selected", Toast.LENGTH_SHORT).show()
            // Re-check the last one that was checked
            cbLeft.isChecked = true
        }
    }
}
