package com.example.model

data class SongSearchResult(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val artist: String = "",
    val chords: List<String> = emptyList(),
    val isChordified: Boolean = false
)

enum class SearchTab {
    ALL,
    SONGS,
    ARTISTS
}
