package com.medicinoclinic.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.medicinoclinic.R
import com.medicinoclinic.model.SliderDataModel
import java.util.Objects

class ViewPagerAdapter (val context: Context, val imageList: List<SliderDataModel>) : PagerAdapter() {
    // on below line we are creating a method
    // as get count to return the size of the list.
    override fun getCount(): Int {
        return imageList.size
    }

    // on below line we are returning the object
    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as RelativeLayout
    }

    // on below line we are initializing
    // our item and inflating our layout file
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        // on below line we are initializing
        // our layout inflater.
        val mLayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // on below line we are inflating our custom
        // layout file which we have created.
        val itemView: View = mLayoutInflater.inflate(R.layout.item_image, container, false)

        // on below line we are initializing
        // our image view with the id.
        val sliderItem: SliderDataModel = imageList.get(position)
        val imageView: ImageView = itemView.findViewById<View>(R.id.myimage) as ImageView

        // on below line we are setting
        // image resource for image view.
        // Glide is use to load image
        // from url in your imageview.
//       ((HomeActivity1)context).runTvScrollingContent(sliderItem.getId());
        Glide.with(context)
            .load(sliderItem.imgUrl)
            .fitCenter()
            .into(imageView)


        // on the below line we are adding this
        // item view to the container.
        Objects.requireNonNull(container).addView(itemView)

        // on below line we are simply
        // returning our item view.
        return itemView
    }

    // on below line we are creating a destroy item method.
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        // on below line we are removing view
        container.removeView(`object` as RelativeLayout)
    }
}