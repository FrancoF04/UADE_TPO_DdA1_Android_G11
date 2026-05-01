package com.example.androidapp.ui.news;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.androidapp.R;
import com.example.androidapp.data.model.News;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends BaseAdapter {

    private final Context context;
    private final List<News> items = new ArrayList<>();

    public NewsAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<News> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public News getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_news, parent, false);
            holder = new ViewHolder();
            holder.ivThumb = convertView.findViewById(R.id.ivThumb);
            holder.tvTitle = convertView.findViewById(R.id.tvTitle);
            holder.tvDescription = convertView.findViewById(R.id.tvDescription);
            holder.tvChip = convertView.findViewById(R.id.tvChip);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        News n = items.get(position);
        holder.tvTitle.setText(n.getTitle());
        holder.tvDescription.setText(n.getDescription());

        if (n.hasRelatedActivity()) {
            holder.tvChip.setText("Oferta");
            holder.tvChip.setBackgroundResource(R.drawable.chip_offer);
        } else {
            holder.tvChip.setText("Destacado");
            holder.tvChip.setBackgroundResource(R.drawable.chip_destacado);
        }

        if (n.getImage() != null && !n.getImage().isEmpty()) {
            Glide.with(context).load(n.getImage()).into(holder.ivThumb);
        } else {
            holder.ivThumb.setImageDrawable(null);
        }

        return convertView;
    }

    private static class ViewHolder {
        ImageView ivThumb;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvChip;
    }
}
