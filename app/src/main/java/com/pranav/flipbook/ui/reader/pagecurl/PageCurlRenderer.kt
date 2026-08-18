package com.pranav.flipbook.ui.reader.pagecurl

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class PageTransitionStyle {
    CURL, SLIDE, FADE, NONE
}

@Composable
fun PageCurlView(
    currentBitmap: Bitmap?,
    nextBitmap: Bitmap?,
    previousBitmap: Bitmap?,
    pageIndex: Int,
    totalPages: Int,
    transitionStyle: PageTransitionStyle,
    animationDurationMs: Int,
    onPageForward: () -> Unit,
    onPageBackward: () -> Unit,
    onTurnStart: (forward: Boolean) -> Unit,
    onCenterTap: () -> Unit,
    isZoomed: Boolean,
    modifier: Modifier = Modifier
) {
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var curlState by remember { mutableStateOf(PageCurlStateData()) }
    val curlAmount = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }

    val currentImage = remember(currentBitmap) { currentBitmap?.asImageBitmap() }
    val nextImage = remember(nextBitmap) { nextBitmap?.asImageBitmap() }
    val previousImage = remember(previousBitmap) { previousBitmap?.asImageBitmap() }

    fun canGoForward() = pageIndex < totalPages - 1
    fun canGoBackward() = pageIndex > 0

    suspend fun completeTurn(forward: Boolean) {
        if (curlState.isAnimating) return
        curlState = curlState.copy(
            state = if (forward) CurlState.COMPLETING_FORWARD else CurlState.COMPLETING_BACKWARD,
            direction = if (forward) CurlDirection.FORWARD else CurlDirection.BACKWARD,
            pageWidth = viewSize.width.toFloat(),
            pageHeight = viewSize.height.toFloat(),
            dragX = if (forward) 0f else viewSize.width.toFloat(),
            dragY = viewSize.height / 2f,
            startX = if (forward) viewSize.width.toFloat() else 0f,
            startY = viewSize.height / 2f
        )
        onTurnStart(forward)
        curlAmount.snapTo(curlAmount.value.coerceAtLeast(0.01f))
        curlAmount.animateTo(1f, animationSpec = tween(animationDurationMs))
        if (forward) onPageForward() else onPageBackward()
        curlState = PageCurlStateData()
        curlAmount.snapTo(0f)
    }

    suspend fun cancelTurn() {
        val current = curlAmount.value
        curlState = curlState.copy(
            state = if (curlState.direction == CurlDirection.FORWARD)
                CurlState.CANCELING_FORWARD else CurlState.CANCELING_BACKWARD
        )
        curlAmount.animateTo(
            0f,
            animationSpec = tween((animationDurationMs * current).toInt().coerceIn(80, 600))
        )
        curlState = PageCurlStateData()
        curlAmount.snapTo(0f)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
            .pointerInput(isZoomed, pageIndex, transitionStyle) {
                if (isZoomed || transitionStyle == PageTransitionStyle.NONE) return@pointerInput
                detectTapGestures { offset ->
                    if (!curlState.canStartTurn()) return@detectTapGestures
                    val third = viewSize.width / 3f
                    when {
                        offset.x < third && canGoBackward() -> scope.launch { completeTurn(false) }
                        offset.x > viewSize.width - third && canGoForward() -> scope.launch { completeTurn(true) }
                        else -> onCenterTap()
                    }
                }
            }
            .pointerInput(isZoomed, pageIndex, transitionStyle) {
                if (isZoomed || transitionStyle == PageTransitionStyle.NONE) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (!curlState.canStartTurn()) return@detectHorizontalDragGestures
                        val fromRight = offset.x > viewSize.width * 0.55f
                        val fromLeft = offset.x < viewSize.width * 0.45f
                        val forward = fromRight && canGoForward()
                        val backward = fromLeft && canGoBackward()
                        if (!forward && !backward) return@detectHorizontalDragGestures

                        val dir = if (forward) CurlDirection.FORWARD else CurlDirection.BACKWARD
                        curlState = curlState.copy(
                            state = if (forward) CurlState.DRAGGING_FORWARD else CurlState.DRAGGING_BACKWARD,
                            direction = dir,
                            startX = if (forward) viewSize.width.toFloat() else 0f,
                            startY = offset.y,
                            dragX = offset.x,
                            dragY = offset.y,
                            pageWidth = viewSize.width.toFloat(),
                            pageHeight = viewSize.height.toFloat()
                        )
                        velocityTracker.resetTracking()
                    },
                    onDragEnd = {
                        if (!curlState.isDragging) return@detectHorizontalDragGestures
                        val velocity = velocityTracker.calculateVelocity().x
                        val current = curlAmount.value
                        val forward = curlState.direction == CurlDirection.FORWARD
                        val shouldComplete = current > 0.38f || (forward && velocity < -800f) || (!forward && velocity > 800f)

                        scope.launch {
                            if (shouldComplete) {
                                completeTurn(forward)
                            } else {
                                cancelTurn()
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (!curlState.isDragging) return@detectHorizontalDragGestures
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)

                        val newDragX = (curlState.dragX + dragAmount).coerceIn(0f, viewSize.width.toFloat())
                        val forward = curlState.direction == CurlDirection.FORWARD

                        val curl = if (forward) {
                            ((curlState.startX - newDragX) / viewSize.width).coerceIn(0f, 1f)
                        } else {
                            ((newDragX - curlState.startX) / viewSize.width).coerceIn(0f, 1f)
                        }

                        curlState = curlState.copy(dragX = newDragX)
                        scope.launch { curlAmount.snapTo(curl) }
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0 || h <= 0) return@Canvas

        when (transitionStyle) {
            PageTransitionStyle.CURL -> drawPageCurl(
                currentImage, nextImage, previousImage,
                curlAmount.value, curlState, w, h
            )
            PageTransitionStyle.SLIDE -> drawPageSlide(
                currentImage, nextImage, previousImage,
                curlAmount.value, curlState, w, h
            )
            PageTransitionStyle.FADE -> drawPageFade(
                currentImage, nextImage, previousImage,
                curlAmount.value, curlState, w, h
            )
            PageTransitionStyle.NONE -> drawPageNone(currentImage, w, h)
        }
    }
}

private fun DrawScope.drawPageCurl(
    currentImage: ImageBitmap?,
    nextImage: ImageBitmap?,
    previousImage: ImageBitmap?,
    curlAmount: Float,
    state: PageCurlStateData,
    w: Float,
    h: Float
) {
    if (curlAmount <= 0.001f || state.isIdle) {
        drawCurrentPage(currentImage, w, h)
        return
    }

    val isForward = state.direction == CurlDirection.FORWARD
    val originX = if (isForward) w else 0f
    val originY = state.startY.coerceIn(0f, h)
    val touchX = if (isForward) w * (1f - curlAmount) else w * curlAmount
    val touchY = state.dragY.coerceIn(0f, h)

    val result = CurlGeometry.computePhysicalCurl(
        touchX = touchX,
        touchY = touchY,
        originX = originX,
        originY = originY,
        pageWidth = w,
        pageHeight = h,
        isForward = isForward
    )

    // Underlying page
    val underImage = if (isForward) nextImage else previousImage
    if (underImage != null) {
        drawImageBitmap(underImage, w, h)
    } else {
        drawRect(Color(0xFFF5F0E8), size = Size(w, h))
    }

    // Fold shadow on underlying page
    clipPath(result.curlShadowPath) {
        drawRect(Color.Black.copy(alpha = result.shadowAlpha), size = Size(w, h))
    }

    // Flat front portion
    clipPath(result.frontClipPath) {
        drawCurrentPage(currentImage, w, h)
    }

    // Curled back of page - paper underside
    clipPath(result.backClipPath) {
        withTransform({ transform(result.backMatrix) }) {
            drawRect(Color(0xFFEDE6DA), size = Size(w, h))
            if (currentImage != null) {
                drawImageBitmap(currentImage, w, h, alpha = 0.75f)
            }
            // Subtle paper texture lines
            drawRect(Color.Black.copy(alpha = 0.06f), size = Size(w, h))
        }
    }

    // Fold highlight
    drawLine(
        color = Color.White.copy(alpha = 0.35f * (1f - result.curlProgress * 0.5f)),
        start = result.foldLineStart,
        end = result.foldLineEnd,
        strokeWidth = 1.5f
    )
}

private fun DrawScope.drawPageSlide(
    currentImage: ImageBitmap?,
    nextImage: ImageBitmap?,
    previousImage: ImageBitmap?,
    curlAmount: Float,
    state: PageCurlStateData,
    w: Float,
    h: Float
) {
    if (curlAmount <= 0.001f || state.isIdle) {
        drawCurrentPage(currentImage, w, h)
        return
    }
    val isForward = state.direction == CurlDirection.FORWARD
    val offset = curlAmount * w
    val incoming = if (isForward) nextImage else previousImage
    if (incoming != null) drawImageBitmap(incoming, w, h) else drawRect(Color(0xFFF5F0E8), size = Size(w, h))
    val tx = if (isForward) -offset else offset
    drawContext.transform.translate(tx, 0f)
    drawCurrentPage(currentImage, w, h)
    drawContext.transform.translate(-tx, 0f)
}

private fun DrawScope.drawPageFade(
    currentImage: ImageBitmap?,
    nextImage: ImageBitmap?,
    previousImage: ImageBitmap?,
    curlAmount: Float,
    state: PageCurlStateData,
    w: Float,
    h: Float
) {
    if (curlAmount <= 0.001f || state.isIdle) {
        drawCurrentPage(currentImage, w, h)
        return
    }
    val isForward = state.direction == CurlDirection.FORWARD
    val incoming = if (isForward) nextImage else previousImage
    if (incoming != null) drawImageBitmap(incoming, w, h, alpha = curlAmount)
    drawCurrentPage(currentImage, w, h, alpha = 1f - curlAmount)
}

private fun DrawScope.drawPageNone(currentImage: ImageBitmap?, w: Float, h: Float) {
    drawCurrentPage(currentImage, w, h)
}

private fun DrawScope.drawCurrentPage(image: ImageBitmap?, w: Float, h: Float, alpha: Float = 1f) {
    if (image != null) drawImageBitmap(image, w, h, alpha)
    else drawRect(Color(0xFFFFFAF2), size = Size(w, h), alpha = alpha)
}

private fun DrawScope.drawImageBitmap(image: ImageBitmap, w: Float, h: Float, alpha: Float = 1f) {
    drawImage(
        image = image,
        srcSize = IntSize(image.width, image.height),
        dstSize = IntSize(w.toInt(), h.toInt()),
        alpha = alpha
    )
}
