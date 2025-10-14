package com.rayyanmshaikh.audvis

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.rayyanmshaikh.audvis.overlay.AudioEdgeOverlayService

/**
 * MainActivity for AudVis.
 * Handles UI for starting/stopping the audio edge visualizer overlay,
 * manages overlay permission flow, and prevents duplicate service starts.
 */
class MainActivity : AppCompatActivity() {

    /**
     * Tracks if overlay permission is being requested.
     */
    private var requestingOverlayPermission = false

    /**
     * Tracks if the visualizer service is currently running.
     */
    private var isVisualizerRunning = false

    /**
     * Launcher for MediaProjection screen capture intent.
     * Starts the overlay service if not already running.
     */
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res: ActivityResult ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            if (!isVisualizerRunning) {
                // Start the overlay service with projection permission
                AudioEdgeOverlayService.start(this, res.resultCode, res.data!!)
                isVisualizerRunning = true

            } else {
                Toast.makeText(this, "Visualizer is already running", Toast.LENGTH_SHORT).show()
            }

        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Activity lifecycle: sets up UI and button listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        
        // Start button: checks permissions and starts visualizer if not running
        findViewById<Button>(R.id.btnStart).setOnClickListener { checkAndStartFlow() }
        // Stop button: stops visualizer if running
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            if (isVisualizerRunning) {
                AudioEdgeOverlayService.stop(this)
                isVisualizerRunning = false

            } else {
                Toast.makeText(this, "Visualizer is not running", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Handles returning from overlay permission settings.
     * If permission is granted, continues with screen capture flow.
     */
    override fun onResume() {
        super.onResume()

        //Resume starting after permission
        if (requestingOverlayPermission) {
            requestingOverlayPermission = false

            if (Settings.canDrawOverlays(this)) {
                //Start after version check
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val mpm = getSystemService(MediaProjectionManager::class.java)
                    mediaProjectionLauncher.launch(mpm.createScreenCaptureIntent())
                }

            } else {
                Toast.makeText(this, "Overlay permission is required. Please grant it and try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Checks overlay permission and service state before starting visualizer.
     * Requests permission if needed, otherwise launches screen capture intent.
     */
    private fun checkAndStartFlow() {
        //Check if already running
        if (isVisualizerRunning) {
            Toast.makeText(this, "Visualizer is already running", Toast.LENGTH_SHORT).show()
            return
        }

        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            requestingOverlayPermission = true
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            Toast.makeText(this, "Please enable 'Display over other apps' for AudVis", Toast.LENGTH_LONG).show()

            return
        }

        // Launch screen capture intent for output audio
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mpm = getSystemService(MediaProjectionManager::class.java)
            mediaProjectionLauncher.launch(mpm.createScreenCaptureIntent())

        } else {
            Toast.makeText(this, "Output audio capture requires Android 10+", Toast.LENGTH_LONG).show()
        }
    }
}
