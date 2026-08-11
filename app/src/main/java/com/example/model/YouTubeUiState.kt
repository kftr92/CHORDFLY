package com.example.model

data class YouTubeUiState(
    val searchQuery: String = "",
    val youtubeUrl: String = "",
    val selectedVideoId: String? = null,
    val showYouTubeSearch: Boolean = false,
    val status: String = "Belum ada video dipilih"
)
