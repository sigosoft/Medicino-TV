package com.medicinoclinic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.ContentModel
import com.medicinoclinic.R


class InfiniteAutoScrollAdapter(private val evenLayoutResId: Int) :
    RecyclerView.Adapter<InfiniteAutoScrollAdapter.InfiniteAutoScrollViewHolder>() {

    private var contents =  arrayListOf<ContentModel>()

    fun notifyData(data: List<ContentModel>) {
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
        holder.scrollingtitle.setText(contents[position % contents.size].title)
        holder.scrollingtext.setText(contents[position % contents.size].body)
    }

    override fun getItemCount(): Int {
        return Int.MAX_VALUE
    }

    class InfiniteAutoScrollViewHolder(@NonNull view: View) :
        RecyclerView.ViewHolder(view) {
        val scrollingtext: TextView = view.findViewById(R.id.scrollingtext)
        val scrollingtitle: TextView = view.findViewById(R.id.scrollingtitle)
    }
}