package com.pranav.flipbook.ui.reader.pagecurl

enum class CurlDirection {
    NONE, FORWARD, BACKWARD
}

enum class CurlState {
    IDLE,
    DRAGGING_FORWARD,
    COMPLETING_FORWARD,
    CANCELING_FORWARD,
    DRAGGING_BACKWARD,
    COMPLETING_BACKWARD,
    CANCELING_BACKWARD
}

data class PageCurlStateData(
    val state: CurlState = CurlState.IDLE,
    val direction: CurlDirection = CurlDirection.NONE,
    val curlPosition: Float = 0f, // 0 = flat, 1 = fully turned
    val dragX: Float = 0f,
    val dragY: Float = 0f,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val pageWidth: Float = 0f,
    val pageHeight: Float = 0f
) {
    val isAnimating: Boolean
        get() = state == CurlState.COMPLETING_FORWARD ||
                state == CurlState.COMPLETING_BACKWARD ||
                state == CurlState.CANCELING_FORWARD ||
                state == CurlState.CANCELING_BACKWARD

    val isDragging: Boolean
        get() = state == CurlState.DRAGGING_FORWARD ||
                state == CurlState.DRAGGING_BACKWARD

    val isIdle: Boolean
        get() = state == CurlState.IDLE

    fun canStartTurn(): Boolean = isIdle
}
