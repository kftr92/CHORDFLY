# CHORDFLY V2

V2 memisahkan UI, player, DSP detector, transpose, ViewModel, dan Gemini analyzer.

## Fitur
- Current chord besar + timeline.
- Sinkronisasi chord berdasarkan waktu player.
- Transpose -12..+12 semitone.
- Real-time microphone chord observation.
- Gemini sebagai correction/interpretation layer.
- YouTube player via IFrame API di WebView.
- JSON chord model yang konsisten.

## Catatan penting
1. Tambahkan `google-services.json` Firebase ke `app/` bila project Firebase Anda memerlukannya.
2. Aktifkan Firebase AI Logic/Gemini pada Firebase project.
3. App Check disarankan untuk penggunaan produksi.
4. Detector DSP di V2 adalah baseline ringan, bukan pengganti chord recognition studio-grade.
5. Untuk analisis lagu dari file audio penuh, tahap berikutnya sebaiknya menambahkan pipeline audio-file -> chroma/beat tracking -> Gemini refinement.

## Struktur
app/src/main/java/com/example/chordfly/
- MainActivity.kt
- MainViewModel.kt
- model/ChordModels.kt
- music/ChordTransposer.kt
- music/ChordDetector.kt
- audio/AudioChordEngine.kt
- youtube/YouTubeWebPlayer.kt
- ai/GeminiChordAnalyzer.kt
