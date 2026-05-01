package com.example.androidapp.ui.reservation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.androidapp.R;
import com.example.androidapp.data.model.Reservation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
            holder.ivActivityImage = convertView.findViewById(R.id.ivActivityImage);
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
        Glide.with(convertView)
                .load(reservation.getImageUrl())
                .placeholder(R.drawable.ic_placeholder_activity)
                .error(R.drawable.ic_placeholder_activity)
                .centerCrop()
                .into(holder.ivActivityImage);
        holder.tvActivityName.setText(String.format(reservation.getActivityName()));

        String scheduleText = reservation.getSelectedDate();
        if (scheduleText != null) {
            try {
                Instant inst = parseInstant(scheduleText);
                if (inst == null) {
                    throw new IllegalArgumentException();
                }
                ZonedDateTime zdt = ZonedDateTime.ofInstant(inst, ZoneId.systemDefault());
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                scheduleText = fmt.format(zdt);
            } catch (Exception ignored) {
            }
        }

        holder.tvSchedule.setText(String.format("Horario: %s", scheduleText));
        holder.tvStatus.setText(String.format("Estado: %s", reservation.getStatus()));
        holder.tvQuantity.setText(String.format("Cantidad: %d", reservation.getQuantity()));

        // Mostrar botón cancelar solo para reservas activas (no canceladas y fecha futura)
        boolean isActive = !isCancelled(reservation.getStatus()) && !isPast(reservation.getSelectedDate());
        if (isActive) {
            holder.btnCancelar.setVisibility(View.VISIBLE);
            holder.btnCancelar.setOnClickListener(v -> {
                if (canCancelReservation(reservation)) {
                    if (onCancelClickListener != null) {
                        onCancelClickListener.onCancelClick(reservation);
                    }
                } else {
                    Toast.makeText(context, "Esta reserva ya no se puede cancelar", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            holder.btnCancelar.setVisibility(View.GONE);
            holder.btnCancelar.setOnClickListener(null);
        }

        return convertView;
    }

    private boolean isCancelled(String status) {
        if (status == null) return false;
        return status.trim().toLowerCase().contains("cancel");
    }

    private boolean isPast(String selectedDate) {
        try {
            Instant instant = parseInstant(selectedDate);
            return instant != null && instant.isBefore(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canCancelReservation(Reservation reservation) {
        try {
            Instant now = Instant.now();
            Instant reservationTime = parseInstant(reservation.getSelectedDate());
            if (reservationTime == null) {
                return false;
            }
            
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

    private Instant parseInstant(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        try {
            return Instant.parse(raw);
        } catch (RuntimeException ignored) {
        }

        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (RuntimeException ignored) {
        }

        try {
            return LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static class ViewHolder {
        ImageView ivActivityImage;
        TextView tvActivityName;
        TextView tvSchedule;
        TextView tvStatus;
        TextView tvQuantity;
        Button btnCancelar;
    }
}
