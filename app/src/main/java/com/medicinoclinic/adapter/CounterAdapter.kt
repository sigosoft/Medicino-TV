package com.medicinoclinic.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.R
import com.medicinoclinic.SettingsActivity
import com.medicinoclinic.model.CounterListingModel
import com.medicinoclinic.utils.CounterScrollTextView

class CounterAdapter(private var mList: List<CounterListingModel>) :
    RecyclerView.Adapter<CounterAdapter.ViewHolder>() {

    private var layoutMode = VIEW_TYPE_DEFAULT

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

    override fun getItemViewType(position: Int): Int = layoutMode

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view: View = when (viewType) {
            VIEW_TYPE_SMALL -> layoutInflater.inflate(R.layout.item_counter1, parent, false)
            VIEW_TYPE_MEDIUM -> layoutInflater.inflate(R.layout.item_counter2, parent, false)
            VIEW_TYPE_LARGE -> layoutInflater.inflate(R.layout.item_counter3, parent, false)
            else -> layoutInflater.inflate(R.layout.item_counter, parent, false)
        }
        view.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
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
        val counter = mList[position]
        bindToken(holder, counter)
        adjustItemHeight(holder)
        bindCounterName(holder, counter.counter_name)

        holder.itemView.setOnClickListener { view ->
            view.context.startActivity(Intent(view.context, SettingsActivity::class.java))
        }
    }

    override fun getItemCount(): Int = mList.size

    private fun bindToken(holder: ViewHolder, counter: CounterListingModel) {
        val tokenText = counter.token.trim()
        val displayToken = when {
            tokenText.isEmpty() || tokenText.equals("null", ignoreCase = true) -> "0"
            else -> tokenText
        }
        if (holder.tv_token.text?.toString() == displayToken) return

        val row = holder.itemView as ViewGroup
        row.suppressLayout(true)
        try {
            holder.tv_token.text = displayToken
        } finally {
            row.suppressLayout(false)
        }
    }

    private fun bindCounterName(holder: ViewHolder, counterName: String) {
        holder.itemView.post {
            if (holder.bindingAdapterPosition == RecyclerView.NO_POSITION) return@post
            holder.tvCounterName.setScrollingText(counterName)
        }
    }

    private fun adjustItemHeight(holder: ViewHolder) {
        val parent = holder.itemView.parent as? RecyclerView ?: return
        if (parent.height > 0) {
            applyItemHeight(holder, parent)
            return
        }
        parent.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                parent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                applyItemHeight(holder, parent)
            }
        })
    }

    private fun applyItemHeight(holder: ViewHolder, parent: RecyclerView) {
        val count = itemCount
        if (count <= 0 || parent.height <= 0) return

        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return

        val itemHeight = itemHeightForPosition(position, parent.height, count)
        val lp = holder.itemView.layoutParams
        if (lp.height != itemHeight) {
            lp.height = itemHeight
            holder.itemView.layoutParams = lp
        }
    }

    private fun itemHeightForPosition(position: Int, parentHeight: Int, count: Int): Int {
        val baseHeight = parentHeight / count
        val remainder = parentHeight % count
        return baseHeight + if (position < remainder) 1 else 0
    }

    fun setCounters(newList: List<CounterListingModel>) {
        layoutMode = layoutModeForCount(newList.size)
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
    }

    companion object {
        private const val PAYLOAD_TOKEN = "payload_token"
        private const val VIEW_TYPE_SMALL = 1
        private const val VIEW_TYPE_MEDIUM = 2
        private const val VIEW_TYPE_LARGE = 3
        private const val VIEW_TYPE_DEFAULT = 4
    }
}
