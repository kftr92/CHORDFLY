package com.example.model

data class YouTubeUiState(
    val searchQuery: String = "",
    val youtubeUrl: String = "",
    val selectedVideoId: String? = null,
    val showYouTubeSearch: Boolean = false,
    val status: String = "Belum ada video dipilih",
    val searchResults: List<SongSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val selectedSearchTab: SearchTab = SearchTab.ALL
)
