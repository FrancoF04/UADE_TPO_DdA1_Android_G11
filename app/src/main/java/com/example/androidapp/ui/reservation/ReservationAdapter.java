package com.example.androidapp.ui.reservation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.androidapp.R;
import com.example.androidapp.data.model.Reservation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReservationAdapter extends BaseAdapter {
    private final Context context;
    private final List<Reservation> reservations;
    private final boolean showPast;

    public ReservationAdapter(Context context, List<Reservation> reservations, boolean showPast) {
        this.context = context;
        this.showPast = showPast;
        this.reservations = new ArrayList<>();
        if (reservations != null) {
            this.reservations.addAll(reservations);
        }
    }

    public void setReservations(List<Reservation> newReservations) {
        reservations.clear();
        if (newReservations != null) {
            reservations.addAll(newReservations);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return reservations.size();
    }

    @Override
    public Reservation getItem(int position) {
        return reservations.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_reservation, parent, false);
            holder = new ViewHolder();
            holder.tvActivityName = convertView.findViewById(R.id.tvActivityName);
            holder.tvSchedule = convertView.findViewById(R.id.tvSchedule);
            holder.tvQuantity = convertView.findViewById(R.id.tvQuantity);
            holder.tvStatus = convertView.findViewById(R.id.tvStatus);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Reservation reservation = getItem(position);
        holder.tvActivityName.setText(String.format(reservation.getActivityName()));

        String scheduleText = reservation.getSelectedDate();
        if (scheduleText != null) {
            try {
                Instant inst = Instant.parse(scheduleText);
                ZonedDateTime zdt = ZonedDateTime.ofInstant(inst, ZoneId.systemDefault());
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                scheduleText = fmt.format(zdt);
            } catch (Exception ignored) {
            }
        }

        holder.tvSchedule.setText(String.format("Horario: %s", scheduleText));
        holder.tvStatus.setText(String.format("Estado: %s", reservation.getStatus()));
        holder.tvQuantity.setText(String.format("Cantidad: %d", reservation.getQuantity()));

        return convertView;
    }

    static class ViewHolder {
        TextView tvActivityName;
        TextView tvSchedule;
        TextView tvStatus;
        TextView tvQuantity;
    }
}
