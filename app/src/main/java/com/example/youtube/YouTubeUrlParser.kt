package com.example.youtube

import java.util.regex.Pattern

object YouTubeUrlParser {
    private val VIDEO_ID_REGEX = Pattern.compile("^[A-Za-z0-9_-]{11}$")

    fun extractVideoId(input: String): String? {
        val clean = input.trim()
        if (clean.isBlank()) return null

        // Direct 11-character video ID
        if (isValidVideoId(clean)) {
            return clean
        }

        // Standard watch?v= format
        if (clean.contains("watch?v=")) {
            val candidate = clean.substringAfter("watch?v=")
                .substringBefore("&")
                .substringBefore("?")
                .substringBefore("#")
            if (isValidVideoId(candidate)) return candidate
        }

        // Shortened youtu.be/ format
        if (clean.contains("youtu.be/")) {
            val candidate = clean.substringAfter("youtu.be/")
                .substringBefore("?")
                .substringBefore("#")
                .substringBefore("/")
            if (isValidVideoId(candidate)) return candidate
        }

        // Shorts format
        if (clean.contains("youtube.com/shorts/")) {
            val candidate = clean.substringAfter("shorts/")
                .substringBefore("?")
                .substringBefore("#")
                .substringBefore("/")
            if (isValidVideoId(candidate)) return candidate
        }

        // Embed format
        if (clean.contains("youtube.com/embed/")) {
            val candidate = clean.substringAfter("embed/")
                .substringBefore("?")
                .substringBefore("#")
                .substringBefore("/")
            if (isValidVideoId(candidate)) return candidate
        }

        // General regex fallback for complex query strings or shared links
        val generalPattern = Pattern.compile("(?:v=|/videos/|embed/|shorts/|youtu\\.be/|/v/|/e/)([^#&?%\n\"']{11})")
        val matcher = generalPattern.matcher(clean)
        if (matcher.find()) {
            val candidate = matcher.group(1)
            if (candidate != null && isValidVideoId(candidate)) {
                return candidate
            }
        }

        return null
    }

    fun isValidVideoId(id: String): Boolean {
        return id.length == 11 && VIDEO_ID_REGEX.matcher(id).matches()
    }
}
