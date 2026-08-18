package com.pranav.flipbook.pdf.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.pranav.flipbook.ui.reader.ReaderAppearance
import com.pranav.flipbook.ui.reader.ReaderMode

object BitmapAppearanceProcessor {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun apply(source: Bitmap, appearance: ReaderAppearance): Bitmap {
        if (source.isRecycled) return source

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val matrix = buildColorMatrix(appearance.mode)
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        paint.colorFilter = null

        return output
    }

    private fun buildColorMatrix(mode: ReaderMode): ColorMatrix {
        return when (mode) {
            ReaderMode.LIGHT -> ColorMatrix()
            ReaderMode.DARK -> {
                // Map bright paper to a warm dark page and dark ink to warm light text.
                ColorMatrix().apply {
                    set(floatArrayOf(
                        -0.23f, -0.46f, -0.09f, 0f, 235f,
                        -0.22f, -0.43f, -0.08f, 0f, 224f,
                        -0.20f, -0.39f, -0.08f, 0f, 205f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
            }
            ReaderMode.SEPIA -> {
                ColorMatrix().apply {
                    set(floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
            }
            ReaderMode.WARM -> {
                ColorMatrix().apply {
                    set(floatArrayOf(
                        1.05f, 0.05f, 0f, 0f, 8f,
                        0.02f, 1.0f, 0f, 0f, 4f,
                        0f, 0f, 0.92f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
            }
            ReaderMode.BLACK_WHITE -> {
                ColorMatrix().apply { setSaturation(0f) }
            }
        }
    }
}
