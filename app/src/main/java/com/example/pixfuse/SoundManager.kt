package com.example.pixfuse

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.pixfuse.R

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<SoundType, Int>()
    private var bgmPlayer: MediaPlayer? = null

    enum class SoundType {
        SHOOT, EXPLOSION, GAME_OVER, WALL_HIT
    }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(5)
            .build()

        // Load hiệu ứng
        soundMap[SoundType.SHOOT] = soundPool.load(context, R.raw.click, 1)
        soundMap[SoundType.EXPLOSION] = soundPool.load(context, R.raw.crash, 1)
        soundMap[SoundType.GAME_OVER] = soundPool.load(context, R.raw.landingsuccess, 1)
        soundMap[SoundType.WALL_HIT] = soundPool.load(context, R.raw.thruster, 1)

        // Nhạc nền
        bgmPlayer = MediaPlayer.create(context, R.raw.music)
        bgmPlayer?.isLooping = true
    }

    fun playSound(type: SoundType) {
        val id = soundMap[type] ?: return
        soundPool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun playBGM() {
        bgmPlayer?.start()
    }

    fun stopBGM() {
        bgmPlayer?.pause()
    }

    fun release() {
        soundPool.release()
        bgmPlayer?.release()
        bgmPlayer = null
    }
}
