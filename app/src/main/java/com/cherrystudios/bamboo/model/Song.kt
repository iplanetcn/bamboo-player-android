package com.cherrystudios.bamboo.model

import android.net.Uri

/**
 * Song
 *
 * @author john
 * @since 2025-11-13
 */
data class Song(
    val title: String,
    val artist: String,
    val uri: String,
    val coverUri: Uri? = null
)