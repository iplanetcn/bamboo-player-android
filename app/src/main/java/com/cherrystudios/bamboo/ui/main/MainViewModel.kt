package com.cherrystudios.bamboo.ui.main

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.cherrystudios.bamboo.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit


data class UiState(
    val data: List<AudioFile> = listOf(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class AudioFile(
    val id: Long,
    val displayName: String,
    val artist: String?,
    val duration: Int,
    val size: Int
) {
    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<AudioFile> = object : DiffUtil.ItemCallback<AudioFile>() {
            override fun areItemsTheSame(
                oldItem: AudioFile,
                newItem: AudioFile
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: AudioFile,
                newItem: AudioFile
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class MainViewModel : ViewModel() {


    private val _audioFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    val audioFiles = _audioFiles.asStateFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    fun queryMediaAudio(applicationContext: Application) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val audioList = queryAudioFiles(applicationContext)
            _audioFiles.value = audioList
            // If the list is empty, it might be because the media store is outdated.
            // Let's trigger a scan to refresh it.
            if (audioList.isEmpty()) {
                scanAudioFiles(applicationContext)
            } else {
                _uiState.value = _uiState.value.copy(data = audioList, isLoading = false)
            }
        }
    }

    /**
     * 查询音频文件
     */
    private suspend fun queryAudioFiles(applicationContext: Application): List<AudioFile> =
        withContext(Dispatchers.IO) {
            val audioList = mutableListOf<AudioFile>()

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ARTIST,
            )
            val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
            val selectionArgs = arrayOf(
                TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS).toString()
            )
            val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val query = applicationContext.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            Timber.d("--- Start query audio files ---")
            query?.use { cursor ->
                if (cursor.count > 0) {
                    Timber.d("Found ${cursor.count} file(s)")
                    while (cursor.moveToNext()) {
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

                        val id = cursor.getLong(idColumn)
                        val displayName = cursor.getString(displayNameColumn)
                        val duration = cursor.getInt(durationColumn)
                        val size = cursor.getInt(sizeColumn)
                        val artist = cursor.getString(artistColumn)
                        val audioFile = AudioFile(id, displayName, artist, duration, size)
                        audioList.add(audioFile)
                        Timber.d("id: $id, displayName: $displayName, artist: $artist, duration: $duration, size: $size")
                    }
                } else {
                    Timber.d("Found none audio file")
                }
            }
            audioList.firstOrNull()?.apply {
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                Timber.d("First audio file's uri: $uri")
            }
            audioList
        }

    fun scanAudioFiles(context: Context) {
        // 扫描文件
        MediaScannerConnection.scanFile(
            context,
            arrayOf(Environment.getExternalStorageDirectory().path),  // File path(s)
            null  // MIME type (null auto-detects)
        ) { path, uri ->
            Timber.d("Scan complete for path: $path, uri: $uri")
        }
    }
}
