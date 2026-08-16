package com.pranav.flipbook.ui.reader.pagecurl

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import kotlin.math.*

object CurlGeometry {

    data class Vector2D(val x: Float, val y: Float) {
        fun length() = sqrt(x * x + y * y)
        fun normalized(): Vector2D {
            val len = length()
            return if (len > 0f) Vector2D(x / len, y / len) else Vector2D(0f, 0f)
        }
        operator fun plus(other: Vector2D) = Vector2D(x + other.x, y + other.y)
        operator fun minus(other: Vector2D) = Vector2D(x - other.x, y - other.y)
        operator fun times(factor: Float) = Vector2D(x * factor, y * factor)
    }

    data class CurlMeshResult(
        val frontClipPath: Path,
        val backClipPath: Path,
        val curlShadowPath: Path,
        val foldLineStart: Offset,
        val foldLineEnd: Offset,
        val backMatrix: Matrix,
        val shadowAlpha: Float,
        val cylinderWidth: Float
    )

    /**
     * Compute authentic paper curl geometry based on touch drag point P relative to origin corner C.
     * Uses perpendicular bisector geometry: fold line is normal to vector (P - C) passing through midpoint M.
     */
    fun computePhysicalCurl(
        touchX: Float,
        touchY: Float,
        originX: Float,
        originY: Float,
        pageWidth: Float,
        pageHeight: Float,
        isForward: Boolean
    ): CurlMeshResult {
        val pX = touchX.coerceIn(0f, pageWidth)
        val pY = touchY.coerceIn(0f, pageHeight)

        // Midpoint between origin corner C and dragged touch point P
        val midX = (originX + pX) / 2f
        val midY = (originY + pY) / 2f

        // Vector from C to P
        val dx = pX - originX
        val dy = pY - originY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

        // Perpendicular vector along the fold line
        val normalX = -dy / dist
        val normalY = dx / dist

        // Extend fold line across the page bounding box
        val lineLen = sqrt(pageWidth * pageWidth + pageHeight * pageHeight) * 2f
        val lineStart = Offset(midX - normalX * lineLen, midY - normalY * lineLen)
        val lineEnd = Offset(midX + normalX * lineLen, midY + normalY * lineLen)

        // Calculate progress (0 = no curl, 1 = fully turned)
        val progress = (abs(pX - originX) / pageWidth).coerceIn(0f, 1f)

        // Front path (unturned portion of page)
        val frontPath = Path().apply {
            if (isForward) {
                moveTo(0f, 0f)
                lineTo(midX, 0f)
                lineTo(midX, pageHeight)
                lineTo(0f, pageHeight)
                close()
            } else {
                moveTo(midX, 0f)
                lineTo(pageWidth, 0f)
                lineTo(pageWidth, pageHeight)
                lineTo(midX, pageHeight)
                close()
            }
        }

        // Folded back path
        val cylinderW = (pageWidth * 0.12f * sin(progress * PI.toFloat())).coerceAtLeast(10f)
        val backPath = Path().apply {
            if (isForward) {
                moveTo(midX, 0f)
                lineTo((midX + cylinderW).coerceAtMost(pageWidth), 0f)
                lineTo((midX + cylinderW).coerceAtMost(pageWidth), pageHeight)
                lineTo(midX, pageHeight)
                close()
            } else {
                moveTo((midX - cylinderW).coerceAtLeast(0f), 0f)
                lineTo(midX, 0f)
                lineTo(midX, pageHeight)
                lineTo((midX - cylinderW).coerceAtLeast(0f), pageHeight)
                close()
            }
        }

        // Shadow along fold line
        val shadowW = cylinderW * 1.5f + 15f
        val shadowPath = Path().apply {
            moveTo(midX - shadowW / 2, 0f)
            lineTo(midX + shadowW / 2, 0f)
            lineTo(midX + shadowW / 2, pageHeight)
            lineTo(midX - shadowW / 2, pageHeight)
            close()
        }

        // Reflection matrix across fold line for back of page
        val matrix = Matrix().apply {
            reset()
            translate(midX, midY)
            scale(if (isForward) -0.95f else 0.95f, 1f)
            translate(-midX, -midY)
        }

        val shadowAlpha = (0.35f * (1f - progress)).coerceIn(0.05f, 0.4f)

        return CurlMeshResult(
            frontClipPath = frontPath,
            backClipPath = backPath,
            curlShadowPath = shadowPath,
            foldLineStart = lineStart,
            foldLineEnd = lineEnd,
            backMatrix = matrix,
            shadowAlpha = shadowAlpha,
            cylinderWidth = cylinderW
        )
    }
}
