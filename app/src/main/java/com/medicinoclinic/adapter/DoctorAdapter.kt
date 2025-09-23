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
import com.medicinoclinic.model.DoctorListingModel
import java.lang.reflect.Field

class DoctorAdapter(private var mList: List<DoctorListingModel>) : RecyclerView.Adapter<DoctorAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_room, parent, false)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doctor = mList[position]
        setMarqueeSpeed(holder.tvDoctorName, 20f)

        holder.tvDoctorName.isSelected = doctor.name.length > 10
        holder.tvDoctorName.text = "Dr. ${doctor.name}"
        holder.tv_room.text = if (doctor.room == "null") "NA" else doctor.room
        holder.tv_token.text = doctor.token.trim().ifEmpty { "0" }

        holder.itemView.setOnFocusChangeListener { _, _ -> }
        holder.itemView.setOnClickListener { view ->
            if (view.isFocusable) {
                view.context.startActivity(Intent(view.context, SettingsActivity::class.java))
            }
        }
    }

    override fun getItemCount(): Int = mList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDoctorName: TextView = itemView.findViewById(R.id.tv_name)
        val tv_room: TextView = itemView.findViewById(R.id.tv_room)
        val tv_token: TextView = itemView.findViewById(R.id.tv_token)
    }

    fun setDoctors(newList: List<DoctorListingModel>) {
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
