package com.medicinoclinic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.DoctorModel
import com.medicinoclinic.R


class DoctorInfiniteAutoScrollAdapter(private val evenLayoutResId: Int) :
    RecyclerView.Adapter<DoctorInfiniteAutoScrollAdapter.InfiniteAutoScrollViewHolder>() {

    private var contents = arrayListOf<DoctorModel>()

    fun notifyData(data: List<DoctorModel>) {
        this.contents.clear()
        this.contents.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): InfiniteAutoScrollViewHolder {
        return InfiniteAutoScrollViewHolder(
            LayoutInflater.from(viewGroup.context)
                .inflate(evenLayoutResId, viewGroup, false)
        )
    }

    override fun onBindViewHolder(holder: InfiniteAutoScrollViewHolder, position: Int) {
        holder.tv_name.setText(contents[position % contents.size].name)
        holder.tv_room.setText(contents[position % contents.size].room)
        holder.tv_token.setText(contents[position % contents.size].token)
    }

    override fun getItemCount(): Int {
        return Int.MAX_VALUE
    }

    class InfiniteAutoScrollViewHolder(@NonNull view: View) :
        RecyclerView.ViewHolder(view) {
        val tv_name: TextView = view.findViewById(R.id.tv_name)
        val tv_room: TextView = view.findViewById(R.id.tv_room)
        val tv_token: TextView = view.findViewById(R.id.tv_token)
    }
}