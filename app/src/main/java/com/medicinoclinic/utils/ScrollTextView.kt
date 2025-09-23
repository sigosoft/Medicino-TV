package com.medicinoclinic.utils

import android.R
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import android.widget.TextView


class ScrollTextView(context: Context?, attrs: AttributeSet?, defStyle: Int) :
    androidx.appcompat.widget.AppCompatTextView(context!!, attrs, defStyle) {
    // scrolling feature
    private var mSlr: Scroller? = null

    // milliseconds for a round of scrolling
    var rndDuration = 70000

    // the X offset when paused
    private var mXPaused = 0

    // whether it's being paused
    var isPaused = true
        private set

    /*
       * constructor
       */
    constructor(context: Context?) : this(context, null) {
        // customize the TextView
        setSingleLine()
        ellipsize = null
        visibility = INVISIBLE
    }

    /*
     * constructor
     */
    constructor(context: Context?, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.textViewStyle
    ) {
        // customize the TextView
        setSingleLine()
        ellipsize = null
        visibility = INVISIBLE
    }

    /**
     * begin to scroll the text from the original position
     */
    fun startScroll() {
        // begin from the very right side
        mXPaused = -1 * width
        // assume it's paused
        isPaused = true
        resumeScroll()
    }

    /**
     * resume the scroll from the pausing point
     */
    fun resumeScroll() {
        if (!isPaused) return

        // Do not know why it would not scroll sometimes
        // if setHorizontallyScrolling is called in constructor.
        setHorizontallyScrolling(true)

        // use LinearInterpolator for steady scrolling
        mSlr = Scroller(this.context, LinearInterpolator())
        setScroller(mSlr)
        val scrollingLen = calculateScrollingLen()
        val distance = scrollingLen - (width + mXPaused)
        val duration = (rndDuration * distance * 1.00000
                / scrollingLen).toInt()
        visibility = VISIBLE
        mSlr!!.startScroll(mXPaused, 0, distance, 0, duration)
        invalidate()
        isPaused = false
    }

    /**
     * calculate the scrolling length of the text in pixel
     *
     * @return the scrolling length in pixels
     */
    private fun calculateScrollingLen(): Int {
        val tp = paint
        var rect: Rect? = Rect()
        val strTxt = text.toString()
        tp.getTextBounds(strTxt, 0, strTxt.length, rect)
        val scrollingLen: Int = rect!!.width() + width
        rect = null
        return scrollingLen
    }

    /**
     * pause scrolling the text
     */
    fun pauseScroll() {
        if (null == mSlr) return
        if (isPaused) return
        isPaused = true

        // abortAnimation sets the current X to be the final X,
        // and sets isFinished to be true
        // so current position shall be saved
        mXPaused = mSlr!!.currX
        mSlr!!.abortAnimation()
    }

    /*
     * override the computeScroll to restart scrolling when finished so as that
     * the text is scrolled forever
     */  override fun computeScroll() {
        super.computeScroll()
        if (null == mSlr) return
        if (mSlr!!.isFinished && !isPaused) {
            startScroll()
        }
    }

    /*
     * constructor
     */
    init {
        // customize the TextView
        setSingleLine()
        ellipsize = null
        visibility = INVISIBLE
    }
}