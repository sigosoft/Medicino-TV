package com.medicinoclinic.utils

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView

/**
 * Horizontal ticker for counter names. Keeps its scroll position when sibling views
 * (token TextView) update or the row is re-laid out.
 */
class CounterScrollTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var sourceText: String = ""
    private var scrollAnimator: ValueAnimator? = null
    private val gap = "          "
    private val pixelsPerSecond = 45f

    init {
        setSingleLine()
        ellipsize = null
        isHorizontalScrollBarEnabled = false
        setHorizontallyScrolling(true)
    }

    fun setScrollingText(text: String) {
        if (text == sourceText) return

        sourceText = text
        stopScrolling()

        if (text.isEmpty()) {
            setText("")
            scrollTo(0, 0)
            return
        }

        post { applyTextAndMaybeScroll() }
    }

    private fun applyTextAndMaybeScroll() {
        if (width == 0) {
            post { applyTextAndMaybeScroll() }
            return
        }

        if (!needsScrolling(sourceText)) {
            gravity = Gravity.CENTER
            setText(sourceText)
            scrollTo(0, 0)
            return
        }

        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        setText(sourceText + gap + sourceText + gap)
        scrollTo(0, 0)
        startScrolling()
    }

    private fun needsScrolling(text: String): Boolean {
        if (text.isEmpty() || width == 0) return false
        return paint.measureText(text) > width
    }

    private fun startScrolling() {
        if (sourceText.isEmpty() || width == 0) return

        val loopWidth = paint.measureText(sourceText + gap)
        if (loopWidth <= width) return

        scrollAnimator?.cancel()
        scrollAnimator = ValueAnimator.ofFloat(0f, loopWidth).apply {
            duration = ((loopWidth / pixelsPerSecond) * 1000).toLong().coerceAtLeast(5000L)
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { animator ->
                scrollTo((animator.animatedValue as Float).toInt(), 0)
            }
            start()
        }
    }

    private fun stopScrolling() {
        scrollAnimator?.cancel()
        scrollAnimator = null
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val currentScroll = scrollX
        val animator = scrollAnimator
        val wasRunning = animator?.isRunning == true
        val animatedX = if (wasRunning) animator?.animatedValue as? Float else null

        super.onLayout(changed, left, top, right, bottom)

        if (wasRunning && animatedX != null) {
            scrollTo(animatedX.toInt(), 0)
        } else if (currentScroll > 0 && scrollAnimator != null) {
            scrollTo(currentScroll, 0)
        }
    }

    override fun onDetachedFromWindow() {
        stopScrolling()
        super.onDetachedFromWindow()
    }
}
