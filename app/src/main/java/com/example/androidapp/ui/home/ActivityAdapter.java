package com.example.androidapp.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.androidapp.R;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class ActivityAdapter extends BaseAdapter {

    private final Context context;
    private final List<Activity> activities;
    private final List<String> favoriteIds = new ArrayList<>();
    private OnFavoriteClickListener favoriteClickListener;

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Activity activity);
    }

    public ActivityAdapter(Context context) {
        this.context = context;
        this.activities = new ArrayList<>();
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteClickListener = listener;
    }

    public void setFavoriteIds(List<String> ids) {
        this.favoriteIds.clear();
        if (ids != null) {
            this.favoriteIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    public void setActivities(List<Activity> newActivities) {
        activities.clear();
        if (newActivities != null) {
            activities.addAll(newActivities);
        }
        notifyDataSetChanged();
    }

    public void append(List<Activity> moreActivities) {
        if (moreActivities != null && !moreActivities.isEmpty()) {
            activities.addAll(moreActivities);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return activities.size();
    }

    @Override
    public Activity getItem(int position) {
        return activities.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_activity_card, parent, false);
            holder = new ViewHolder();
            holder.ivImage = convertView.findViewById(R.id.ivImage);
            holder.tvName = convertView.findViewById(R.id.tvName);
            holder.tvDestination = convertView.findViewById(R.id.tvDestination);
            holder.tvCategory = convertView.findViewById(R.id.tvCategory);
            holder.tvPrice = convertView.findViewById(R.id.tvPrice);
            holder.btnFavorite = convertView.findViewById(R.id.btnFavorite);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Activity activity = activities.get(position);
        holder.tvName.setText(activity.getName());
        holder.tvDestination.setText(activity.getDestination());
        holder.tvCategory.setText(activity.getCategory());

        if (activity.getPrice() == 0) {
            holder.tvPrice.setText(R.string.free_label);
        } else {
            holder.tvPrice.setText(context.getString(R.string.price_format, activity.getPrice()));
        }

        ImageLoader.load(holder.ivImage, activity.getCoverUrl());

        // Favoritos logic
        boolean isFavorite = favoriteIds.contains(activity.getId());
        holder.btnFavorite.setColorFilter(isFavorite ?
                context.getColor(R.color.price_color) : context.getColor(android.R.color.darker_gray));

        holder.btnFavorite.setOnClickListener(v -> {
            if (favoriteClickListener != null) {
                favoriteClickListener.onFavoriteClick(activity);
            }
        });

        return convertView;
    }

    static class ViewHolder {
        ImageView ivImage;
        TextView tvName;
        TextView tvDestination;
        TextView tvCategory;
        TextView tvPrice;
        ImageButton btnFavorite;
    }
}
