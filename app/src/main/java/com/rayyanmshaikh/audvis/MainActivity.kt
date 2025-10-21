package com.rayyanmshaikh.audvis

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.rayyanmshaikh.audvis.overlay.AudioEdgeOverlayService
import com.rayyanmshaikh.audvis.view.MainViewModel

/**
 * MainActivity for AudVis.
 * Handles UI for starting/stopping the audio edge visualizer overlay,
 * manages overlay permission flow, and prevents duplicate service starts.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var toast: Toast? = null
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioEdgeOverlayService.ACTION_STATE) {
                val running = intent.getBooleanExtra(com.rayyanmshaikh.audvis.overlay.AudioEdgeOverlayService.EXTRA_RUNNING, false)
                viewModel.setRunning(running)
            }
        }
    }

    /** Launcher for microphone permission */
    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) checkAllPermissionsAndStart() // continue flow once granted
            else showToast("Microphone permission is required.")

        }

    /** Launcher for overlay permission */
    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) checkAllPermissionsAndStart()
            else showToast("Overlay permission is required.")
        }

    /** Launcher for MediaProjection permission */
    private val mediaProjectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res: ActivityResult ->
            if (res.resultCode == Activity.RESULT_OK && res.data != null) {
                if (!AudioEdgeOverlayService.isRunning(this)) {
                    AudioEdgeOverlayService.start(this, res.resultCode, res.data!!)
                    viewModel.setRunning(true)
                    showToast("Visualizer started")

                } else showToast("Visualizer is already running")

            } else showToast("Screen capture permission denied")

        }

    /**
     * Sets up UI and button listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        viewModel.isVisualizerRunning.observe(this) { running ->
            btnStart.isEnabled = !running
            btnStop.isEnabled = running
        }

        btnStart.setOnClickListener { checkAllPermissionsAndStart() }

        btnStop.setOnClickListener {
            if (AudioEdgeOverlayService.isRunning(this)) {
                AudioEdgeOverlayService.stop(this)
                viewModel.setRunning(false)
                showToast("Visualizer stopped")

            } else showToast("Visualizer is not running")
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

    // Sync initial state using persisted flag (fallback to service check)
    val persistedRunning = VisualizerPreferences.loadRunning(this)
    viewModel.setRunning(persistedRunning || AudioEdgeOverlayService.isRunning(this))
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(stateReceiver, IntentFilter(AudioEdgeOverlayService.ACTION_STATE),
            RECEIVER_NOT_EXPORTED
        )
    // Also resync state on foreground
    val persistedRunning = VisualizerPreferences.loadRunning(this)
    viewModel.setRunning(persistedRunning || AudioEdgeOverlayService.isRunning(this))
    }

    override fun onStop() {
        super.onStop()
        try { unregisterReceiver(stateReceiver) } catch (_: Exception) {}
    }

    /**
     * Checks all required permissions
     * Handles denial and continues flow when granted.
     */
    private fun checkAllPermissionsAndStart() {
        //Microphone permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }

        //Overlay permission
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
            showToast("Please enable 'Display over other apps' for AudVis")

            return
        }

        startMediaProjection()
    }

    /** Launches screen capture permission request */
    private fun startMediaProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mpm = getSystemService(MediaProjectionManager::class.java)
            mediaProjectionLauncher.launch(mpm.createScreenCaptureIntent())

        } else showToast("Output audio capture requires Android 10+")
    }

    /** Reusable Toast helper */
    private fun showToast(msg: String) {
        toast?.cancel()
        toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
        toast?.show()
    }
}

/** Extension for checking if AudioEdgeOverlayService is running */
fun AudioEdgeOverlayService.Companion.isRunning(context: Context): Boolean {
    val mgr = context.getSystemService(ActivityManager::class.java)
    return mgr.getRunningServices(Int.MAX_VALUE)
        .any { it.service.className == AudioEdgeOverlayService::class.qualifiedName }
}
