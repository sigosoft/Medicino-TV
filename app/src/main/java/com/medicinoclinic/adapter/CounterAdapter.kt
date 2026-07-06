package com.medicinoclinic.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.R
import com.medicinoclinic.SettingsActivity
import com.medicinoclinic.model.CounterListingModel
import com.medicinoclinic.utils.CounterScrollTextView

class CounterAdapter(private var mList: List<CounterListingModel> = emptyList()) :
    RecyclerView.Adapter<CounterAdapter.ViewHolder>() {

    private var layoutType = resolveLayoutType(0)

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return mList[position].counter_id.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int = layoutType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = when (viewType) {
            TYPE_1_2 -> R.layout.item_counter1
            TYPE_3 -> R.layout.item_counter2
            TYPE_4 -> R.layout.item_counter3
            else -> R.layout.item_counter
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
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
        holder.tvCounterName.setScrollingText(counter.counter_name)
        bindToken(holder, counter)
        adjustItemHeight(holder)

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

        val itemHeight = parent.height / count
        val lp = holder.itemView.layoutParams
        if (lp.height != itemHeight) {
            lp.height = itemHeight
            holder.itemView.layoutParams = lp
        }
    }

    fun setCounters(newList: List<CounterListingModel>) {
        val newLayoutType = resolveLayoutType(newList.size)
        val layoutChanged = newLayoutType != layoutType
        layoutType = newLayoutType

        if (layoutChanged) {
            mList = newList
            notifyDataSetChanged()
            return
        }

        val diffResult = DiffUtil.calculateDiff(CounterDiffCallback(mList, newList))
        mList = newList
        diffResult.dispatchUpdatesTo(this)
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

    private class CounterDiffCallback(
        private val oldList: List<CounterListingModel>,
        private val newList: List<CounterListingModel>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].counter_id == newList[newItemPosition].counter_id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return if (old.counter_name == new.counter_name) PAYLOAD_TOKEN else null
        }
    }

    companion object {
        private const val PAYLOAD_TOKEN = "payload_token"
        private const val TYPE_1_2 = 0
        private const val TYPE_3 = 1
        private const val TYPE_4 = 2
        private const val TYPE_DEFAULT = 3

        fun resolveLayoutType(count: Int): Int = when {
            count <= 2 -> TYPE_1_2
            count == 3 -> TYPE_3
            count == 4 -> TYPE_4
            else -> TYPE_DEFAULT
        }
    }
}
