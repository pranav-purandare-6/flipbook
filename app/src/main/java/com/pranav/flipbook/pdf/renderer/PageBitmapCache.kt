package com.pranav.flipbook.pdf.renderer

import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class PageBitmapCache(
    maxMemoryMB: Int = 64
) {
    private val maxCacheSize = maxMemoryMB * 1024 * 1024

    private val memoryCache = object : LruCache<Int, Bitmap>(maxCacheSize) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            return bitmap.allocationByteCount
        }

        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            // Don't recycle - let GC handle it, as bitmap might still be in use by Compose
        }
    }

    private val renderingJobs = ConcurrentHashMap<Int, Job>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun get(pageIndex: Int): Bitmap? = memoryCache.get(pageIndex)

    fun put(pageIndex: Int, bitmap: Bitmap) {
        memoryCache.put(pageIndex, bitmap)
    }

    fun contains(pageIndex: Int): Boolean = memoryCache.get(pageIndex) != null

    fun preloadPages(
        currentPage: Int,
        totalPages: Int,
        renderer: PdfRendererManager,
        width: Int,
        height: Int,
        range: Int = 2
    ) {
        // Cancel jobs for pages far from current position
        renderingJobs.keys.forEach { page ->
            if (page < currentPage - range - 1 || page > currentPage + range + 1) {
                renderingJobs[page]?.cancel()
                renderingJobs.remove(page)
            }
        }

        // Preload nearby pages
        val pagesToLoad = mutableListOf<Int>()
        for (offset in -range..range) {
            val page = currentPage + offset
            if (page in 0 until totalPages && !contains(page)) {
                pagesToLoad.add(page)
            }
        }

        // Sort by distance from current page (closest first)
        pagesToLoad.sortBy { kotlin.math.abs(it - currentPage) }

        for (page in pagesToLoad) {
            if (renderingJobs.containsKey(page)) continue
            val job = scope.launch {
                try {
                    val bitmap = renderer.renderPage(page, width, height)
                    if (bitmap != null && isActive) {
                        put(page, bitmap)
                    }
                } catch (_: CancellationException) {
                    // Expected
                } catch (_: Exception) {
                    // Silently handle render errors for preloading
                }
            }
            renderingJobs[page] = job
        }
    }

    fun cancelAll() {
        renderingJobs.values.forEach { it.cancel() }
        renderingJobs.clear()
    }

    fun evictAll() {
        cancelAll()
        memoryCache.evictAll()
    }

    fun evictDistant(currentPage: Int, keepRange: Int = 5) {
        val snapshot = memoryCache.snapshot()
        snapshot.keys.forEach { key ->
            if (key < currentPage - keepRange || key > currentPage + keepRange) {
                memoryCache.remove(key)
            }
        }
    }

    fun destroy() {
        cancelAll()
        memoryCache.evictAll()
        scope.cancel()
    }
}
