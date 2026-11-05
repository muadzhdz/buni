package dev.buni.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.sin

object BuniAudio {

    fun playPowerOn() {
        playTone(startFreq = 520.0, endFreq = 940.0, durationMs = 150, volume = 0.85f)
    }

    fun playPowerOff() {
        playTone(startFreq = 840.0, endFreq = 420.0, durationMs = 140, volume = 0.80f)
    }

    private fun playTone(startFreq: Double, endFreq: Double, durationMs: Int, volume: Float) {
        thread(isDaemon = true) {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(numSamples * 2, minBufferSize)
                val buffer = ShortArray(bufferSize / 2)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val currentFreq = startFreq + (endFreq - startFreq) * progress
                    val angle = 2.0 * Math.PI * i / (sampleRate / currentFreq)
                    val attack = if (i < 200) i / 200.0 else 1.0
                    val decay = 1.0 - (i.toDouble() / numSamples)
                    val envelope = attack * decay
                    buffer[i] = (sin(angle) * envelope * Short.MAX_VALUE * volume).toInt().toShort()
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 50)
                track.stop()
                track.release()
            } catch (ignored: Exception) {
            }
        }
    }
}
