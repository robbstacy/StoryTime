package com.robbstacy.bedtimecast.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Records one narration segment to an AAC .m4a file. */
class SegmentRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null

    var isRecording: Boolean = false
        private set

    fun start(output: File) {
        stop()
        output.parentFile?.mkdirs()
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(128_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(output.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        isRecording = true
    }

    fun stop() {
        recorder?.let { r ->
            runCatching { r.stop() }
            r.release()
        }
        recorder = null
        isRecording = false
    }
}
