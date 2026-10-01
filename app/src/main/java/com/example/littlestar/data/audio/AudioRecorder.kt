package com.example.littlestar.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

/**
 * 마이크 녹음을 MediaRecorder로 수행하는 얇은 래퍼.
 * 사용이 끝나면 반드시 [stop] 또는 [cancel]을 호출해 리소스를 해제한다.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var startRealtimeMs: Long = 0L

    val isRecording: Boolean get() = recorder != null

    /** 녹음을 시작한다. [file]은 부모 디렉터리가 이미 존재해야 한다. */
    fun start(file: File) {
        if (recorder != null) return

        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        newRecorder.setOutputFile(file.absolutePath)
        newRecorder.prepare()
        newRecorder.start()

        recorder = newRecorder
        startRealtimeMs = SystemClock.elapsedRealtime()
    }

    /** 녹음을 정상 종료하고 경과 시간(초)을 반환한다. */
    fun stop(): Int {
        val recorder = recorder ?: return 0
        val durationSeconds = ((SystemClock.elapsedRealtime() - startRealtimeMs) / 1000).toInt()
        runCatching { recorder.stop() }
        recorder.release()
        this.recorder = null
        return durationSeconds
    }

    /** 녹음을 취소하고 리소스를 해제한다(파일은 호출부에서 정리). */
    fun cancel() {
        val recorder = recorder ?: return
        runCatching { recorder.stop() }
        recorder.release()
        this.recorder = null
    }
}
