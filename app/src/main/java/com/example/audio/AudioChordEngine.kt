package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.model.DetectedPitch
import com.example.music.ChromaChordDetector
import com.example.music.ChordSmoother
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioChordEngine {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize = Math.max(
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat),
        4096
    )

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val smoother = ChordSmoother()

    private val _livePitch = MutableStateFlow(DetectedPitch(0f, "Off", "N.C.", 0f))
    val livePitch: StateFlow<DetectedPitch> = _livePitch.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startListening(scope: CoroutineScope) {
        if (_isListening.value) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _livePitch.value = DetectedPitch(0f, "Error", "Mic Unavailable", 0f)
                return
            }

            audioRecord?.startRecording()
            _isListening.value = true
            smoother.reset()

            recordingJob = scope.launch(Dispatchers.Default) {
                val pcmBuffer = ShortArray(2048)
                while (isActive && _isListening.value) {
                    val readSize = audioRecord?.read(pcmBuffer, 0, pcmBuffer.size) ?: 0
                    if (readSize > 0) {
                        val rawPitch = ChromaChordDetector.processAudioBuffer(pcmBuffer, sampleRate)
                        val smoothedPitch = smoother.smooth(rawPitch)
                        _livePitch.value = smoothedPitch
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isListening.value = false
            _livePitch.value = DetectedPitch(0f, "Error", e.localizedMessage ?: "Mic Error", 0f)
        }
    }

    fun stopListening() {
        _isListening.value = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
        _livePitch.value = DetectedPitch(0f, "Off", "N.C.", 0f)
    }
}
