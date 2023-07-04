package com.bleaudiocontroller.app

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playSong(filePath: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
        }

        // Retrieve and display metadata
        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(filePath)
        val title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artist = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val album = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

        // Now you can use the metadata as needed (e.g., display it in a TextView)
        // For example:
        println("Title: $title")
        println("Artist: $artist")
        println("Album: $album")
    }

    fun stopSong() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
