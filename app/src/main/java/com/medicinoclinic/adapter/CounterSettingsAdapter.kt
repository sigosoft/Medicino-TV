package com.medicinoclinic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.R
import com.medicinoclinic.model.CounterSettingsModel

class CounterSettingsAdapter(
    private val mList: List<CounterSettingsModel>
) : RecyclerView.Adapter<CounterSettingsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_counter_settings, parent, false)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mList[position]
        holder.tvCounterName.text = item.counterName

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isClickable = true
        holder.checkBox.isFocusable = true
        holder.checkBox.isEnabled = true
        holder.checkBox.isChecked = item.isSelected

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
        }

        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
        }
    }

    override fun getItemCount(): Int = mList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCounterName: TextView = itemView.findViewById(R.id.tv_counter_name)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)
    }
}
