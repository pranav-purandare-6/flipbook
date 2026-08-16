package com.pranav.flipbook.ui.reader.pagecurl

enum class CurlDirection {
    NONE, FORWARD, BACKWARD
}

enum class CurlState {
    IDLE,
    DRAGGING,
    ANIMATING_FORWARD,
    ANIMATING_BACKWARD,
    ANIMATING_CANCEL
}

data class PageCurlStateData(
    val state: CurlState = CurlState.IDLE,
    val direction: CurlDirection = CurlDirection.NONE,
    val curlPosition: Float = 1f, // 0=fully turned, 1=flat
    val dragX: Float = 0f,
    val dragY: Float = 0f,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val pageWidth: Float = 0f,
    val pageHeight: Float = 0f
) {
    val isAnimating: Boolean
        get() = state == CurlState.ANIMATING_FORWARD ||
                state == CurlState.ANIMATING_BACKWARD ||
                state == CurlState.ANIMATING_CANCEL

    val isDragging: Boolean
        get() = state == CurlState.DRAGGING

    val isIdle: Boolean
        get() = state == CurlState.IDLE

    fun canStartTurn(): Boolean = isIdle
}
