package com.medicinoclinic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.medicinoclinic.R;
import com.medicinoclinic.home.HomeActivity1;
import com.medicinoclinic.model.SliderData;
import com.medicinoclinic.model.SliderDataModel;
import com.smarteist.autoimageslider.SliderView;
import com.smarteist.autoimageslider.SliderViewAdapter;

import java.util.ArrayList;
import java.util.List;

public class SliderAdapter extends SliderViewAdapter<SliderAdapter.SliderAdapterViewHolder> {

    // list for storing urls of images.
    private final List<SliderDataModel> mSliderItems;
    final Context context;
    SliderView sliderView;


    // Constructor
    public SliderAdapter(Context context, ArrayList<SliderDataModel> sliderDataArrayList,SliderView sliderView) {
        this.mSliderItems = sliderDataArrayList;
        this.context = context;
        this.sliderView = sliderView;
    }

    // We are inflating the slider_layout
    // inside on Create View Holder method.
    @Override
    public SliderAdapterViewHolder onCreateViewHolder(ViewGroup parent) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, null);
        return new SliderAdapterViewHolder(inflate);
    }


    // Inside on bind view holder we will
    // set data to item of Slider View.
    @Override
    public void onBindViewHolder(SliderAdapterViewHolder viewHolder, final int position) {

        final SliderDataModel sliderItem = mSliderItems.get(position);
        sliderView.setScrollTimeInSec(Integer.parseInt(sliderItem.getDuration()));

        // Glide is use to load image
        // from url in your imageview.
//       ((HomeActivity1)context).runTvScrollingContent(sliderItem.getId());
        Glide.with(viewHolder.itemView)
                .load(sliderItem.getImgUrl())
                .fitCenter()
                .into(viewHolder.imageViewBackground);



    }


    // this method will return
    // the count of our list.
    @Override
    public int getCount() {
        return mSliderItems.size();
    }

    static class SliderAdapterViewHolder extends SliderViewAdapter.ViewHolder {
        // Adapter class for initializing
        // the views of our slider view.
        View itemView;
        ImageView imageViewBackground;

        public SliderAdapterViewHolder(View itemView) {
            super(itemView);
            imageViewBackground = itemView.findViewById(R.id.myimage);
            this.itemView = itemView;
        }
    }



}


