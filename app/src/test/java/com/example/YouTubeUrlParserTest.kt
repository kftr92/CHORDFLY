package com.example

import com.example.youtube.YouTubeUrlParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class YouTubeUrlParserTest {

    @Test
    fun testExtractVideoIdFromWatchUrl() {
        val url = "https://www.youtube.com/watch?v=jfKfPfyJRdk"
        val videoId = YouTubeUrlParser.extractVideoId(url)
        assertEquals("jfKfPfyJRdk", videoId)
    }

    @Test
    fun testExtractVideoIdFromShortenedUrl() {
        val url = "https://youtu.be/jfKfPfyJRdk?t=10"
        val videoId = YouTubeUrlParser.extractVideoId(url)
        assertEquals("jfKfPfyJRdk", videoId)
    }

    @Test
    fun testExtractVideoIdFromShortsUrl() {
        val url = "https://www.youtube.com/shorts/jfKfPfyJRdk"
        val videoId = YouTubeUrlParser.extractVideoId(url)
        assertEquals("jfKfPfyJRdk", videoId)
    }

    @Test
    fun testExtractVideoIdFromEmbedUrl() {
        val url = "https://www.youtube.com/embed/jfKfPfyJRdk?autoplay=1"
        val videoId = YouTubeUrlParser.extractVideoId(url)
        assertEquals("jfKfPfyJRdk", videoId)
    }

    @Test
    fun testExtractVideoIdFromDirectVideoId() {
        val directId = "jfKfPfyJRdk"
        val videoId = YouTubeUrlParser.extractVideoId(directId)
        assertEquals("jfKfPfyJRdk", videoId)
    }

    @Test
    fun testInvalidUrlReturnsNull() {
        val invalidUrl = "https://example.com/notayoutubeurl"
        val videoId = YouTubeUrlParser.extractVideoId(invalidUrl)
        assertNull(videoId)
    }

    @Test
    fun testBlankInputReturnsNull() {
        assertNull(YouTubeUrlParser.extractVideoId("   "))
    }

    @Test
    fun testViewModelSearchEmptyQueryReturnsMessage() = runBlocking {
        val viewModel = MainViewModel()
        viewModel.onSearchQueryChange("   ")
        viewModel.openYouTubeSearch()
        assertEquals("Masukkan judul lagu terlebih dahulu.", viewModel.youtubeState.value.status)
    }

    @Test
    fun testViewModelOpenValidUrlLoadsVideo() = runBlocking {
        val viewModel = MainViewModel()
        viewModel.onYoutubeUrlChange("https://youtu.be/jfKfPfyJRdk")
        viewModel.openVideoFromUrl()
        assertEquals("jfKfPfyJRdk", viewModel.youtubeState.value.selectedVideoId)
        assertTrue(viewModel.youtubeState.value.status.contains("Video dimuat"))
    }

    @Test
    fun testViewModelClipboardPasteValidUrl() = runBlocking {
        val viewModel = MainViewModel()
        viewModel.pasteFromClipboard("https://www.youtube.com/watch?v=jfKfPfyJRdk")
        assertEquals("jfKfPfyJRdk", viewModel.youtubeState.value.selectedVideoId)
    }

    @Test
    fun testViewModelSearchValidQueryOpensWebViewSearch() = runBlocking {
        val viewModel = MainViewModel()
        viewModel.onSearchQueryChange("Peterpan Bintang di Surga")
        viewModel.openYouTubeSearch()
        assertTrue(viewModel.youtubeState.value.showYouTubeSearch)
    }

    @Test
    fun testViewModelSelectVideoClosesWebViewAndLoadsPlayer() = runBlocking {
        val viewModel = MainViewModel()
        viewModel.onSearchQueryChange("Peterpan Bintang di Surga")
        viewModel.openYouTubeSearch()
        assertTrue(viewModel.youtubeState.value.showYouTubeSearch)

        viewModel.selectYouTubeVideo("jfKfPfyJRdk")
        assertEquals("jfKfPfyJRdk", viewModel.youtubeState.value.selectedVideoId)
        assertFalse(viewModel.youtubeState.value.showYouTubeSearch)
        assertTrue(viewModel.uiState.value.activeTargetUrl.contains("jfKfPfyJRdk"))
    }

    @Test
    fun testViewModelCloseYouTubeSearch() = runBlocking {
        val viewModel = MainViewModel()
        viewModel.onSearchQueryChange("Peterpan Bintang di Surga")
        viewModel.openYouTubeSearch()
        assertTrue(viewModel.youtubeState.value.showYouTubeSearch)

        viewModel.closeYouTubeSearch()
        assertFalse(viewModel.youtubeState.value.showYouTubeSearch)
    }

    @Test
    fun testViewModelClipboardPasteInvalidUrl() = runBlocking {
        val viewModel = MainViewModel()
        viewModel.pasteFromClipboard("not a url")
        assertEquals("Clipboard tidak berisi URL YouTube.", viewModel.youtubeState.value.status)
    }
}
