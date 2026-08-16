package com.pranav.flipbook.pdf.renderer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererManager(private val context: Context) {

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentUri: Uri? = null
    private val mutex = Mutex()
    private var tempFile: File? = null

    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    suspend fun open(uri: Uri): Boolean = mutex.withLock {
        return@withLock withContext(Dispatchers.IO) {
            try {
                close_internal()
                
                val targetFile: File? = when {
                    uri.scheme == "file" && uri.path != null -> {
                        File(uri.path!!).takeIf { it.exists() && it.length() > 0 }
                    }
                    else -> null
                }

                val fd: ParcelFileDescriptor = if (targetFile != null) {
                    ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
                } else {
                    // Try openFileDescriptor directly from contentResolver first
                    val pfd = try {
                        context.contentResolver.openFileDescriptor(uri, "r")
                    } catch (_: Exception) { null }

                    if (pfd != null) {
                        pfd
                    } else {
                        // Fallback: Copy stream to cache file
                        val temp = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(temp).use { output ->
                                input.copyTo(output)
                            }
                        } ?: return@withContext false
                        tempFile = temp
                        ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
                    }
                }

                val renderer = PdfRenderer(fd)
                fileDescriptor = fd
                pdfRenderer = renderer
                currentUri = uri
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun renderPage(pageIndex: Int, width: Int, height: Int): Bitmap? = mutex.withLock {
        return@withLock withContext(Dispatchers.IO) {
            try {
                val renderer = pdfRenderer ?: return@withContext null
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

                val page = renderer.openPage(pageIndex)
                
                // Calculate dimensions maintaining aspect ratio
                val pageAspect = page.width.toFloat() / page.height.toFloat()
                val targetAspect = width.toFloat() / height.toFloat()
                
                val bitmapWidth: Int
                val bitmapHeight: Int
                
                if (pageAspect > targetAspect) {
                    bitmapWidth = width
                    bitmapHeight = (width / pageAspect).toInt()
                } else {
                    bitmapHeight = height
                    bitmapWidth = (height * pageAspect).toInt()
                }

                val bitmap = Bitmap.createBitmap(
                    bitmapWidth.coerceAtLeast(1),
                    bitmapHeight.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun renderThumbnail(pageIndex: Int, maxSize: Int = 200): Bitmap? {
        return renderPage(pageIndex, maxSize, maxSize)
    }

    suspend fun close() = mutex.withLock {
        withContext(Dispatchers.IO) {
            close_internal()
        }
    }

    private fun close_internal() {
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        try {
            fileDescriptor?.close()
        } catch (_: Exception) {}
        try {
            tempFile?.delete()
        } catch (_: Exception) {}
        pdfRenderer = null
        fileDescriptor = null
        currentUri = null
        tempFile = null
    }
}
