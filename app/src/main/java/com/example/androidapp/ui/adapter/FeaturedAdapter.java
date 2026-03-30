package com.example.androidapp.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.androidapp.R;
import com.example.androidapp.data.model.Activity;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public class FeaturedAdapter extends RecyclerView.Adapter<FeaturedAdapter.ViewHolder> {

    private static final int[] PLACEHOLDER_COLORS = {
            R.color.placeholder_1, R.color.placeholder_2, R.color.placeholder_3,
            R.color.placeholder_4, R.color.placeholder_5
    };

    private final List<Activity> activities;
    private final ActivityAdapter.OnActivityClickListener listener;

    public FeaturedAdapter(ActivityAdapter.OnActivityClickListener listener) {
        this.activities = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_featured, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Activity activity = activities.get(position);
        Context context = holder.itemView.getContext();

        holder.tvName.setText(activity.getName());
        holder.tvDestination.setText(activity.getDestination());

        if (activity.getPrice() == 0) {
            holder.tvPrice.setText(R.string.free_label);
        } else {
            holder.tvPrice.setText(context.getString(R.string.price_format, activity.getPrice()));
        }

        String imageUrl = activity.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(PLACEHOLDER_COLORS[position % PLACEHOLDER_COLORS.length])
                    .error(PLACEHOLDER_COLORS[position % PLACEHOLDER_COLORS.length])
                    .centerCrop()
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageDrawable(null);
            holder.ivImage.setBackgroundResource(PLACEHOLDER_COLORS[position % PLACEHOLDER_COLORS.length]);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActivityClick(activity);
            }
        });
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public void setActivities(List<Activity> newActivities) {
        activities.clear();
        activities.addAll(newActivities);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivImage;
        final MaterialTextView tvName;
        final MaterialTextView tvDestination;
        final MaterialTextView tvPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
            tvName = itemView.findViewById(R.id.tv_name);
            tvDestination = itemView.findViewById(R.id.tv_destination);
            tvPrice = itemView.findViewById(R.id.tv_price);
        }
    }
}
