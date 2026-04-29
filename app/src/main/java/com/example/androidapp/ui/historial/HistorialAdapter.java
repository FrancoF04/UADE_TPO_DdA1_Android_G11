package com.example.androidapp.ui.historial;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.androidapp.R;
import com.example.androidapp.data.model.HistorialItem;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistorialAdapter extends BaseAdapter {

    private final Context context;
    private final List<HistorialItem> items;

    public HistorialAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_historial, parent, false);
            holder = new ViewHolder();
            holder.tvNombre = convertView.findViewById(R.id.tvHistorialNombre);
            holder.tvFecha = convertView.findViewById(R.id.tvHistorialFecha);
            holder.tvDestino = convertView.findViewById(R.id.tvHistorialDestino);
            holder.tvGuia = convertView.findViewById(R.id.tvHistorialGuia);
            holder.tvDuracion = convertView.findViewById(R.id.tvHistorialDuracion);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HistorialItem item = getItem(position);
        holder.tvNombre.setText(item.getActivityName());
        holder.tvFecha.setText(formatDate(item.getSelectedDate()));
        holder.tvDestino.setText(context.getString(R.string.historial_destino, item.getDestination()));
        holder.tvGuia.setText(context.getString(R.string.historial_guia, item.getGuideName()));
        holder.tvDuracion.setText(context.getString(R.string.historial_duracion, item.getDuration()));

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
        TextView tvNombre;
        TextView tvFecha;
        TextView tvDestino;
        TextView tvGuia;
        TextView tvDuracion;
    }
}
