package com.example.androidapp.ui.favorites;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.androidapp.R;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.ui.home.ActivityDetailFragment;
import com.example.androidapp.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FavoritosAdapter extends BaseAdapter {

    private final Context context;
    private final List<Activity> favorites;
    private final OnFavoriteActionListener listener;

    public interface OnFavoriteActionListener {
        void onRemoveFavorite(Activity activity);
        void onReserveNow(Activity activity);
        void onItemClick(Activity activity);
    }

    public FavoritosAdapter(Context context, OnFavoriteActionListener listener) {
        this.context = context;
        this.favorites = new ArrayList<>();
        this.listener = listener;
    }

    public void setFavorites(List<Activity> newFavorites) {
        favorites.clear();
        if (newFavorites != null) {
            favorites.addAll(newFavorites);
            // Si el servidor ya reporta que no hay novedad, limpiamos el estado local "visto"
            // para que futuras novedades puedan volver a mostrarse.
            for (Activity activity : newFavorites) {
                if (!activity.getPriceChanged() && !activity.getSpotsChanged()) {
                    ActivityDetailFragment.viewedNovelties.remove(activity.getId());
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return favorites.size();
    }

    @Override
    public Activity getItem(int position) {
        return favorites.get(position);
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
                    .inflate(R.layout.item_favoritos_card, parent, false);
            holder = new ViewHolder();
            holder.ivImage = convertView.findViewById(R.id.ivImage);
            holder.btnFavorite = convertView.findViewById(R.id.btnFavorite);
            holder.tvNoveltyIndicator = convertView.findViewById(R.id.tvNoveltyIndicator);
            holder.tvName = convertView.findViewById(R.id.tvName);
            holder.tvDestination = convertView.findViewById(R.id.tvDestination);
            holder.tvCategory = convertView.findViewById(R.id.tvCategory);
            holder.tvPrice = convertView.findViewById(R.id.tvPrice);
            holder.btnReserveNow = convertView.findViewById(R.id.btnReserveNow);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Activity activity = favorites.get(position);

        holder.tvName.setText(activity.getName());
        holder.tvDestination.setText(activity.getDestination());
        holder.tvCategory.setText(activity.getCategory());

        if (activity.getPrice() == 0) {
            holder.tvPrice.setText(context.getString(R.string.free_label));
        } else {
            holder.tvPrice.setText(context.getString(R.string.price_format, activity.getPrice()));
        }

        ImageLoader.load(holder.ivImage, activity.getCoverUrl());

        // Lógica de Novedades: basada en el servidor, con override local si ya se visitó el detalle
        boolean hasNovelty = (activity.getPriceChanged() || activity.getSpotsChanged())
                && !ActivityDetailFragment.viewedNovelties.contains(activity.getId());

        if (hasNovelty) {
            holder.tvNoveltyIndicator.setVisibility(View.VISIBLE);
            holder.tvNoveltyIndicator.setText(context.getString(R.string.favorites_novelty));
        } else {
            holder.tvNoveltyIndicator.setVisibility(View.GONE);
        }

        // Listeners
        holder.btnReserveNow.setText(context.getString(R.string.favorites_reserve_now));
        holder.btnFavorite.setOnClickListener(v -> {
            if (listener != null) listener.onRemoveFavorite(activity);
        });

        holder.btnReserveNow.setOnClickListener(v -> {
            if (listener != null) listener.onReserveNow(activity);
        });

        convertView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(activity);
        });

        return convertView;
    }

    static class ViewHolder {
        ImageView ivImage;
        ImageButton btnFavorite;
        TextView tvNoveltyIndicator;
        TextView tvName;
        TextView tvDestination;
        TextView tvCategory;
        TextView tvPrice;
        Button btnReserveNow;
    }
}
