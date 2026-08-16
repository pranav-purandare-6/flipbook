package com.pranav.flipbook.ui.reader.pagecurl

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.*

object CurlGeometry {

    data class CurlMesh(
        val frontClipPath: Path,
        val curlPath: Path,
        val curlShadowPath: Path,
        val backClipPath: Path,
        val curlTipX: Float,
        val curlTipY: Float,
        val foldLineAngle: Float,
        val shadowAlpha: Float
    )

    /**
     * Compute curl geometry from a normalized position (0..1).
     * curlAmount: 0 = fully curled (page turned), 1 = flat (no curl)
     * For forward turn: page curls from right to left
     */
    fun computeCurl(
        curlAmount: Float,
        pageWidth: Float,
        pageHeight: Float,
        isForward: Boolean = true
    ): CurlMesh {
        val clampedCurl = curlAmount.coerceIn(0f, 1f)

        // The fold line x position: moves from right edge to left
        val foldX = if (isForward) {
            pageWidth * clampedCurl
        } else {
            pageWidth * (1f - clampedCurl)
        }

        // Curl radius for the cylinder effect
        val curlRadius = pageWidth * 0.08f * (1f - abs(clampedCurl - 0.5f) * 2f)
        
        // Shadow parameters
        val shadowAlpha = (0.4f * (1f - clampedCurl)).coerceIn(0f, 0.4f)

        // Front clip path: the visible, flat part of the current page
        val frontClipPath = Path().apply {
            if (isForward) {
                moveTo(0f, 0f)
                lineTo(foldX, 0f)
                lineTo(foldX, pageHeight)
                lineTo(0f, pageHeight)
                close()
            } else {
                moveTo(foldX, 0f)
                lineTo(pageWidth, 0f)
                lineTo(pageWidth, pageHeight)
                lineTo(foldX, pageHeight)
                close()
            }
        }

        // The curled section of the page
        val curlWidth = curlRadius * PI.toFloat()
        val curlPath = Path().apply {
            if (isForward) {
                val curlStart = foldX
                val curlEnd = (foldX + curlWidth).coerceAtMost(pageWidth)
                moveTo(curlStart, 0f)
                // Add slight curve at top
                cubicTo(
                    curlStart + curlWidth * 0.3f, 0f,
                    curlStart + curlWidth * 0.5f, pageHeight * 0.02f,
                    curlEnd, 0f
                )
                lineTo(curlEnd, pageHeight)
                cubicTo(
                    curlStart + curlWidth * 0.5f, pageHeight - pageHeight * 0.02f,
                    curlStart + curlWidth * 0.3f, pageHeight,
                    curlStart, pageHeight
                )
                close()
            } else {
                val curlStart = foldX
                val curlEnd = (foldX - curlWidth).coerceAtLeast(0f)
                moveTo(curlStart, 0f)
                cubicTo(
                    curlStart - curlWidth * 0.3f, 0f,
                    curlStart - curlWidth * 0.5f, pageHeight * 0.02f,
                    curlEnd, 0f
                )
                lineTo(curlEnd, pageHeight)
                cubicTo(
                    curlStart - curlWidth * 0.5f, pageHeight - pageHeight * 0.02f,
                    curlStart - curlWidth * 0.3f, pageHeight,
                    curlStart, pageHeight
                )
                close()
            }
        }

        // Shadow path along the fold line
        val shadowWidth = curlRadius * 2f + 20f
        val curlShadowPath = Path().apply {
            if (isForward) {
                moveTo(foldX - shadowWidth / 2, 0f)
                lineTo(foldX + shadowWidth / 2, 0f)
                lineTo(foldX + shadowWidth / 2, pageHeight)
                lineTo(foldX - shadowWidth / 2, pageHeight)
                close()
            } else {
                moveTo(foldX - shadowWidth / 2, 0f)
                lineTo(foldX + shadowWidth / 2, 0f)
                lineTo(foldX + shadowWidth / 2, pageHeight)
                lineTo(foldX - shadowWidth / 2, pageHeight)
                close()
            }
        }

        // Back of page clip (reflected)
        val backClipPath = Path().apply {
            if (isForward) {
                val backX = foldX + curlWidth
                moveTo(foldX, 0f)
                lineTo(backX.coerceAtMost(pageWidth), 0f)
                lineTo(backX.coerceAtMost(pageWidth), pageHeight)
                lineTo(foldX, pageHeight)
                close()
            } else {
                val backX = foldX - curlWidth
                moveTo(backX.coerceAtLeast(0f), 0f)
                lineTo(foldX, 0f)
                lineTo(foldX, pageHeight)
                lineTo(backX.coerceAtLeast(0f), pageHeight)
                close()
            }
        }

        return CurlMesh(
            frontClipPath = frontClipPath,
            curlPath = curlPath,
            curlShadowPath = curlShadowPath,
            backClipPath = backClipPath,
            curlTipX = foldX,
            curlTipY = pageHeight / 2f,
            foldLineAngle = 0f,
            shadowAlpha = shadowAlpha
        )
    }

    /**
     * Convert a drag X position to a curl amount (0..1).
     */
    fun dragToCurlAmount(dragX: Float, startX: Float, pageWidth: Float, isForward: Boolean): Float {
        return if (isForward) {
            val delta = startX - dragX
            (delta / pageWidth).coerceIn(0f, 1f)
        } else {
            val delta = dragX - startX
            (delta / pageWidth).coerceIn(0f, 1f)
        }
    }

    /**
     * Determine if the curl should complete or cancel based on the amount.
     */
    fun shouldCompleteTurn(curlAmount: Float, velocity: Float = 0f): Boolean {
        return curlAmount > 0.35f || abs(velocity) > 500f
    }
}
