package com.medicinoclinic.utils

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Choreographer
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import kotlin.math.abs

/**
 * Smooth seamless horizontal ticker — two counter names visible, no jump at loop.
 * When scrolling is disabled (scroll_up=2), shows counter name on exactly 2 lines,
 * auto-sized to fit the available counter card without overflow.
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
    private var scrollingEnabled = true
    private var defaultTextSizePx: Float = textSize

    private val gap = "        "
    private val pixelsPerSecond = 50f

    private val frameCallback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!tickerRunning || !scrollingEnabled || segmentWidth <= 0f) return

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
        defaultTextSizePx = textSize
        applyScrollingMode()
    }

    fun setScrollingText(text: String) {
        scrollingEnabled = true
        applyScrollingMode()

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

    /**
     * Disable ticker and show counter name on 2 centered lines, sized to fit the card.
     * Example: "Test Counter 2" -> "Test\nCounter 2"
     */
    fun setStaticText(text: String) {
        scrollingEnabled = false
        sourceText = text
        stopScrolling()
        applyStaticMode()

        scrollTo(0, 0)
        setText(formatTwoLineName(text))
        post { fitStaticTextToBounds() }
    }

    private fun applyScrollingMode() {
        TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSizePx)
        setSingleLine()
        maxLines = 1
        ellipsize = null
        isHorizontalScrollBarEnabled = false
        setHorizontallyScrolling(true)
    }

    private fun applyStaticMode() {
        setSingleLine(false)
        isSingleLine = false
        maxLines = 2
        ellipsize = null
        setHorizontallyScrolling(false)
        gravity = Gravity.CENTER
        isHorizontalScrollBarEnabled = false
        includeFontPadding = false
    }

    /** Split words into two balanced lines so the full name is readable. */
    private fun formatTwoLineName(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return trimmed

        val words = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size <= 1) return trimmed

        var bestSplit = 1
        var bestDiff = Int.MAX_VALUE
        for (i in 1 until words.size) {
            val line1 = words.take(i).joinToString(" ")
            val line2 = words.drop(i).joinToString(" ")
            val diff = abs(line1.length - line2.length)
            if (diff < bestDiff) {
                bestDiff = diff
                bestSplit = i
            }
        }
        return words.take(bestSplit).joinToString(" ") + "\n" + words.drop(bestSplit).joinToString(" ")
    }

    private fun fitStaticTextToBounds() {
        if (scrollingEnabled || sourceText.isEmpty()) return
        if (width <= 0 || height <= 0) {
            post { fitStaticTextToBounds() }
            return
        }

        val maxSp = pxToSp(defaultTextSizePx).coerceAtLeast(16)
        val minSp = 12
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            this,
            minSp,
            maxSp,
            1,
            TypedValue.COMPLEX_UNIT_SP
        )
    }

    private fun pxToSp(px: Float): Int {
        val scaledDensity = resources.displayMetrics.scaledDensity
        return (px / scaledDensity).toInt().coerceAtLeast(12)
    }

    private fun startScrollingTicker() {
        if (!scrollingEnabled || sourceText.isEmpty()) return
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
        if (w <= 0 || h <= 0) return
        if (scrollingEnabled && sourceText.isNotEmpty() && w != oldw) {
            post { startScrollingTicker() }
        } else if (!scrollingEnabled && sourceText.isNotEmpty() && (w != oldw || h != oldh)) {
            post { fitStaticTextToBounds() }
        }
    }

    override fun onDetachedFromWindow() {
        stopScrolling()
        super.onDetachedFromWindow()
    }
}
