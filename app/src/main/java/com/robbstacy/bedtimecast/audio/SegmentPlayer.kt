package com.robbstacy.bedtimecast.audio

import android.media.MediaPlayer
import java.io.File

/** Plays one narration segment at a time; completion drives segment chaining. */
class SegmentPlayer {

    private var player: MediaPlayer? = null

    fun play(file: File, onDone: () -> Unit) {
        stop()
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener { onDone() }
            prepare()
            start()
        }
    }

    fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    /** 0.0–1.0; used by the sleep timer's gentle fade-out. */
    fun setVolume(volume: Float) {
        player?.setVolume(volume, volume)
    }

    fun resume() {
        player?.takeIf { !it.isPlaying }?.start()
    }

    fun stop() {
        player?.release()
        player = null
    }
}
