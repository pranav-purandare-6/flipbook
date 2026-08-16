package com.pranav.flipbook.pdf.thumbnails

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.pranav.flipbook.pdf.renderer.PdfRendererManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ThumbnailManager(private val context: Context) {

    private val cacheDir: File
        get() = File(context.cacheDir, "thumbnails").apply { mkdirs() }

    private val coverDir: File
        get() = File(context.filesDir, "covers").apply { mkdirs() }

    suspend fun generateCover(uri: Uri, bookId: Long): String? {
        return withContext(Dispatchers.IO) {
            try {
                val coverFile = File(coverDir, "cover_$bookId.jpg")
                if (coverFile.exists()) return@withContext coverFile.absolutePath

                val renderer = PdfRendererManager(context)
                if (!renderer.open(uri)) return@withContext null

                val bitmap = renderer.renderPage(0, 400, 600)
                renderer.close()

                bitmap?.let {
                    FileOutputStream(coverFile).use { out ->
                        it.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    it.recycle()
                    coverFile.absolutePath
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getThumbnail(uri: Uri, pageIndex: Int, bookId: Long): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val thumbFile = File(cacheDir, "thumb_${bookId}_${pageIndex}.jpg")
                if (thumbFile.exists()) {
                    return@withContext BitmapFactory.decodeFile(thumbFile.absolutePath)
                }

                val renderer = PdfRendererManager(context)
                if (!renderer.open(uri)) return@withContext null

                val bitmap = renderer.renderThumbnail(pageIndex, 200)
                renderer.close()

                bitmap?.let {
                    FileOutputStream(thumbFile).use { out ->
                        it.compress(Bitmap.CompressFormat.JPEG, 75, out)
                    }
                    it
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun getCoverPath(bookId: Long): String? {
        val coverFile = File(coverDir, "cover_$bookId.jpg")
        return if (coverFile.exists()) coverFile.absolutePath else null
    }

    fun deleteCover(bookId: Long) {
        File(coverDir, "cover_$bookId.jpg").delete()
    }

    fun clearThumbnailCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun clearCoverCache() {
        coverDir.listFiles()?.forEach { it.delete() }
    }

    fun getCacheSize(): Long {
        val thumbSize = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        val coverSize = coverDir.listFiles()?.sumOf { it.length() } ?: 0L
        return thumbSize + coverSize
    }
}
