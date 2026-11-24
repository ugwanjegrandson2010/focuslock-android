package com.focuslock.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import com.focuslock.app.data.MusicProfileEntity
import com.focuslock.app.domain.MusicSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicController(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: MediaPlayer? = null
    private var fileDescriptor: android.os.ParcelFileDescriptor? = null
    private var fadeJob: Job? = null
    private var targetVolume = 0.7f
    private var focusRequest: AudioFocusRequest? = null

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player?.pause()
            AudioManager.AUDIOFOCUS_GAIN -> try { if (player?.isPlaying == false) player?.start() } catch (e: Exception) {}
        }
    }

    fun play(profile: MusicProfileEntity?) {
        if (profile == null) return
        stopImmediate()
        targetVolume = profile.volume.coerceIn(0f, 1f)
        val mp = MediaPlayer()
        player = mp
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        mp.isLooping = true
        mp.setVolume(0f, 0f)
        try {
            when (MusicSource.valueOf(profile.source)) {
                MusicSource.BUNDLED -> {
                    context.assets.openFd(profile.location).use { afd ->
                        mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    }
                }
                MusicSource.LOCAL -> {
                    val uri = Uri.parse(profile.location)
                    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    fileDescriptor = pfd
                    if (pfd == null) { release(); return }
                    mp.setDataSource(pfd.fileDescriptor)
                }
                MusicSource.STREAMING -> {
                    if (!profile.location.startsWith("http://") && !profile.location.startsWith("https://")) {
                        release(); return
                    }
                    mp.setDataSource(profile.location)
                }
            }
            requestFocus()
            mp.setOnPreparedListener {
                try {
                    it.start()
                    fadeTo(targetVolume, 2000)
                } catch (e: Exception) {}
            }
            mp.setOnErrorListener { _, _, _ -> release(); true }
            mp.prepareAsync()
        } catch (e: Exception) {
            release()
        }
    }

    fun stopWithFade() {
        fadeJob?.cancel()
        val current = player ?: return
        fadeJob = scope.launch {
            val start = current.getVolumeApprox()
            for (step in 10 downTo 0) {
                val v = start * (step / 10f)
                try { current.setVolume(v, v) } catch (e: Exception) { break }
                delay(150)
            }
            release()
        }
    }

    fun stopImmediate() {
        fadeJob?.cancel()
        release()
    }

    private fun MediaPlayer.getVolumeApprox(): Float = targetVolume

    private fun fadeTo(target: Float, durationMs: Long) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 20
            for (i in 0..steps) {
                val v = target * (i / steps.toFloat())
                try { player?.setVolume(v, v) } catch (e: Exception) { break }
                delay(durationMs / steps)
            }
        }
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
                )
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun release() {
        try { player?.stop() } catch (e: Exception) {}
        try { player?.release() } catch (e: Exception) {}
        player = null
        try { fileDescriptor?.close() } catch (e: Exception) {}
        fileDescriptor = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { try { audioManager.abandonAudioFocusRequest(it) } catch (e: Exception) {} }
        } else {
            @Suppress("DEPRECATION")
            try { audioManager.abandonAudioFocus(focusListener) } catch (e: Exception) {}
        }
        focusRequest = null
    }
}
