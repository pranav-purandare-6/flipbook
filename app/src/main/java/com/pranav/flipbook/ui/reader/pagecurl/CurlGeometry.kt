package com.pranav.flipbook.ui.reader.pagecurl

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import kotlin.math.*

object CurlGeometry {

    data class CurlMeshResult(
        val frontClipPath: Path,
        val backClipPath: Path,
        val curlShadowPath: Path,
        val foldLineStart: Offset,
        val foldLineEnd: Offset,
        val backMatrix: Matrix,
        val shadowAlpha: Float,
        val curlProgress: Float
    )

    /**
     * Physical page curl from corner C to touch point P.
     * Fold line is perpendicular to CP through midpoint M.
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

        val dx = pX - originX
        val dy = pY - originY
        val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

        val midX = (originX + pX) / 2f
        val midY = (originY + pY) / 2f

        // Normal to CP vector - fold line direction
        val nx = -dy / dist
        val ny = dx / dist

        val lineLen = hypot(pageWidth, pageHeight) * 2f
        val foldStart = Offset(midX - nx * lineLen, midY - ny * lineLen)
        val foldEnd = Offset(midX + nx * lineLen, midY + ny * lineLen)

        val progress = (dist / pageWidth).coerceIn(0f, 1f)

        // Signed distance from fold line to determine front/back regions
        // Front = unturned portion (contains origin corner)
        val frontPath = Path().apply {
            if (isForward) {
                // Front is left of fold - polygon from left edge to fold intersection
                val foldX = computeFoldIntersectionX(midX, midY, nx, ny, pageWidth, pageHeight, isForward)
                moveTo(0f, 0f)
                lineTo(foldX.coerceIn(0f, pageWidth), 0f)
                lineTo(foldX.coerceIn(0f, pageWidth), pageHeight)
                lineTo(0f, pageHeight)
                close()
            } else {
                val foldX = computeFoldIntersectionX(midX, midY, nx, ny, pageWidth, pageHeight, isForward)
                moveTo(foldX.coerceIn(0f, pageWidth), 0f)
                lineTo(pageWidth, 0f)
                lineTo(pageWidth, pageHeight)
                lineTo(foldX.coerceIn(0f, pageWidth), pageHeight)
                close()
            }
        }

        // Cylinder width simulates paper thickness at fold
        val cylinderW = (pageWidth * 0.08f + progress * pageWidth * 0.06f).coerceIn(8f, pageWidth * 0.2f)

        val backPath = Path().apply {
            if (isForward) {
                val fx = computeFoldIntersectionX(midX, midY, nx, ny, pageWidth, pageHeight, isForward)
                    .coerceIn(0f, pageWidth)
                moveTo(fx, 0f)
                lineTo((fx + cylinderW).coerceAtMost(pageWidth), 0f)
                lineTo((fx + cylinderW).coerceAtMost(pageWidth), pageHeight)
                lineTo(fx, pageHeight)
                close()
            } else {
                val fx = computeFoldIntersectionX(midX, midY, nx, ny, pageWidth, pageHeight, isForward)
                    .coerceIn(0f, pageWidth)
                moveTo((fx - cylinderW).coerceAtLeast(0f), 0f)
                lineTo(fx, 0f)
                lineTo(fx, pageHeight)
                lineTo((fx - cylinderW).coerceAtLeast(0f), pageHeight)
                close()
            }
        }

        val shadowW = cylinderW * 2f + 20f * progress
        val shadowPath = Path().apply {
            val fx = computeFoldIntersectionX(midX, midY, nx, ny, pageWidth, pageHeight, isForward)
            moveTo(fx - shadowW / 2, 0f)
            lineTo(fx + shadowW / 2, 0f)
            lineTo(fx + shadowW / 2, pageHeight)
            lineTo(fx - shadowW / 2, pageHeight)
            close()
        }

        // Reflect back surface across fold line
        val matrix = Matrix().apply {
            reset()
            translate(midX, midY)
            val scaleX = if (isForward) -0.92f else 0.92f
            scale(scaleX, 0.98f - progress * 0.03f)
            translate(-midX, -midY)
        }

        val shadowAlpha = (0.15f + progress * 0.35f).coerceIn(0.1f, 0.55f)

        return CurlMeshResult(
            frontClipPath = frontPath,
            backClipPath = backPath,
            curlShadowPath = shadowPath,
            foldLineStart = foldStart,
            foldLineEnd = foldEnd,
            backMatrix = matrix,
            shadowAlpha = shadowAlpha,
            curlProgress = progress
        )
    }

    private fun computeFoldIntersectionX(
        midX: Float, midY: Float,
        nx: Float, ny: Float,
        pageWidth: Float, pageHeight: Float,
        isForward: Boolean
    ): Float {
        // Intersect fold line with top edge (y=0) and bottom edge (y=pageHeight), use average x
        val topX = if (abs(ny) > 0.001f) midX - (midY / ny) * nx else midX
        val bottomX = if (abs(ny) > 0.001f) midX + ((pageHeight - midY) / ny) * nx else midX
        val avg = (topX + bottomX) / 2f
        return if (isForward) avg.coerceIn(0f, pageWidth) else avg.coerceIn(0f, pageWidth)
    }
}
