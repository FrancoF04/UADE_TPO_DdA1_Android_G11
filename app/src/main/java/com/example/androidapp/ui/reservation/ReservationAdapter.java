package com.example.androidapp.ui.reservation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
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
    private final OnCancelClickListener onCancelClickListener;

    public interface OnCancelClickListener {
        void onCancelClick(Reservation reservation);
    }

    public ReservationAdapter(Context context, List<Reservation> reservations, boolean showPast, OnCancelClickListener listener) {
        this.context = context;
        this.showPast = showPast;
        this.onCancelClickListener = listener;
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
            holder.btnCancelar = convertView.findViewById(R.id.btnCancelar);
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

        // Determinar si se puede cancelar
        boolean canCancel = canCancelReservation(reservation);
        
        if (canCancel) {
            holder.btnCancelar.setVisibility(View.VISIBLE);
            holder.btnCancelar.setOnClickListener(v -> {
                if (onCancelClickListener != null) {
                    onCancelClickListener.onCancelClick(reservation);
                }
            });
        } else {
            holder.btnCancelar.setVisibility(View.GONE);
        }

        return convertView;
    }

    private boolean canCancelReservation(Reservation reservation) {
        try {
            Instant now = Instant.now();
            Instant reservationTime = Instant.parse(reservation.getSelectedDate());
            
            // Calcular las horas entre ahora y la reserva
            long hoursBetween = java.time.temporal.ChronoUnit.HOURS.between(now, reservationTime);
            
            // Se puede cancelar si:
            // 1. La reserva aún no ha pasado
            // 2. Faltan más horas de las requeridas para cancelar
            return hoursBetween > reservation.getCancellationHours();
        } catch (Exception e) {
            return false;
        }
    }

    static class ViewHolder {
        TextView tvActivityName;
        TextView tvSchedule;
        TextView tvStatus;
        TextView tvQuantity;
        Button btnCancelar;
    }
}
