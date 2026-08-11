package com.example.chordfly.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.example.chordfly.music.ChordDetector
import com.example.chordfly.model.ChordSource
import com.example.chordfly.model.ChordTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioChordEngine(
    private val context: Context,
    private val onChord: (ChordTimestamp) -> Unit
) {
    private val detector = ChordDetector()
    private var job: Job? = null
    private var record: AudioRecord? = null
    private var sequence = 0L

    fun start() {
        if (job != null) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return

        val minBuffer = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBuffer * 2).coerceAtLeast(4096)

        record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        record?.startRecording()

        job = CoroutineScope(Dispatchers.Default).launch {
            val buffer = ShortArray(4096)
            var lastChord = ""

            while (isActive) {
                val read = record?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val samples = buffer.copyOf(read)
                    val (chord, confidence) = detector.detect(samples)
                    if (chord != lastChord && chord != "N.C.") {
                        lastChord = chord
                        onChord(
                            ChordTimestamp(
                                id = sequence++,
                                chord = chord,
                                timeSec = 0f,
                                confidence = confidence,
                                source = ChordSource.DSP
                            )
                        )
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        record?.runCatching { stop() }
        record?.release()
        record = null
    }
}
