package com.example.youtube

import android.util.Log
import com.example.model.SongSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object YouTubeSearchService {
    private const val TAG = "CHORDFLY_YOUTUBE_SRCH"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun searchSongs(query: String): List<SongSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SongSearchResult>()
        if (query.isBlank()) return@withContext results

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string().orEmpty()

            val ytInitialDataIndex = html.indexOf("var ytInitialData = ")
            val jsonString = if (ytInitialDataIndex != -1) {
                val start = ytInitialDataIndex + "var ytInitialData = ".length
                val end = html.indexOf(";</script>", start)
                if (end != -1) html.substring(start, end) else null
            } else {
                val altIndex = html.indexOf("ytInitialData = ")
                if (altIndex != -1) {
                    val start = altIndex + "ytInitialData = ".length
                    val end = html.indexOf(";</script>", start)
                    if (end != -1) html.substring(start, end) else null
                } else null
            }

            if (jsonString != null) {
                try {
                    val jsonObject = JSONObject(jsonString)
                    parseYtInitialData(jsonObject, results)
                } catch (e: Exception) {
                    Log.e(TAG, "JSON parse error", e)
                }
            }

            // Fallback regex parsing if JSON parsing didn't return items
            if (results.isEmpty()) {
                parseByRegex(html, results)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search request failed", e)
        }

        return@withContext results
    }

    private fun parseYtInitialData(root: JSONObject, outList: MutableList<SongSearchResult>) {
        val contents = root.optJSONObject("contents")
            ?.optJSONObject("twoColumnSearchResultsRenderer")
            ?.optJSONObject("primaryContents")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return

        val addedIds = mutableSetOf<String>()

        for (i in 0 until contents.length()) {
            val item = contents.optJSONObject(i) ?: continue
            val itemSection = item.optJSONObject("itemSectionRenderer") ?: continue
            val contentsArray = itemSection.optJSONArray("contents") ?: continue

            for (j in 0 until contentsArray.length()) {
                val videoObj = contentsArray.optJSONObject(j)?.optJSONObject("videoRenderer") ?: continue
                val videoId = videoObj.optString("videoId")
                if (videoId.isBlank() || !addedIds.add(videoId)) continue

                val titleObj = videoObj.optJSONObject("title")
                val titleRuns = titleObj?.optJSONArray("runs")
                val title = if (titleRuns != null && titleRuns.length() > 0) {
                    titleRuns.getJSONObject(0).optString("text")
                } else {
                    titleObj?.optJSONObject("accessibility")?.optJSONObject("accessibilityData")?.optString("label") ?: "Song $videoId"
                }

                val ownerTextObj = videoObj.optJSONObject("ownerText") ?: videoObj.optJSONObject("shortBylineText")
                val ownerRuns = ownerTextObj?.optJSONArray("runs")
                val artist = if (ownerRuns != null && ownerRuns.length() > 0) {
                    ownerRuns.getJSONObject(0).optString("text")
                } else ""

                val thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                outList.add(
                    SongSearchResult(
                        videoId = videoId,
                        title = title,
                        thumbnailUrl = thumbUrl,
                        artist = artist,
                        chords = emptyList(),
                        isChordified = true
                    )
                )
            }
        }
    }

    private fun parseByRegex(html: String, outList: MutableList<SongSearchResult>) {
        val regex = """"videoRenderer":\s*\{"videoId":"([a-zA-Z0-9_-]{11})".*?"title":\s*\{\s*"runs":\s*\[\s*\{\s*"text":"([^"]+)"""".toRegex()
        val matches = regex.findAll(html)
        val addedIds = mutableSetOf<String>()

        for (match in matches) {
            val videoId = match.groupValues[1]
            val title = match.groupValues[2]
            if (addedIds.add(videoId)) {
                outList.add(
                    SongSearchResult(
                        videoId = videoId,
                        title = title,
                        thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                        artist = "",
                        chords = emptyList(),
                        isChordified = true
                    )
                )
            }
            if (outList.size >= 20) break
        }
    }
}
