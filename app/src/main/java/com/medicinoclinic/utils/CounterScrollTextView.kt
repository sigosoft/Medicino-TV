package com.medicinoclinic.utils

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Choreographer
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import kotlin.math.abs
import kotlin.math.min

/**
 * Smooth seamless horizontal ticker for counter names.
 * scroll_up=1: scrolling ticker, font fitted to the row so mixed pages cannot overflow
 * scroll_up=2: static 2-line name, auto-sized to the card
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
    private var sizeRetryCount = 0

    private val gap = "        "
    private val pixelsPerSecond = 50f
    private val maxSizeRetries = 8

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
        includeFontPadding = false
        applyScrollingMode()
    }

    fun setScrollingText(text: String) {
        scrollingEnabled = true
        applyScrollingMode()
        sourceText = text
        sizeRetryCount = 0
        stopScrolling()

        if (text.isEmpty()) {
            setText("")
            scrollTo(0, 0)
            return
        }

        gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
        scrollTo(0, 0)
        setText(text)
        post { startScrollingTicker() }
    }

    /**
     * Disable ticker and show counter name on 2 centered lines, sized to fit the card.
     */
    fun setStaticText(text: String) {
        scrollingEnabled = false
        sourceText = text
        sizeRetryCount = 0
        stopScrolling()
        applyStaticMode()

        scrollTo(0, 0)
        val display = formatTwoLineName(text)
        setText(display)
        if (width <= 0 || height <= 0) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSizePx)
        }
        post { fitStaticTextToBounds() }
    }

    /** Re-apply current mode after attach / layout on slow TVs. */
    fun refreshDisplayedText() {
        if (sourceText.isEmpty()) return
        if (scrollingEnabled) {
            post { startScrollingTicker() }
        } else {
            if (text.isNullOrBlank()) {
                setText(formatTwoLineName(sourceText))
            }
            post { fitStaticTextToBounds() }
        }
    }

    private fun applyScrollingMode() {
        TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSizePx)
        setSingleLine()
        maxLines = 1
        ellipsize = null
        includeFontPadding = false
        isHorizontalScrollBarEnabled = false
        setHorizontallyScrolling(true)
        visibility = VISIBLE
        alpha = 1f
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
        visibility = VISIBLE
        alpha = 1f
    }

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
        if (!isAttachedToWindow) return

        if (width <= 0 || height <= 0) {
            if (sizeRetryCount < maxSizeRetries) {
                sizeRetryCount++
                post { fitStaticTextToBounds() }
            }
            return
        }
        sizeRetryCount = 0

        val maxSp = pxToSp(defaultTextSizePx).coerceAtLeast(16)
        val minSp = 12
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            this,
            minSp,
            maxSp,
            1,
            TypedValue.COMPLEX_UNIT_SP
        )
        if (text.isNullOrBlank()) {
            setText(formatTwoLineName(sourceText))
        }
    }

    private fun pxToSp(px: Float): Int {
        val scaledDensity = resources.displayMetrics.scaledDensity
        return (px / scaledDensity).toInt().coerceAtLeast(12)
    }

    private fun spToPx(sp: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp.toFloat(),
            resources.displayMetrics
        )
    }

    private fun fitScrollingFontToHeight() {
        val availableHeight = (height - paddingTop - paddingBottom).toFloat()
        if (availableHeight <= 0f) return

        TextViewCompat.setAutoSizeTextTypeWithDefaults(this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
        val minSize = spToPx(12)
        var size = min(defaultTextSizePx, availableHeight * 0.9f).coerceAtLeast(minSize)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, size)

        var metrics = paint.fontMetrics
        var textHeight = metrics.descent - metrics.ascent
        while (textHeight > availableHeight && size > minSize) {
            size = (size - 1f).coerceAtLeast(minSize)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
            metrics = paint.fontMetrics
            textHeight = metrics.descent - metrics.ascent
        }
    }

    private fun startScrollingTicker() {
        if (!scrollingEnabled || sourceText.isEmpty()) return
        if (!isAttachedToWindow) return

        if (width <= 0 || height <= 0) {
            if (sizeRetryCount < maxSizeRetries) {
                sizeRetryCount++
                if (text.isNullOrBlank()) setText(sourceText)
                post { startScrollingTicker() }
            }
            return
        }
        sizeRetryCount = 0
        includeFontPadding = false
        fitScrollingFontToHeight()

        setHorizontallyScrolling(true)
        gravity = Gravity.START or Gravity.CENTER_VERTICAL

        val loopSegment = buildLoopSegment()
        segmentWidth = paint.measureText(loopSegment)
        if (segmentWidth <= 0f) {
            setText(sourceText)
            return
        }

        setText(loopSegment + loopSegment)
        scrollOffset = 0f
        lastFrameTimeNanos = 0L
        scrollTo(0, 0)

        tickerRunning = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

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
        if (w <= 0 || h <= 0 || sourceText.isEmpty()) return
        if (scrollingEnabled && (w != oldw || h != oldh)) {
            post { startScrollingTicker() }
        } else if (!scrollingEnabled && (w != oldw || h != oldh)) {
            post { fitStaticTextToBounds() }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        refreshDisplayedText()
    }

    override fun onDetachedFromWindow() {
        stopScrolling()
        super.onDetachedFromWindow()
    }
}
