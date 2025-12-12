package com.medicinoclinic.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.R
import com.medicinoclinic.SettingsActivity
import com.medicinoclinic.model.CounterListingModel

class CounterAdapter(private var mList: List<CounterListingModel>) : RecyclerView.Adapter<CounterAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        // Determine the correct layout based on the item count
        val view: View = when {
            mList.size <= 2 -> layoutInflater.inflate(R.layout.item_counter1, parent, false)
            mList.size == 3 -> layoutInflater.inflate(R.layout.item_counter2, parent, false)
            mList.size == 4 -> layoutInflater.inflate(R.layout.item_counter3, parent, false)
            else -> layoutInflater.inflate(R.layout.item_counter, parent, false)
        }

        view.isFocusable = true
        view.isFocusableInTouchMode = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val counter = mList[position]

        // --- Marquee logic is now split for performance ---
        // 1. Set the text for the counter name
        holder.tvCounterName.text = counter.counter_name

        // 2. Conditionally enable the marquee based on text length.
        // This is a fast operation and is safe to do in onBindViewHolder.
        holder.tvCounterName.isSelected = counter.counter_name.length > 7

        // Simplified and safer way to handle the token text
        val tokenText = counter.token?.trim()
        if (tokenText.isNullOrEmpty() || tokenText.equals("null", ignoreCase = true)) {
            holder.tv_token.text = "0"
        } else {
            holder.tv_token.text = tokenText
        }

        // Click listener for navigation
        holder.itemView.setOnClickListener { view ->
            // The isFocusable check here is redundant since you set it in onCreateViewHolder.
            // It's safe to keep, but not strictly necessary.
            view.context.startActivity(Intent(view.context, SettingsActivity::class.java))
        }

        // An empty focus change listener is not usually needed unless you want to override default behavior.
        // holder.itemView.setOnFocusChangeListener { _, _ -> }
    }

    override fun getItemCount(): Int = mList.size

    /**
     * ViewHolder for the counter items.
     * The init block handles one-time setup for views to improve performance.
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCounterName: TextView = itemView.findViewById(R.id.tv_name)
        val tv_token: TextView = itemView.findViewById(R.id.tv_token)

        // This init block runs only ONCE when a new ViewHolder is created
        init {
            // Set the marquee speed once and for all for this TextView instance.
            // This avoids doing slow reflection during every scroll.
            setMarqueeSpeed(tvCounterName, 20f)
        }

        /**
         * Sets the scrolling speed of a TextView's marquee using reflection.
         * This is a private helper function, encapsulated within the ViewHolder.
         */
        private fun setMarqueeSpeed(tv: TextView, speed: Float) {
            try {
                val field = if (tv is AppCompatTextView) {
                    // For AppCompatTextView, marquee is in the superclass
                    tv.javaClass.superclass?.getDeclaredField("mMarquee")
                } else {
                    tv.javaClass.getDeclaredField("mMarquee")
                }
                field?.let {
                    it.isAccessible = true
                    val marquee = it[tv]
                    if (marquee != null) {
                        val scrollSpeedFieldName = "mPixelsPerSecond"
                        val speedField = marquee.javaClass.getDeclaredField(scrollSpeedFieldName)
                        speedField.isAccessible = true
                        speedField.setFloat(marquee, speed)
                    }
                }
            } catch (e: Exception) {
                // Log the exception to help with debugging if it ever fails
                Log.e("CounterAdapter", "Failed to set marquee speed via reflection.", e)
            }
        }
    }

    /**
     * Updates the list of counters and notifies the adapter to refresh the UI.
     */
    fun setCounters(newList: List<CounterListingModel>) {
        mList = newList
        notifyDataSetChanged()
    }

    // The global setMarqueeSpeed function is no longer needed here as it's
    // now a private part of the ViewHolder's implementation.
}
