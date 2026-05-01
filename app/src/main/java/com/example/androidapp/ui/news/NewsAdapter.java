package com.example.androidapp.ui.news;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.androidapp.R;
import com.example.androidapp.data.model.News;
import com.example.androidapp.util.ImageLoader;

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
        holder.tvChip.setText(n.categoryLabel());
        holder.tvChip.setBackgroundResource(chipBackgroundFor(n.getCategory()));

        ImageLoader.load(holder.ivThumb, n.getImage());

        return convertView;
    }

    private static int chipBackgroundFor(String category) {
        if (category == null) return R.drawable.chip_noticia;
        switch (category) {
            case "descuento": return R.drawable.chip_offer;
            case "promocion": return R.drawable.chip_promocion;
            case "nuevo_destino": return R.drawable.chip_destacado;
            case "noticia":
            default: return R.drawable.chip_noticia;
        }
    }

    private static class ViewHolder {
        ImageView ivThumb;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvChip;
    }
}
