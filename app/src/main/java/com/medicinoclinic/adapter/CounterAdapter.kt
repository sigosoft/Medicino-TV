package com.medicinoclinic.adapter

import android.content.Intent
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.R
import com.medicinoclinic.SettingsActivity
import com.medicinoclinic.model.CounterListingModel
import com.medicinoclinic.utils.CounterScrollTextView

class CounterAdapter(private var mList: List<CounterListingModel>) :
    RecyclerView.Adapter<CounterAdapter.ViewHolder>() {

    private var scrollLayoutMode = VIEW_TYPE_DEFAULT

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        val counterId = mList[position].counter_id.trim()
        if (counterId.isEmpty() || counterId.equals("null", ignoreCase = true)) {
            return RecyclerView.NO_ID
        }
        return counterId.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return if (mList[position].scroll_up.trim() == "2") {
            VIEW_TYPE_STATIC
        } else {
            scrollLayoutMode
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view: View = when (viewType) {
            VIEW_TYPE_STATIC -> layoutInflater.inflate(R.layout.item_counter_static, parent, false)
            VIEW_TYPE_SMALL -> layoutInflater.inflate(R.layout.item_counter1, parent, false)
            VIEW_TYPE_MEDIUM -> layoutInflater.inflate(R.layout.item_counter2, parent, false)
            VIEW_TYPE_LARGE -> layoutInflater.inflate(R.layout.item_counter3, parent, false)
            else -> layoutInflater.inflate(R.layout.item_counter, parent, false)
        }

        // Never use MATCH_PARENT before RV is measured — that causes intermittent clipping
        // / missing names on slower Android TVs.
        val initialHeight = if (viewType == VIEW_TYPE_STATIC && parent.height > 0) {
            parent.height / mList.size.coerceAtLeast(1)
        } else {
            ViewGroup.LayoutParams.WRAP_CONTENT
        }
        view.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            initialHeight
        )

        view.isFocusable = true
        view.isFocusableInTouchMode = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_TOKEN)) {
            bindToken(holder, mList[position])
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < 0 || position >= mList.size) return
        val counter = mList[position]
        holder.boundCounterId = counter.counter_id
        holder.boundCounterName = counter.counter_name
        holder.boundScrollUp = counter.scroll_up

        val disableScroll = counter.scroll_up.trim() == "2"

        bindToken(holder, counter)
        // Bind name immediately so it never depends only on a delayed post.
        bindCounterNameNow(holder, counter)

        if (disableScroll) {
            applyEqualItemHeight(holder)
            holder.itemView.post {
                if (!holder.isStillBoundTo(counter.counter_id)) return@post
                applyEqualItemHeight(holder)
                bindCounterNameNow(holder, counter)
                fitTokenToBounds(holder)
            }
        } else {
            resetItemHeight(holder)
        }

        holder.itemView.setOnClickListener { view ->
            view.context.startActivity(Intent(view.context, SettingsActivity::class.java))
        }
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION || position >= mList.size) return
        val counter = mList[position]
        if (counter.scroll_up.trim() == "2") {
            applyEqualItemHeight(holder)
        }
        // Re-assert name after attach — fixes intermittent blank names on some TVs.
        bindCounterNameNow(holder, counter)
        holder.tvCounterName.refreshDisplayedText()
    }

    override fun getItemCount(): Int = mList.size

    private fun bindToken(holder: ViewHolder, counter: CounterListingModel) {
        val tokenText = counter.token.trim()
        val displayToken = when {
            tokenText.isEmpty() || tokenText.equals("null", ignoreCase = true) -> "0"
            else -> tokenText
        }

        val disableScroll = counter.scroll_up.trim() == "2"
        if (holder.tv_token.text?.toString() != displayToken) {
            holder.tv_token.text = displayToken
        }

        if (disableScroll) {
            fitTokenToBounds(holder)
        } else {
            resetTokenSize(holder)
        }
    }

    private fun bindCounterNameNow(holder: ViewHolder, counter: CounterListingModel) {
        val counterName = counter.counter_name.trim()
        val disableScroll = counter.scroll_up.trim() == "2"
        holder.tvCounterName.visibility = View.VISIBLE
        holder.tvCounterName.alpha = 1f
        if (disableScroll) {
            holder.tvCounterName.setStaticText(counterName)
            fitTokenToBounds(holder)
        } else {
            holder.tvCounterName.setScrollingText(counterName)
            resetTokenSize(holder)
        }
    }

    private fun fitTokenToBounds(holder: ViewHolder) {
        val tokenView = holder.tv_token
        tokenView.post {
            if (holder.bindingAdapterPosition == RecyclerView.NO_POSITION) return@post
            if (tokenView.width <= 0 || tokenView.height <= 0) return@post
            val maxSp = pxToSp(tokenView, holder.defaultTokenSizePx).coerceAtLeast(18)
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                tokenView,
                14,
                maxSp,
                1,
                TypedValue.COMPLEX_UNIT_SP
            )
            tokenView.maxLines = 1
            tokenView.gravity = android.view.Gravity.CENTER
        }
    }

    private fun resetTokenSize(holder: ViewHolder) {
        val tokenView = holder.tv_token
        TextViewCompat.setAutoSizeTextTypeWithDefaults(
            tokenView,
            TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
        )
        if (holder.defaultTokenSizePx > 0f) {
            tokenView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.defaultTokenSizePx)
        }
    }

    private fun pxToSp(view: View, px: Float): Int {
        val scaledDensity = view.resources.displayMetrics.scaledDensity
        return (px / scaledDensity).toInt().coerceAtLeast(14)
    }

    private fun applyEqualItemHeight(holder: ViewHolder) {
        val parent = holder.itemView.parent as? RecyclerView ?: run {
            holder.itemView.post {
                if (holder.bindingAdapterPosition == RecyclerView.NO_POSITION) return@post
                applyEqualItemHeight(holder)
            }
            return
        }
        if (parent.height > 0) {
            setEqualHeight(holder, parent)
            return
        }
        parent.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (parent.height <= 0) return
                parent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (holder.bindingAdapterPosition == RecyclerView.NO_POSITION) return
                setEqualHeight(holder, parent)
                val pos = holder.bindingAdapterPosition
                if (pos in mList.indices) {
                    bindCounterNameNow(holder, mList[pos])
                }
            }
        })
    }

    private fun setEqualHeight(holder: ViewHolder, parent: RecyclerView) {
        val count = itemCount
        if (count <= 0 || parent.height <= 0) return

        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return

        val itemHeight = itemHeightForPosition(position, parent.height, count)
        val lp = holder.itemView.layoutParams as? RecyclerView.LayoutParams
            ?: RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight)
        if (lp.height != itemHeight || lp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = itemHeight
            holder.itemView.layoutParams = lp
        }
    }

    private fun resetItemHeight(holder: ViewHolder) {
        val lp = holder.itemView.layoutParams ?: return
        if (lp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            holder.itemView.layoutParams = lp
        }
    }

    private fun itemHeightForPosition(position: Int, parentHeight: Int, count: Int): Int {
        val baseHeight = parentHeight / count
        val remainder = parentHeight % count
        return baseHeight + if (position < remainder) 1 else 0
    }

    fun setCounters(newList: List<CounterListingModel>) {
        scrollLayoutMode = layoutModeForCount(newList.size)
        mList = newList
        notifyDataSetChanged()
    }

    private fun layoutModeForCount(count: Int): Int = when {
        count <= 2 -> VIEW_TYPE_SMALL
        count == 3 -> VIEW_TYPE_MEDIUM
        count == 4 -> VIEW_TYPE_LARGE
        else -> VIEW_TYPE_DEFAULT
    }

    fun updateTokenForCounter(counterId: String, newToken: String) {
        val index = mList.indexOfFirst { it.counter_id == counterId }
        if (index < 0) return

        val current = mList[index]
        if (current.token.trim() == newToken.trim()) return

        val updatedList = mList.toMutableList()
        updatedList[index] = current.copy(token = newToken)
        mList = updatedList
        notifyItemChanged(index, PAYLOAD_TOKEN)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCounterName: CounterScrollTextView = itemView.findViewById(R.id.tv_name)
        val tv_token: TextView = itemView.findViewById(R.id.tv_token)
        val defaultTokenSizePx: Float = tv_token.textSize
        var boundCounterId: String = ""
        var boundCounterName: String = ""
        var boundScrollUp: String = "1"

        fun isStillBoundTo(counterId: String): Boolean {
            return bindingAdapterPosition != RecyclerView.NO_POSITION && boundCounterId == counterId
        }
    }

    companion object {
        private const val PAYLOAD_TOKEN = "payload_token"
        private const val VIEW_TYPE_SMALL = 1
        private const val VIEW_TYPE_MEDIUM = 2
        private const val VIEW_TYPE_LARGE = 3
        private const val VIEW_TYPE_DEFAULT = 4
        private const val VIEW_TYPE_STATIC = 5
    }
}
