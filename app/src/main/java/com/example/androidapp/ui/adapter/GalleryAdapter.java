package com.example.androidapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.androidapp.R;

import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private static final int[] PLACEHOLDER_COLORS = {
            R.color.placeholder_1, R.color.placeholder_2, R.color.placeholder_3,
            R.color.placeholder_4, R.color.placeholder_5
    };

    private final List<String> imageUrls;

    public GalleryAdapter() {
        this.imageUrls = new ArrayList<>();
    }

    public void setImageUrls(List<String> urls) {
        imageUrls.clear();
        if (urls != null && !urls.isEmpty()) {
            imageUrls.addAll(urls);
        } else {
            imageUrls.add("");
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = imageUrls.get(position);
        if (url != null && !url.isEmpty()) {
            Glide.with(holder.ivGallery.getContext())
                    .load(url)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(PLACEHOLDER_COLORS[position % PLACEHOLDER_COLORS.length])
                    .error(PLACEHOLDER_COLORS[position % PLACEHOLDER_COLORS.length])
                    .centerCrop()
                    .into(holder.ivGallery);
        } else {
            holder.ivGallery.setImageDrawable(null);
            holder.ivGallery.setBackgroundResource(PLACEHOLDER_COLORS[position % PLACEHOLDER_COLORS.length]);
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivGallery;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGallery = itemView.findViewById(R.id.iv_gallery);
        }
    }
}
