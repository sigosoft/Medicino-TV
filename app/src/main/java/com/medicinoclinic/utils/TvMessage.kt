package com.medicinoclinic.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.medicinoclinic.R
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * QA-20: a message that can be read from across a room.
 *
 * Toasts are small, grey and gone in about 2 s. This shows the text large, in a dark box near
 * the bottom of the screen, for 5–10 s depending on its length. A new message replaces the
 * previous one. It's drawn over the current screen, so no layout changes are needed, and it
 * never takes focus from the remote.
 */
object TvMessage {

    private const val TAG_VIEW = "tv_message_view"
    private const val MIN_SHOW_MS = 5_000L
    private const val MAX_SHOW_MS = 10_000L
    private const val MS_PER_CHAR = 70L

    fun show(activity: Activity, text: CharSequence) {
        if (activity.isFinishing || activity.isDestroyed) return
        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        val view = (content.findViewWithTag<TextView>(TAG_VIEW) ?: createView(activity, content))

        (view.getTag(R.id.tv_message_hide) as? Runnable)?.let { view.removeCallbacks(it) }
        view.text = text
        view.alpha = 1f
        view.visibility = View.VISIBLE
        view.bringToFront()

        val showMs = (text.length * MS_PER_CHAR).coerceIn(MIN_SHOW_MS, MAX_SHOW_MS)
        val hide = Runnable {
            view.animate().alpha(0f).setDuration(300).withEndAction {
                view.visibility = View.GONE
            }.start()
        }
        view.setTag(R.id.tv_message_hide, hide)
        view.postDelayed(hide, showMs)
    }

    private fun createView(activity: Activity, content: FrameLayout): TextView {
        val density = activity.resources.displayMetrics.density
        val screenWidth = activity.resources.displayMetrics.widthPixels
        val view = TextView(activity).apply {
            tag = TAG_VIEW
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            gravity = Gravity.CENTER
            maxWidth = (screenWidth * 0.7f).toInt()
            val padH = (32 * density).toInt()
            val padV = (18 * density).toInt()
            setPadding(padH, padV, padH, padV)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E6212121"))
                cornerRadius = 16 * density
            }
            elevation = 12 * density
            isFocusable = false
            isClickable = false
            visibility = View.GONE
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply { bottomMargin = (48 * density).toInt() }
        content.addView(view, params)
        return view
    }

    /** True when the TV has any network connection (Wi-Fi or Ethernet). */
    fun isOnline(context: Context): Boolean = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.activeNetwork != null
    } catch (e: Exception) {
        true // can't tell; let the request try
    }

    /**
     * A message that says what actually went wrong, instead of "Poor network connection" for
     * everything: no network at all, server too slow, or server unreachable.
     */
    fun networkErrorText(context: Context, error: Throwable): String = when {
        !isOnline(context) -> context.getString(R.string.no_internet)
        error is SocketTimeoutException -> context.getString(R.string.server_timeout)
        error is UnknownHostException || error is ConnectException ->
            context.getString(R.string.server_unreachable)
        else -> context.getString(R.string.server_unreachable)
    }
}
