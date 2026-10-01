package com.example.littlestar.data.audio

import android.media.MediaPlayer
import java.io.File

/**
 * 녹음 파일 재생을 MediaPlayer로 수행하는 얇은 래퍼.
 * 재생 완료/중지 시 콜백을 제공하며, 사용이 끝나면 [stop]으로 리소스를 해제한다.
 */
class AudioPlayer {

    private var player: MediaPlayer? = null
    private var onCompletion: (() -> Unit)? = null

    val isPlaying: Boolean get() = player?.isPlaying == true

    /** [file]을 재생한다. 재생이 끝나면 [onFinished]가 호출된다. */
    fun play(file: File, onFinished: () -> Unit) {
        stop()
        onCompletion = onFinished
        val newPlayer = MediaPlayer()
        newPlayer.setDataSource(file.absolutePath)
        newPlayer.setOnCompletionListener {
            stop()
            onFinished()
        }
        newPlayer.prepare()
        newPlayer.start()
        player = newPlayer
    }

    /** 재생을 중지하고 리소스를 해제한다. */
    fun stop() {
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
        onCompletion = null
    }
}
