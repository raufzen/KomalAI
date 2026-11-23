package com.komal.ai.gemini

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class GeminiAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null

    /**
     * Plays Gemini AI native audio output (base64 encoded audio/pcm or audio/mp3/wav)
     */
    fun playBase64Audio(base64Data: String, mimeType: String = "audio/pcm;rate=24000", onDone: (() -> Unit)? = null) {
        try {
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            stop()

            if (mimeType.contains("pcm", ignoreCase = true)) {
                playRawPcm(audioBytes, onDone)
            } else {
                playEncodedAudio(audioBytes, onDone)
            }
        } catch (e: Exception) {
            Log.e("GeminiAudioPlayer", "Error playing Gemini audio", e)
            onDone?.invoke()
        }
    }

    private fun playRawPcm(pcmData: ByteArray, onDone: (() -> Unit)?) {
        try {
            val sampleRate = 24000 // Gemini default audio sample rate
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, pcmData.size)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(audioFormat)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack?.write(pcmData, 0, pcmData.size)
            audioTrack?.setNotificationMarkerPosition(pcmData.size / 2)
            audioTrack?.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(track: AudioTrack?) {}
                override fun onMarkerReached(track: AudioTrack?) {
                    onDone?.invoke()
                }
            })

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("GeminiAudioPlayer", "Error in AudioTrack PCM playback", e)
            onDone?.invoke()
        }
    }

    private fun playEncodedAudio(audioBytes: ByteArray, onDone: (() -> Unit)?) {
        try {
            val tempFile = File.createTempFile("gemini_voice_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener {
                    tempFile.delete()
                    onDone?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    tempFile.delete()
                    onDone?.invoke()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("GeminiAudioPlayer", "Error in MediaPlayer playback", e)
            onDone?.invoke()
        }
    }

    fun stop() {
        try {
            if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.stop()
                audioTrack?.release()
            }
            audioTrack = null

            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // ignore
        }
    }
}
