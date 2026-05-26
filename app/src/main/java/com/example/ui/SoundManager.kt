package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.*
import kotlin.math.sin
import kotlin.math.exp
import kotlin.random.Random

object SoundManager {
    private const val SAMPLE_RATE = 21000
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    var isSfxEnabled = true
    var isBgmEnabled = true
        set(value) {
            field = value
            if (value) {
                startBgmLoop()
            } else {
                stopBgmLoop()
            }
        }

    private var bgmJob: Job? = null

    // Pre-allocated / Cached SFX tracks
    private var clickTrack: AudioTrack? = null
    private var discardTrack: AudioTrack? = null
    private var claimTrack: AudioTrack? = null
    private var diceTrack: AudioTrack? = null
    private var winTrack: AudioTrack? = null

    @Suppress("DEPRECATION")
    private fun createAudioTrack(sizeInBytes: Int, isStatic: Boolean = true): AudioTrack {
        val transferMode = if (isStatic) AudioTrack.MODE_STATIC else AudioTrack.MODE_STREAM
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(sizeInBytes)
                .setTransferMode(transferMode)
                .build()
        } else {
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                sizeInBytes,
                transferMode
            )
        }
    }

    private fun createStaticTrack(pcm: ShortArray): AudioTrack {
        val byteSize = pcm.size * 2
        val track = createAudioTrack(byteSize, isStatic = true)
        track.write(pcm, 0, pcm.size)
        return track
    }

    private fun playStaticTrack(track: AudioTrack) {
        if (!isSfxEnabled) return
        scope.launch {
            try {
                track.stop()
                track.setPlaybackHeadPosition(0)
                track.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Lazy / Safe track retrieval
    private fun getClickTrack(): AudioTrack {
        return clickTrack ?: synchronized(this) {
            clickTrack ?: createStaticTrack(generateClickPcm()).also { clickTrack = it }
        }
    }

    private fun getDiscardTrack(): AudioTrack {
        return discardTrack ?: synchronized(this) {
            discardTrack ?: createStaticTrack(generateDiscardPcm()).also { discardTrack = it }
        }
    }

    private fun getClaimTrack(): AudioTrack {
        return claimTrack ?: synchronized(this) {
            claimTrack ?: createStaticTrack(generateClaimPcm()).also { claimTrack = it }
        }
    }

    private fun getDiceTrack(): AudioTrack {
        return diceTrack ?: synchronized(this) {
            diceTrack ?: createStaticTrack(generateDicePcm()).also { diceTrack = it }
        }
    }

    private fun getWinTrack(): AudioTrack {
        return winTrack ?: synchronized(this) {
            winTrack ?: createStaticTrack(generateWinPcm()).also { winTrack = it }
        }
    }

    // Sound wave synthesizers
    private fun generateClickPcm(): ShortArray {
        val duration = 0.04
        val totalSamples = (SAMPLE_RATE * duration).toInt()
        val pcm = ShortArray(totalSamples)
        val freq = 900.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-120.0 * t)
            val valDouble = sin(2.0 * Math.PI * freq * t) * envelope
            pcm[i] = (valDouble * 16000).toInt().coerceIn(-32767..32767).toShort()
        }
        return pcm
    }

    private fun generateDiscardPcm(): ShortArray {
        val duration = 0.12
        val totalSamples = (SAMPLE_RATE * duration).toInt()
        val pcm = ShortArray(totalSamples)
        val freq1 = 780.0
        val freq2 = 420.0
        val random = Random(42)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env1 = exp(-45.0 * t)
            val env2 = exp(-15.0 * t)
            val noise = (random.nextDouble() * 2.0 - 1.0) * exp(-200.0 * t)
            val tone1 = sin(2.0 * Math.PI * freq1 * t) * env1
            val tone2 = sin(2.0 * Math.PI * freq2 * t) * env2 * 0.4
            val valDouble = (tone1 + tone2 + noise) * 0.75
            pcm[i] = (valDouble * 24000).toInt().coerceIn(-32767..32767).toShort()
        }
        return pcm
    }

    private fun generateClaimPcm(): ShortArray {
        val duration = 0.35
        val totalSamples = (SAMPLE_RATE * duration).toInt()
        val pcm = ShortArray(totalSamples)
        val f1 = 587.33
        val f2 = 739.99
        val f3 = 880.00
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-8.0 * t) * (1.0 - exp(-80.0 * t))
            val valDouble = (sin(2.0 * Math.PI * f1 * t) + 
                             sin(2.0 * Math.PI * f2 * t) * 0.5 + 
                             sin(2.0 * Math.PI * f3 * t) * 0.3) * env * 0.5
            pcm[i] = (valDouble * 25000).toInt().coerceIn(-32767..32767).toShort()
        }
        return pcm
    }

    private fun generateDicePcm(): ShortArray {
        val totalSamples = SAMPLE_RATE * 1
        val pcm = ShortArray(totalSamples)
        val rand = Random(123)
        val tapCount = 10
        val tapIndices = IntArray(tapCount)
        var lastIdx = 0
        for (t in 0 until tapCount) {
            lastIdx += (SAMPLE_RATE * 0.05 + rand.nextInt((SAMPLE_RATE * 0.08).toInt())).toInt()
            if (lastIdx < totalSamples) tapIndices[t] = lastIdx
        }
        for (anchor in tapIndices) {
            if (anchor <= 0) continue
            val clackSamples = (SAMPLE_RATE * 0.03).toInt()
            for (j in 0 until clackSamples) {
                val idx = anchor + j
                if (idx >= totalSamples) break
                val t = j.toDouble() / SAMPLE_RATE
                val env = exp(-120.0 * t)
                val noise = (rand.nextDouble() * 2.0 - 1.0) * exp(-250.0 * t)
                val tone = sin(2.0 * Math.PI * 250.0 * t) * env
                val valDouble = (tone + noise) * 0.5
                pcm[idx] = (pcm[idx] + (valDouble * 15000).toInt()).coerceIn(-32767..32767).toShort()
            }
        }
        return pcm
    }

    private fun generateWinPcm(): ShortArray {
        val notes = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50)
        val totalSamples = (SAMPLE_RATE * 0.75).toInt()
        val pcm = ShortArray(totalSamples)
        for (index in notes.indices) {
            val freq = notes[index]
            val startSample = (SAMPLE_RATE * (index * 0.11)).toInt()
            val noteSamples = (SAMPLE_RATE * 0.15).toInt()
            for (j in 0 until noteSamples) {
                val idx = startSample + j
                if (idx >= totalSamples) break
                val t = j.toDouble() / SAMPLE_RATE
                val env = exp(-12.0 * t) * (1.0 - exp(-150.0 * t))
                val valDouble = sin(2.0 * Math.PI * freq * t) * env * 0.5
                pcm[idx] = (pcm[idx] + (valDouble * 25000).toInt()).coerceIn(-32767..32767).toShort()
            }
        }
        return pcm
    }

    // Playback APIs
    fun playClick() {
        try {
            playStaticTrack(getClickTrack())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playDiscard() {
        try {
            playStaticTrack(getDiscardTrack())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playClaim() {
        try {
            playStaticTrack(getClaimTrack())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playDice() {
        try {
            playStaticTrack(getDiceTrack())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playWin() {
        try {
            playStaticTrack(getWinTrack())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // BGM STREAM Mode Loop (Single track used repeatedly)
    private fun startBgmLoop() {
        if (!isBgmEnabled) return
        stopBgmLoop()
        bgmJob = scope.launch {
            val fG4 = 392.00
            val fA4 = 440.00
            val fC5 = 523.25
            val fD5 = 587.33
            val fE5 = 659.25
            val fG5 = 783.99
            val fA5 = 880.00
            
            val melody = arrayOf(
                Pair(fE5, 2), Pair(fE5, 2), Pair(fG5, 1), Pair(fA5, 1), Pair(fG5, 2),
                Pair(fE5, 2), Pair(fE5, 2), Pair(fG5, 1), Pair(fA5, 1), Pair(fG5, 2),
                Pair(fD5, 2), Pair(fD5, 2), Pair(fE5, 1), Pair(fG5, 1), Pair(fD5, 2),
                Pair(fE5, 1), Pair(fD5, 1), Pair(fC5, 2), Pair(fA4, 2), Pair(fG4, 2),
                
                Pair(fC5, 2), Pair(fC5, 2), Pair(fC5, 2), Pair(fD5, 2),
                Pair(fE5, 2), Pair(fD5, 1), Pair(fC5, 1), Pair(fA4, 2), Pair(fG4, 2),
                Pair(fG4, 2), Pair(fA4, 1), Pair(fC5, 1), Pair(fD5, 2), Pair(fE5, 2),
                Pair(fC5, 2), Pair(fA4, 2), Pair(fG4, 4)
            )

            var track: AudioTrack? = null
            try {
                val minBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val bufSize = (minBufSize * 4).coerceAtLeast(SAMPLE_RATE * 2)
                track = createAudioTrack(bufSize, isStatic = false)
                track.play()

                while (isActive && isBgmEnabled) {
                    for (note in melody) {
                        if (!isActive || !isBgmEnabled) break
                        val freq = note.first
                        val beats = note.second
                        val noteDurationMs = beats * 280L
                        
                        val sampleCount = ((SAMPLE_RATE * noteDurationMs) / 1000).toInt()
                        val pcm = ShortArray(sampleCount)
                        for (i in 0 until sampleCount) {
                            val t = i.toDouble() / SAMPLE_RATE
                            val env = exp(-3.0 * t) * (1.0 - exp(-30.0 * t))
                            val valDouble = sin(2.0 * Math.PI * freq * t) * env * 0.1
                            pcm[i] = (valDouble * 16000).toInt().coerceIn(-32767..32767).toShort()
                        }

                        track.write(pcm, 0, pcm.size)
                        
                        // Sleep to let audio drain, keeping BGM loop perfectly paced
                        delay(noteDurationMs)
                        delay(30)
                    }
                    delay(1200)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (ex: Exception) {}
            }
        }
    }

    private fun stopBgmLoop() {
        bgmJob?.cancel()
        bgmJob = null
    }

    fun release() {
        stopBgmLoop()
        synchronized(this) {
            try { clickTrack?.release(); clickTrack = null } catch(e: Exception) {}
            try { discardTrack?.release(); discardTrack = null } catch(e: Exception) {}
            try { claimTrack?.release(); claimTrack = null } catch(e: Exception) {}
            try { diceTrack?.release(); diceTrack = null } catch(e: Exception) {}
            try { winTrack?.release(); winTrack = null } catch(e: Exception) {}
        }
        scope.cancel()
    }
}
