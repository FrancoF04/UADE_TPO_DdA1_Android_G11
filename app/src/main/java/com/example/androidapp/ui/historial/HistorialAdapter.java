package com.example.androidapp.ui.historial;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.androidapp.R;
import com.example.androidapp.data.model.HistorialItem;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistorialAdapter extends BaseAdapter {

    public interface OnRatingClickListener {
        void onRatingClick(HistorialItem item);
    }

    private final Context context;
    private final List<HistorialItem> items;
    private OnRatingClickListener ratingClickListener;

    public HistorialAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }

    public void setOnRatingClickListener(OnRatingClickListener listener) {
        this.ratingClickListener = listener;
    }

    public void setItems(List<HistorialItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public HistorialItem getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_historial_card, parent, false);
            holder = new ViewHolder();
            holder.ivImage = convertView.findViewById(R.id.ivImage);
            holder.tvName = convertView.findViewById(R.id.tvName);
            holder.tvDestination = convertView.findViewById(R.id.tvDestination);
            holder.tvCategory = convertView.findViewById(R.id.tvCategory);
            holder.tvPrice = convertView.findViewById(R.id.tvPrice);
            holder.btnCalificar = convertView.findViewById(R.id.btnCalificar);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HistorialItem item = getItem(position);
        Glide.with(convertView)
                .load(item.getImageUrl())
                .placeholder(R.drawable.ic_placeholder_activity)
                .error(R.drawable.ic_placeholder_activity)
                .centerCrop()
                .into(holder.ivImage);
        holder.tvName.setText(item.getActivityName());
        holder.tvDestination.setText(item.getDestination());
        holder.tvCategory.setText(formatDate(item.getSelectedDate()));
        holder.tvPrice.setText(item.getDuration() != null && !item.getDuration().isEmpty()
                ? context.getString(R.string.historial_duracion, item.getDuration())
                : "");

        holder.btnCalificar.setOnClickListener(v -> {
            if (ratingClickListener != null) {
                ratingClickListener.onRatingClick(item);
            }
        });

        return convertView;
    }

    private String formatDate(String isoDate) {
        if (isoDate == null) return "";
        try {
            Instant inst = Instant.parse(isoDate);
            ZonedDateTime zdt = ZonedDateTime.ofInstant(inst, ZoneId.systemDefault());
            return zdt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return isoDate;
        }
    }

    static class ViewHolder {
        ImageView ivImage;
        TextView tvName;
        TextView tvDestination;
        TextView tvCategory;
        TextView tvPrice;
        Button btnCalificar;
    }
}
