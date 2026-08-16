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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

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
    onCenterTap: () -> Unit,
    isZoomed: Boolean,
    modifier: Modifier = Modifier
) {
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var curlState by remember { mutableStateOf(PageCurlStateData()) }
    val curlAmount = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }

    val currentImage = currentBitmap?.asImageBitmap()
    val nextImage = nextBitmap?.asImageBitmap()
    val previousImage = previousBitmap?.asImageBitmap()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
            .pointerInput(isZoomed, pageIndex) {
                if (isZoomed) return@pointerInput
                detectTapGestures { offset ->
                    if (curlState.isAnimating) return@detectTapGestures
                    val third = viewSize.width / 3f
                    when {
                        offset.x < third -> {
                            // Left third: previous page
                            if (pageIndex > 0) {
                                scope.launch {
                                    curlState = curlState.copy(
                                        state = CurlState.ANIMATING_BACKWARD,
                                        direction = CurlDirection.BACKWARD,
                                        pageWidth = viewSize.width.toFloat(),
                                        pageHeight = viewSize.height.toFloat()
                                    )
                                    curlAmount.snapTo(0f)
                                    curlAmount.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(animationDurationMs)
                                    )
                                    onPageBackward()
                                    curlState = PageCurlStateData()
                                    curlAmount.snapTo(0f)
                                }
                            }
                        }
                        offset.x > viewSize.width - third -> {
                            // Right third: next page
                            if (pageIndex < totalPages - 1) {
                                scope.launch {
                                    curlState = curlState.copy(
                                        state = CurlState.ANIMATING_FORWARD,
                                        direction = CurlDirection.FORWARD,
                                        pageWidth = viewSize.width.toFloat(),
                                        pageHeight = viewSize.height.toFloat()
                                    )
                                    curlAmount.snapTo(0f)
                                    curlAmount.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(animationDurationMs)
                                    )
                                    onPageForward()
                                    curlState = PageCurlStateData()
                                    curlAmount.snapTo(0f)
                                }
                            }
                        }
                        else -> onCenterTap()
                    }
                }
            }
            .pointerInput(isZoomed, pageIndex) {
                if (isZoomed) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (!curlState.canStartTurn()) return@detectHorizontalDragGestures
                        curlState = curlState.copy(
                            state = CurlState.DRAGGING,
                            startX = offset.x,
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
                        val currentCurl = curlAmount.value
                        val shouldComplete = CurlGeometry.shouldCompleteTurn(currentCurl)

                        scope.launch {
                            if (shouldComplete) {
                                val dir = curlState.direction
                                curlState = curlState.copy(
                                    state = if (dir == CurlDirection.FORWARD)
                                        CurlState.ANIMATING_FORWARD
                                    else
                                        CurlState.ANIMATING_BACKWARD
                                )
                                curlAmount.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = (animationDurationMs * (1f - currentCurl)).toInt().coerceAtLeast(100)
                                    )
                                )
                                if (dir == CurlDirection.FORWARD) onPageForward()
                                else onPageBackward()
                            } else {
                                curlState = curlState.copy(state = CurlState.ANIMATING_CANCEL)
                                curlAmount.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = (animationDurationMs * currentCurl).toInt().coerceAtLeast(100)
                                    )
                                )
                            }
                            curlState = PageCurlStateData()
                            curlAmount.snapTo(0f)
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (!curlState.isDragging) return@detectHorizontalDragGestures
                        change.consume()
                        velocityTracker.addPosition(
                            change.uptimeMillis,
                            change.position
                        )

                        val newDragX = curlState.dragX + dragAmount
                        val direction = if (dragAmount < 0) CurlDirection.FORWARD else CurlDirection.BACKWARD

                        // Prevent direction changes mid-drag if we've committed
                        val effectiveDir = if (curlState.direction == CurlDirection.NONE) direction
                        else curlState.direction

                        val canTurn = when (effectiveDir) {
                            CurlDirection.FORWARD -> pageIndex < totalPages - 1
                            CurlDirection.BACKWARD -> pageIndex > 0
                            CurlDirection.NONE -> false
                        }

                        if (!canTurn) return@detectHorizontalDragGestures

                        curlState = curlState.copy(
                            dragX = newDragX,
                            direction = effectiveDir
                        )

                        val curl = CurlGeometry.dragToCurlAmount(
                            newDragX,
                            curlState.startX,
                            curlState.pageWidth,
                            effectiveDir == CurlDirection.FORWARD
                        )

                        scope.launch {
                            curlAmount.snapTo(curl)
                        }
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
        // No curl - just draw the current page
        drawCurrentPage(currentImage, w, h)
        return
    }

    val isForward = state.direction == CurlDirection.FORWARD
    val mesh = CurlGeometry.computeCurl(curlAmount, w, h, isForward)

    // 1. Draw the page underneath (next or previous page)
    val underImage = if (isForward) nextImage else previousImage
    if (underImage != null) {
        drawImage(
            image = underImage,
            dstSize = IntSize(w.toInt(), h.toInt())
        )
    } else {
        drawRect(Color(0xFFF5F0E8), size = Size(w, h))
    }

    // 2. Draw shadow on the underlying page
    clipPath(mesh.curlShadowPath) {
        drawRect(
            Color.Black.copy(alpha = mesh.shadowAlpha),
            size = Size(w, h)
        )
    }

    // 3. Draw the flat part of the current page (not yet curled)
    clipPath(mesh.frontClipPath) {
        drawCurrentPage(currentImage, w, h)
    }

    // 4. Draw the curled/folded part (back of page)
    clipPath(mesh.curlPath) {
        // Back of page: slightly darker version
        drawRect(Color(0xFFEDE6DA), size = Size(w, h))
        // Add a subtle gradient for the curl cylinder effect
        val gradStart = if (isForward) Offset(mesh.curlTipX, 0f) else Offset(mesh.curlTipX, 0f)
        val gradEnd = if (isForward) {
            Offset(mesh.curlTipX + w * 0.1f, 0f)
        } else {
            Offset(mesh.curlTipX - w * 0.1f, 0f)
        }
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.15f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.05f)
                ),
                start = gradStart,
                end = gradEnd
            ),
            size = Size(w, h)
        )
    }

    // 5. Draw fold line highlight (edge of the page)
    val lineX = mesh.curlTipX
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = Offset(lineX, 0f),
        end = Offset(lineX, h),
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

    // Draw incoming page
    val incomingImage = if (isForward) nextImage else previousImage
    if (incomingImage != null) {
        drawImage(image = incomingImage, dstSize = IntSize(w.toInt(), h.toInt()))
    } else {
        drawRect(Color(0xFFF5F0E8), size = Size(w, h))
    }

    // Draw current page sliding out
    val translateX = if (isForward) -offset else offset
    drawContext.transform.translate(translateX, 0f)
    drawCurrentPage(currentImage, w, h)
    // Draw shadow on edge
    drawRect(
        Color.Black.copy(alpha = 0.1f * curlAmount),
        topLeft = Offset(if (isForward) w - 30f else 0f, 0f),
        size = Size(30f, h)
    )
    drawContext.transform.translate(-translateX, 0f)
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
    val incomingImage = if (isForward) nextImage else previousImage

    // Draw incoming with increasing alpha
    if (incomingImage != null) {
        drawImage(
            image = incomingImage,
            dstSize = IntSize(w.toInt(), h.toInt()),
            alpha = curlAmount
        )
    }

    // Draw current with decreasing alpha
    drawCurrentPage(currentImage, w, h, alpha = 1f - curlAmount)
}

private fun DrawScope.drawPageNone(currentImage: ImageBitmap?, w: Float, h: Float) {
    drawCurrentPage(currentImage, w, h)
}

private fun DrawScope.drawCurrentPage(image: ImageBitmap?, w: Float, h: Float, alpha: Float = 1f) {
    if (image != null) {
        drawImage(
            image = image,
            dstSize = IntSize(w.toInt(), h.toInt()),
            alpha = alpha
        )
    } else {
        // Loading state: draw paper-like background
        drawRect(Color(0xFFFFFAF2), size = Size(w, h), alpha = alpha)
    }
}

private fun DrawScope.drawImage(
    image: ImageBitmap,
    dstSize: IntSize,
    alpha: Float = 1f
) {
    drawImage(
        image = image,
        srcSize = IntSize(image.width, image.height),
        dstSize = dstSize,
        alpha = alpha
    )
}
