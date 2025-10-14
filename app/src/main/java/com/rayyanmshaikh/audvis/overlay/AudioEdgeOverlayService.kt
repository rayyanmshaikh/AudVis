package com.rayyanmshaikh.audvis.overlay

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.rayyanmshaikh.audvis.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt


class AudioEdgeOverlayService : Service() {
    companion object {
        private const val CHANNEL_ID = "audvis_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val EDGE_WIDTH_PX = 32
        private const val ACTION_START = "start"
        private const val ACTION_STOP = "stop"
        private const val EXTRA_RESULT_CODE = "resultCode"
        private const val EXTRA_DATA_INTENT = "dataIntent"

        /**
         * Start the visualization
         */
        fun start(ctx: Context, resultCode: Int, data: Intent) {
            val intent = Intent(ctx, AudioEdgeOverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA_INTENT, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else ctx.startService(intent)
        }

        /**
         * Stop the visualization
         */
        fun stop(ctx: Context) {
            val intent = Intent(ctx, AudioEdgeOverlayService::class.java).apply { action = ACTION_STOP }
            ctx.startService(intent)
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private lateinit var windowManager: WindowManager
    private var visualizerView: EdgeVisualizerView? = null
    private val latestAmplitude = AtomicReference(0f)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WindowManager::class.java)
        startForeground(NOTIFICATION_ID, buildNotification())
        startRenderLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                //Permission check only when starting capture
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                val code = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA_INTENT)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && data != null) {
                    val mpm = getSystemService(MediaProjectionManager::class.java)
                    mediaProjection = mpm.getMediaProjection(code, data)
                    startPlaybackCapture()
                    attachOverlay()
                }
            }

            ACTION_STOP -> stopSelf()
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

    @SuppressLint("MissingPermission")
    private fun startPlaybackCapture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val proj = mediaProjection ?: return
        val config = AudioPlaybackCaptureConfiguration.Builder(proj)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val min = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)

        //Permission checked in onStartCommand
        audioRecord = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(config)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(encoding)
                    .setChannelMask(channelConfig)
                    .build()
            ).setBufferSizeInBytes(min * 2)
            .build()

        audioRecord?.startRecording()
        captureJob = scope.launch {
            val shortBuf = ShortArray(min / 2)
            while (isActive) {
                val read = audioRecord?.read(shortBuf, 0, shortBuf.size) ?: -1

                if (read > 0) {
                    val amp = computeRms(shortBuf, read)
                    latestAmplitude.set(amp)
                }
            }
        }
    }

    private fun computeRms(data: ShortArray, size: Int): Float {
        var sum = 0.0

        for (i in 0 until size) {
            val v = data[i].toInt()
            sum += v * v
        }

        val mean = sum / size
        val rms = sqrt(mean) / Short.MAX_VALUE

        return rms.toFloat().coerceIn(0f, 1f)
    }

    private fun attachOverlay() {
        if (visualizerView != null) return

        visualizerView = EdgeVisualizerView(this)

        val params = WindowManager.LayoutParams(
            EDGE_WIDTH_PX,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
        }

        windowManager.addView(visualizerView, params)
    }

    private fun removeOverlay() {
        visualizerView?.let {
            windowManager.removeView(it)
        }

        visualizerView = null
    }

    private fun startRenderLoop() {
        scope.launch(Dispatchers.Main) {
            while (isActive) {
                visualizerView?.updateAmplitude(latestAmplitude.get())
                kotlinx.coroutines.delay(16L)
            }
        }
    }

    private fun buildNotification(): Notification {
        ensureChannel()

        val stopIntent = Intent(this, AudioEdgeOverlayService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val contentIntent = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Edge Visualizer")
            .setContentText("Capturing output audio")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(contentIntent)
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
