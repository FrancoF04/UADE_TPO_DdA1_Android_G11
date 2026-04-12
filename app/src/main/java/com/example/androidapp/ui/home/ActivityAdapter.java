package com.example.androidapp.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.androidapp.R;
import com.example.androidapp.data.model.Activity;

import java.util.ArrayList;
import java.util.List;

public class ActivityAdapter extends BaseAdapter {

    private final Context context;
    private final List<Activity> activities;

    public ActivityAdapter(Context context) {
        this.context = context;
        this.activities = new ArrayList<>();
    }

    public void setActivities(List<Activity> newActivities) {
        activities.clear();
        if (newActivities != null) {
            activities.addAll(newActivities);
        }
        notifyDataSetChanged();
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

        // Placeholder — no image loading library available (limited knowledge).
        holder.ivImage.setImageDrawable(null);

        return convertView;
    }

    static class ViewHolder {
        ImageView ivImage;
        TextView tvName;
        TextView tvDestination;
        TextView tvCategory;
        TextView tvPrice;
    }
}
