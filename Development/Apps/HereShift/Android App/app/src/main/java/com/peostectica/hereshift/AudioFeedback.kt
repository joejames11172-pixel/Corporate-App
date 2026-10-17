package com.peostectica.hereshift

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper

/** Audible feedback is local, optional, short, and explicitly user controlled. */
class AudioFeedback(private val context: Context, private val engine: SettingsEngine) {
    private val handler = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var pool = newPool()
    private val samples = mutableMapOf<String, Int>()
    private var currentTheme: String? = null
    private var previewing = false
    private var previewPos = 0
    private var generation = 0
    private var loaded = mutableSetOf<Int>()

    private fun newPool(): SoundPool = SoundPool.Builder().setMaxStreams(2)
        .setAudioAttributes(AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
        .build().also { soundPool ->
            soundPool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) loaded.add(sampleId)
            }
        }

    private fun audible(): Boolean = engine.volumePercent > 0 &&
        audio.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
        audio.getStreamVolume(AudioManager.STREAM_SYSTEM) > 0

    fun loadTheme(id: String) {
        if (currentTheme == id) return
        stopPreview()
        pool.release(); loaded.clear(); pool = newPool(); samples.clear(); currentTheme = id
        val list = Catalog.audio.find { it.id == id }?.cues ?: return
        for (item in list) {
            try {
                context.assets.openFd(item).use { fd ->
                    samples[item] = pool.load(fd, 1)
                }
            } catch (_: Exception) { /* Missing sounds never crash the app. */ }
        }
    }

    fun interaction() {
        if (!audible()) return
        val id = engine.audioId
        if (id == "none") return
        if (currentTheme != id) loadTheme(id)
        play(engine.nextInteractionSound())
    }
    private fun play(path: String?) {
        if (!audible() || path == null) return
        val sampleId = samples[path] ?: return
        if (sampleId !in loaded) return // SoundPool loads asynchronously; never play unloaded assets.
        val level = engine.volumePercent / 100f
        pool.play(sampleId, level, level, 1, 0, 1f)
    }
    fun preview(id: String) {
        stopPreview()
        if (id == "none" || !audible()) return
        loadTheme(id)
        val list = Catalog.audio.find { it.id == id }?.cues ?: return
        if (list.isEmpty()) return
        previewing = true; previewPos = 0
        val thisGeneration = generation
        fun tick() {
            if (!previewing || thisGeneration != generation) return
            play(list[previewPos % list.size]);previewPos += 1
            handler.postDelayed({ tick() }, 820L)
        }
        handler.postDelayed({ tick() }, 350L)
    }
    fun stopPreview() {
        generation += 1; previewing = false
        handler.removeCallbacksAndMessages(null)
        pool.autoPause()
    }
    fun close() { stopPreview(); pool.release(); samples.clear(); loaded.clear() }
}