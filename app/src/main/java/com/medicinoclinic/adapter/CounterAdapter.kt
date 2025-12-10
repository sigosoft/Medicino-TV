package com.medicinoclinic.adapter

import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.R
import com.medicinoclinic.SettingsActivity
import com.medicinoclinic.model.CounterListingModel
import com.medicinoclinic.model.DoctorListingModel
import java.lang.reflect.Field

class CounterAdapter(private var mList: List<CounterListingModel>) : RecyclerView.Adapter<CounterAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view: View = if (mList.size <= 2) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_counter1, parent, false)
        } else if (mList.size == 3) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_counter2, parent, false)
        }else if (mList.size == 4) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_counter3, parent, false)
        }
        else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_counter, parent, false)
        }

        view.isFocusable = true
        view.isFocusableInTouchMode = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val counter = mList[position]
        setMarqueeSpeed(holder.tvCounterName, 20f)

        holder.tvCounterName.isSelected = counter.counter_name.length > 7
        holder.tvCounterName.text = counter.counter_name
//        holder.tv_room.text.
        holder.tv_token.text =  counter.token.trim().ifEmpty { "0" }
        if(counter.token.trim().equals("null")){
            holder.tv_token.text = "0"
        } else {
            holder.tv_token.text = counter.token.trim()
        }
        holder.itemView.setOnFocusChangeListener { _, _ -> }
        holder.itemView.setOnClickListener { view ->
            if (view.isFocusable) {
                view.context.startActivity(Intent(view.context, SettingsActivity::class.java))
            }
        }
    }

    override fun getItemCount(): Int = mList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCounterName: TextView = itemView.findViewById(R.id.tv_name)
       // val tv_room: TextView = itemView.findViewById(R.id.tv_room)
        val tv_token: TextView = itemView.findViewById(R.id.tv_token)
    }

    fun setCounters(newList: List<CounterListingModel>) {
        mList = newList
        notifyDataSetChanged()
    }

    fun setMarqueeSpeed(tv: TextView?, speed: Float) {
        if (tv != null) {
            try {
                val field = if (tv is AppCompatTextView) {
                    tv.javaClass.superclass.getDeclaredField("mMarquee")
                } else {
                    tv.javaClass.getDeclaredField("mMarquee")
                }
                field?.let {
                    it.isAccessible = true
                    val marquee = it[tv]
                    if (marquee != null) {
                        val scrollSpeedFieldName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            "mPixelsPerSecond"
                        } else {
                            "mScrollUnit"
                        }
                        val speedField = marquee.javaClass.getDeclaredField(scrollSpeedFieldName)
                        speedField.isAccessible = true
                        speedField.setFloat(marquee, speed)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
