package com.example.chordfly.ai

import com.example.chordfly.model.ChordAnalysis
import com.example.chordfly.model.ChordSource
import com.example.chordfly.model.ChordTimestamp
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeBackend
import com.google.firebase.ai.type.GenerationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gemini is used as a correction/interpretation layer over DSP observations.
 *
 * Important: do not expose a raw Gemini API key in the Android app.
 * Firebase AI Logic is used as the client-side gateway.
 */
class GeminiChordAnalyzer {

    private val model = FirebaseAI
        .getInstance(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = "gemini-3.5-flash",
            generationConfig = GenerationConfig.builder()
                .setTemperature(0.1f)
                .build()
        )

    suspend fun refine(
        title: String,
        artist: String,
        observations: List<ChordTimestamp>
    ): Result<ChordAnalysis> = withContext(Dispatchers.IO) {
        runCatching {
            val compact = observations.joinToString(",") {
                """{"time":${"%.2f".format(it.timeSec)},"chord":"${it.chord}","confidence":${"%.2f".format(it.confidence)}}"""
            }

            val prompt = """
                You are a professional music chord analyst.
                Correct the chord observations below.

                Song title: $title
                Artist: $artist

                Rules:
                - Return ONLY valid JSON.
                - Keep timestamps in seconds.
                - Do not invent timestamps.
                - Prefer practical guitar/piano chord names.
                - Use slash chords only when clearly justified.
                - You may merge unstable repeated detections.
                - Preserve the musical order.
                - confidence must be 0.0..1.0.

                JSON schema:
                {
                  "title": "string",
                  "artist": "string",
                  "key": "string or null",
                  "bpm": "integer or null",
                  "chords": [
                    {"time": 0.0, "chord": "C", "confidence": 0.9}
                  ]
                }

                Observations:
                [$compact]
            """.trimIndent()

            val response = model.generateContent(prompt)
            val text = response.text ?: error("Gemini returned no text")
            parse(text)
        }
    }

    private fun parse(raw: String): ChordAnalysis {
        val cleaned = raw
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val root = JSONObject(cleaned)
        val array = root.optJSONArray("chords") ?: JSONArray()
        val chords = buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    ChordTimestamp(
                        id = i.toLong(),
                        chord = item.optString("chord", "N.C."),
                        timeSec = item.optDouble("time", 0.0).toFloat(),
                        confidence = item.optDouble("confidence", 0.0).toFloat(),
                        source = ChordSource.GEMINI
                    )
                )
            }
        }

        return ChordAnalysis(
            title = root.optString("title", ""),
            artist = root.optString("artist", ""),
            key = root.optString("key").takeIf { it.isNotBlank() && it != "null" },
            bpm = if (root.isNull("bpm")) null else root.optInt("bpm"),
            chords = chords
        )
    }
}
