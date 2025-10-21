package com.rayyanmshaikh.audvis.overlay

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.rayyanmshaikh.audvis.MainActivity
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt


class AudioEdgeOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "audvis_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "start"
        private const val ACTION_STOP = "stop"
        private const val EXTRA_RESULT_CODE = "resultCode"
        private const val EXTRA_DATA_INTENT = "dataIntent"

        /**
         * Start the visualization
         */
        fun start(ctx: Context, resultCode: Int, data: Intent) {
            Intent(ctx, AudioEdgeOverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA_INTENT, data)

            }.also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(it)
                else ctx.startService(it)
            }
        }

        /**
         * Stop the visualization
         */
        fun stop(ctx: Context) {
            Intent(ctx, AudioEdgeOverlayService::class.java).apply { action = ACTION_STOP }
                .also { ctx.startService(it) }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val latestAmplitude = AtomicReference(0f)

    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private val visualizerViews = mutableMapOf<String, EdgeVisualizerView>()

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        startRenderLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCapture(intent)
            ACTION_STOP -> fadeOutAndStop()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        captureJob?.cancel()
        audioRecord?.release()
        mediaProjection?.stop()
        scope.cancel()
        removeOverlay()
        super.onDestroy()
    }

    private fun fadeOutAndStop() {
        captureJob?.cancel()
        scope.launch(Dispatchers.Main) {
            repeat(15) {
                latestAmplitude.updateAndGet { it * 0.7f }
                delay(20)
            }
            latestAmplitude.set(0f)
            delay(1000)
            removeOverlay()
            audioRecord?.release()
            mediaProjection?.stop()
            stopSelf()
        }
    }

    private fun startCapture(intent: Intent) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        val code = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val data = intent.getParcelableExtra<Intent>(EXTRA_DATA_INTENT) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaProjection = getSystemService(MediaProjectionManager::class.java).getMediaProjection(code, data)
            startPlaybackCapture()
            attachOverlay()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startPlaybackCapture() {
        val proj = mediaProjection ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val config = AudioPlaybackCaptureConfiguration.Builder(proj)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding) * 2

        //Permission checked in onStartCommand
        audioRecord = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(config)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .setEncoding(encoding).build())
            .setBufferSizeInBytes(bufferSize)
            .build()
            .apply { startRecording() }

        captureJob = scope.launch {
            val buffer = ShortArray(bufferSize / 4)

            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) latestAmplitude.set(computeRms(buffer, read))
            }
        }
    }

    private fun computeRms(data: ShortArray, size: Int): Float {
        val rms = sqrt(data.take(size).sumOf { (it.toInt() * it).toDouble() } / size)
        return (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Attach overlay visualizers based on user preferences
     */
    private fun attachOverlay() {
        if (visualizerViews.isNotEmpty()) return

        val config = com.rayyanmshaikh.audvis.VisualizerPreferences.loadEdgeConfig(this)

        if (config.left)
            attachEdgeOverlay("left", Gravity.START or Gravity.TOP, true, EdgeVisualizerView.Edge.LEFT)

        if (config.right)
            attachEdgeOverlay("right", Gravity.END or Gravity.TOP, true, EdgeVisualizerView.Edge.RIGHT)

        if (config.top)
            attachEdgeOverlay("top", Gravity.TOP or Gravity.START, false, EdgeVisualizerView.Edge.TOP)

        if (config.bottom)
            attachEdgeOverlay("bottom", Gravity.BOTTOM or Gravity.START, false, EdgeVisualizerView.Edge.BOTTOM)
    }

    /**
     * Attach a single edge overlay
     */
    private fun attachEdgeOverlay(edge: String, gravity: Int, isVertical: Boolean,
                                   edgePosition: EdgeVisualizerView.Edge) {
        // Resolve style
        val style = com.rayyanmshaikh.audvis.VisualizerPreferences.loadStyle(this)
        val strategy: com.rayyanmshaikh.audvis.overlay.visuals.VisualizationStrategy = when(style) {
            com.rayyanmshaikh.audvis.VisualizerPreferences.Style.CURVE -> com.rayyanmshaikh.audvis.overlay.visuals.CurveStrategy()
            com.rayyanmshaikh.audvis.VisualizerPreferences.Style.BARS -> com.rayyanmshaikh.audvis.overlay.visuals.BarsStrategy()
            com.rayyanmshaikh.audvis.VisualizerPreferences.Style.DOTS -> com.rayyanmshaikh.audvis.overlay.visuals.DotsStrategy()
        }

        val view = EdgeVisualizerView(this, isVertical = isVertical, edge = edgePosition, strategy = strategy)

        // Convert configured thickness dp to pixels
        val thicknessDp = com.rayyanmshaikh.audvis.VisualizerPreferences.loadThicknessDp(this)
        val density = resources.displayMetrics.density
        val thicknessPx = (thicknessDp * density).toInt().coerceAtLeast(1)

        val params = WindowManager.LayoutParams(
            if (isVertical) thicknessPx else WindowManager.LayoutParams.MATCH_PARENT,
            if (isVertical) WindowManager.LayoutParams.MATCH_PARENT else thicknessPx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT

        ).apply {
            this.gravity = gravity
            // Extend into system bars (status bar, navigation bar)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        windowManager.addView(view, params)
        visualizerViews[edge] = view
    }

    /**
     * Remove all overlay visualizers
     */
    private fun removeOverlay() {
        visualizerViews.values.forEach(windowManager::removeView)

        visualizerViews.clear()
    }

    /**
     * Render loop updates all visualizer views
     */
    private fun startRenderLoop() {
        scope.launch(Dispatchers.Main) {
            while (isActive) {
                val amplitude = latestAmplitude.get()
                visualizerViews.values.forEach { view ->
                    view.updateAmplitude(amplitude)
                }
                delay(16L)
            }
        }
    }

    private fun buildNotification(): Notification {
        ensureChannel()

        val stopPi = PendingIntent.getService(this, 1, Intent(this, AudioEdgeOverlayService::class.java)
            .setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val contentPi = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Edge Visualizer")
            .setContentText("Capturing output audio")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(contentPi)
            .addAction(0, "Stop", stopPi)
            .setOngoing(true)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)

            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Audio Edge", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }
}
