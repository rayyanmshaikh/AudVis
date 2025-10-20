package com.rayyanmshaikh.audvis

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Provides user-configurable options for the audio visualizer.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var cbLeft: CheckBox
    private lateinit var cbRight: CheckBox
    private lateinit var cbTop: CheckBox
    private lateinit var cbBottom: CheckBox
    private lateinit var spStyle: Spinner
    private lateinit var seekThickness: SeekBar
    private lateinit var tvThicknessValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize checkboxes
        cbLeft = findViewById(R.id.cbEdgeLeft)
        cbRight = findViewById(R.id.cbEdgeRight)
        cbTop = findViewById(R.id.cbEdgeTop)
        cbBottom = findViewById(R.id.cbEdgeBottom)
    spStyle = findViewById(R.id.spStyle)
    seekThickness = findViewById(R.id.seekThickness)
    tvThicknessValue = findViewById(R.id.tvThicknessValue)

        // Populate style spinner
        val styles = VisualizerPreferences.Style.entries.toTypedArray()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            styles.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spStyle.adapter = adapter

        // Load current settings
        loadSettings()

        // Configure thickness seekbar range and tick display
        val minDp = VisualizerPreferences.MIN_THICKNESS_DP
        val maxDp = VisualizerPreferences.MAX_THICKNESS_DP
        val currentDp = VisualizerPreferences.loadThicknessDp(this)
        seekThickness.max = maxDp - minDp
        seekThickness.progress = currentDp - minDp
        tvThicknessValue.text = getString(R.string.tv_thickness, currentDp)

        seekThickness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val dp = minDp + progress
                tvThicknessValue.text = getString(R.string.tv_thickness, dp)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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

        // Set style selection
        val currentStyle = VisualizerPreferences.loadStyle(this)
        spStyle.setSelection(VisualizerPreferences.Style.entries.indexOf(currentStyle))
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

        // Save selected style
        val selectedIndex = spStyle.selectedItemPosition
        val selectedStyle = VisualizerPreferences.Style.entries[selectedIndex]
        VisualizerPreferences.saveStyle(this, selectedStyle)

        // Save thickness dp
        val minDp = VisualizerPreferences.MIN_THICKNESS_DP
        val selectedDp = minDp + seekThickness.progress
        VisualizerPreferences.saveThicknessDp(this, selectedDp)
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
