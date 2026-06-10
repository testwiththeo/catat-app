package com.catat.app.data.storage

import android.graphics.Bitmap

interface FileStorage {
    fun saveBitmap(bitmap: Bitmap, subDir: String, prefix: String = "screenshot"): String
    fun saveText(content: String, subDir: String, prefix: String, extension: String): String
    fun loadBitmap(path: String): Bitmap?
    fun delete(path: String): Boolean
    fun listFiles(subDir: String): List<String>
    fun enforceMaxFiles(subDir: String, max: Int)
    fun getFileUri(path: String): android.net.Uri
}
