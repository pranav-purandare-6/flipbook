package com.pranav.flipbook.utils

import android.content.Context
import com.pranav.flipbook.pdf.renderer.PageBitmapCache
import com.pranav.flipbook.pdf.thumbnails.ThumbnailManager
import java.io.File

object CacheManager {

    fun getPageCacheSizeBytes(pageCache: PageBitmapCache?): Long = 0L

    fun getThumbnailCacheSize(context: Context): Long {
        val dir = File(context.cacheDir, "covers")
        return dirSize(dir)
    }

    fun getTempCacheSize(context: Context): Long {
        val cacheDir = context.cacheDir
        var total = 0L
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("temp_pdf_") || file.name.startsWith("sounds")) {
                total += if (file.isDirectory) dirSize(file) else file.length()
            }
        }
        return total
    }

    fun clearPageCache(pageCache: PageBitmapCache?) {
        pageCache?.evictAll()
    }

    fun clearThumbnailCache(context: Context) {
        val dir = File(context.cacheDir, "covers")
        dir.deleteRecursively()
        dir.mkdirs()
    }

    fun clearTempCache(context: Context) {
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("temp_pdf_")) file.deleteRecursively()
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
        return "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
