package com.catat.app.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileStorage {

    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "com.catat.app.fileprovider"
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss_SSS"
    }

    override fun saveBitmap(bitmap: Bitmap, subDir: String, prefix: String): String {
        val dir = File(context.filesDir, subDir).also { it.mkdirs() }
        val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
        val file = File(dir, "${prefix}_$timestamp.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    override fun saveText(content: String, subDir: String, prefix: String, extension: String): String {
        val safeExtension = extension.trimStart('.').ifBlank { "txt" }
        val dir = File(context.filesDir, subDir).also { it.mkdirs() }
        val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
        val file = File(dir, "${prefix}_$timestamp.$safeExtension")
        file.writeText(content, StandardCharsets.UTF_8)
        return file.absolutePath
    }

    override fun loadBitmap(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            Timber.w(e, "Failed to load bitmap from $path")
            null
        }
    }

    override fun delete(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            Timber.w(e, "Failed to delete file $path")
            false
        }
    }

    override fun listFiles(subDir: String): List<String> {
        val dir = File(context.filesDir, subDir)
        return dir.listFiles()?.map { it.absolutePath } ?: emptyList()
    }

    override fun enforceMaxFiles(subDir: String, max: Int) {
        val dir = File(context.filesDir, subDir)
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        if (files.size > max) {
            files.take(files.size - max).forEach { file ->
                file.delete()
                Timber.d("Deleted oldest export file: ${file.name}")
            }
        }
    }

    override fun getFileUri(path: String): Uri {
        return FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            File(path)
        )
    }
}
