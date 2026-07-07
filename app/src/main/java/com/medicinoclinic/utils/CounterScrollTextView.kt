package com.medicinoclinic.utils

import android.content.Context
import android.util.AttributeSet
import android.view.Choreographer
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView

/**
 * Smooth seamless horizontal ticker — two counter names visible, no jump at loop.
 */
class CounterScrollTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var sourceText: String = ""
    private var segmentWidth = 0f
    private var scrollOffset = 0f
    private var lastFrameTimeNanos = 0L
    private var tickerRunning = false

    private val gap = "        "
    private val pixelsPerSecond = 50f

    private val frameCallback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!tickerRunning || segmentWidth <= 0f) return

            if (lastFrameTimeNanos != 0L) {
                val deltaSeconds = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
                scrollOffset += pixelsPerSecond * deltaSeconds
                if (scrollOffset >= segmentWidth) {
                    scrollOffset %= segmentWidth
                }
                scrollTo(scrollOffset.toInt(), 0)
            }
            lastFrameTimeNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    init {
        setSingleLine()
        ellipsize = null
        isHorizontalScrollBarEnabled = false
        setHorizontallyScrolling(true)
    }

    fun setScrollingText(text: String) {
        val sameText = text == sourceText
        sourceText = text
        stopScrolling()

        if (text.isEmpty()) {
            setText("")
            scrollTo(0, 0)
            return
        }

        if (sameText && tickerRunning) return

        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        scrollTo(0, 0)
        post { startScrollingTicker() }
    }

    private fun startScrollingTicker() {
        if (sourceText.isEmpty()) return
        if (width == 0) {
            post { startScrollingTicker() }
            return
        }

        setHorizontallyScrolling(true)
        gravity = Gravity.START or Gravity.CENTER_VERTICAL

        val loopSegment = buildLoopSegment()
        segmentWidth = paint.measureText(loopSegment)
        if (segmentWidth <= 0f) return

        setText(loopSegment + loopSegment)
        scrollOffset = 0f
        lastFrameTimeNanos = 0L
        scrollTo(0, 0)

        tickerRunning = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    /** Two names per cycle: "Name        Name        " */
    private fun buildLoopSegment(): String {
        val nameWithGap = sourceText + gap
        return nameWithGap + nameWithGap
    }

    private fun stopScrolling() {
        tickerRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        lastFrameTimeNanos = 0L
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (sourceText.isNotEmpty() && w > 0 && w != oldw) {
            post { startScrollingTicker() }
        }
    }

    override fun onDetachedFromWindow() {
        stopScrolling()
        super.onDetachedFromWindow()
    }
}
