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
    private const val SAMPLE_RATE = 22050
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

    // System helper to create AudioTrack robustly
    @Suppress("DEPRECATION")
    private fun createAudioTrack(sizeInBytes: Int): AudioTrack {
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
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } else {
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                sizeInBytes,
                AudioTrack.MODE_STATIC
            )
        }
    }

    // Helper to play synthesized 16-bit PCM buffer statically
    private fun playPcm(pcm: ShortArray) {
        if (!isSfxEnabled) return
        scope.launch {
            try {
                val byteSize = pcm.size * 2
                val track = createAudioTrack(byteSize)
                track.write(pcm, 0, pcm.size)
                track.play()
                // Wait for playback to complete, then release
                val durationMs = (pcm.size * 1000L) / SAMPLE_RATE
                delay(durationMs + 100)
                track.stop()
                track.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 1. Click sound effect (Quick high-pitch tap)
    fun playClick() {
        if (!isSfxEnabled) return
        val duration = 0.04 // 40ms
        val totalSamples = (SAMPLE_RATE * duration).toInt()
        val pcm = ShortArray(totalSamples)
        val freq = 900.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-120.0 * t)
            val valDouble = sin(2.0 * Math.PI * freq * t) * envelope
            pcm[i] = (valDouble * 16000).toInt().coerceIn(-32767..32767).toShort()
        }
        playPcm(pcm)
    }

    // 2. Discard Tile Clack sound (Dull wooden tile hit)
    fun playDiscard() {
        if (!isSfxEnabled) return
        val duration = 0.12 // 120ms
        val totalSamples = (SAMPLE_RATE * duration).toInt()
        val pcm = ShortArray(totalSamples)
        // High impact start followed by two decaying resonators (wood-like pitch)
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
        playPcm(pcm)
    }

    // 3. Claim Alert chime (For Peng / Gang / Hu / Alert)
    fun playClaim() {
        if (!isSfxEnabled) return
        val duration = 0.35 // 350ms
        val totalSamples = (SAMPLE_RATE * duration).toInt()
        val pcm = ShortArray(totalSamples)
        // High-quality major 3rd chime (soft bell-like frequencies)
        val f1 = 587.33 // D5
        val f2 = 739.99 // F#5
        val f3 = 880.00 // A5
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-8.0 * t) * (1.0 - exp(-80.0 * t)) // Soft attack, decay
            val valDouble = (sin(2.0 * Math.PI * f1 * t) + 
                             sin(2.0 * Math.PI * f2 * t) * 0.5 + 
                             sin(2.0 * Math.PI * f3 * t) * 0.3) * env * 0.5
            pcm[i] = (valDouble * 25000).toInt().coerceIn(-32767..32767).toShort()
        }
        playPcm(pcm)
    }

    // 4. Dice Roll sound effects (series of bouncing wood/plastic clicks)
    fun playDice() {
        if (!isSfxEnabled) return
        // We synthesize a longer sequence of short taps
        val totalSamples = SAMPLE_RATE * 1 // 1.0 second duration
        val pcm = ShortArray(totalSamples)
        val rand = Random(123)
        // Set taps at randomized index interval points
        val tapCount = 10
        val tapIndices = IntArray(tapCount)
        var lastIdx = 0
        for (t in 0 until tapCount) {
            lastIdx += (SAMPLE_RATE * 0.05 + rand.nextInt((SAMPLE_RATE * 0.08).toInt())).toInt()
            if (lastIdx < totalSamples) tapIndices[t] = lastIdx
        }

        for (i in 0 until totalSamples) {
            pcm[i] = 0
        }

        // Overlay small clacks at each index
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
        playPcm(pcm)
    }

    // 5. Win Fanfare (Exciting ascending major pentatonic flourish)
    fun playWin() {
        if (!isSfxEnabled) return
        scope.launch {
            val notes = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50) // C5, D5, E5, G5, A5, C6
            for (freq in notes) {
                val duration = 0.15 // 150ms per arpeggio note
                val totalSamples = (SAMPLE_RATE * duration).toInt()
                val pcm = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val env = exp(-12.0 * t) * (1.0 - exp(-150.0 * t))
                    val valDouble = sin(2.0 * Math.PI * freq * t) * env * 0.5
                    pcm[i] = (valDouble * 25000).toInt().coerceIn(-32767..32767).toShort()
                }
                playPcm(pcm)
                delay(120)
            }
        }
    }

    // Background Music (BGM) - Soft traditional Chinese pentatonic melody (Jasmine Flower)
    // played sequentially note-by-note with soft sine tones.
    private fun startBgmLoop() {
        if (!isBgmEnabled) return
        stopBgmLoop() // Ensure clean start
        bgmJob = scope.launch {
            // Jasmine Flower Pentatonic melody defined by frequencies
            // 5 = Sol, 6 = La, 1 = Do, 2 = Re, 3 = Mi
            val fG4 = 392.00
            val fA4 = 440.00
            val fC5 = 523.25
            val fD5 = 587.33
            val fE5 = 659.25
            val fG5 = 783.99
            val fA5 = 880.00
            
            // Pairs of (Frequency, duration in beat units). 1 unit = 250ms (for a 120 bpm tempo)
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

            while (isActive) {
                for (note in melody) {
                    if (!isActive || !isBgmEnabled) break
                    val freq = note.first
                    val beats = note.second
                    val noteDurationMs = beats * 280L
                    
                    // Synthesize soft background chime note (extremely gentle sine wave to be soothing)
                    val sampleCount = ((SAMPLE_RATE * noteDurationMs) / 1000).toInt()
                    val pcm = ShortArray(sampleCount)
                    for (i in 0 until sampleCount) {
                        val t = i.toDouble() / SAMPLE_RATE
                        // Soft envelope, slow fade
                        val env = exp(-3.0 * t) * (1.0 - exp(-30.0 * t))
                        val valDouble = sin(2.0 * Math.PI * freq * t) * env * 0.12 // Gentle low volume melody
                        pcm[i] = (valDouble * 16000).toInt().coerceIn(-32767..32767).toShort()
                    }

                    // Play this brief note background-friendly
                    if (isBgmEnabled && isActive) {
                        try {
                            val byteSize = pcm.size * 2
                            val track = createAudioTrack(byteSize)
                            track.write(pcm, 0, pcm.size)
                            track.play()
                            delay(noteDurationMs)
                            track.stop()
                            track.release()
                        } catch (e: Exception) {
                            delay(noteDurationMs)
                        }
                    } else {
                        break
                    }
                    delay(30) // Little gap between notes
                }
                delay(1200) // 1.2s break between loop lines
            }
        }
    }

    private fun stopBgmLoop() {
        bgmJob?.cancel()
        bgmJob = null
    }

    fun release() {
        stopBgmLoop()
        scope.cancel()
    }
}
