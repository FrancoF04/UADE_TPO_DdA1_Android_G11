package com.example.androidapp.ui.reservation;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidapp.R;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.Reservation;
import com.example.androidapp.data.remote.UserApi;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;
import jakarta.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class MisReservasFragment extends Fragment {
    @Inject
    UserApi userApi;
    private TextView tvActividades;
    private List<Reservation> reservas;
    private ReservationAdapter adapter;
    private ListView lvActividades;
    private Button btnActividadesProximas;
    private Button btnActividadesPasadas;
    private boolean past;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mis_reservas, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        cargarReservas();
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarReservas();
    }

    private void cargarReservas() {
        userApi.getReservations("Bearer " + TokenManager.getInstance(requireContext()).getToken())
                .enqueue(new Callback<ApiResponse<List<Reservation>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Reservation>>> call, Response<ApiResponse<List<Reservation>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            List<Reservation> data = response.body().getData();
                            reservas = data != null ? data : new ArrayList<>();
                        } else {
                            reservas = new ArrayList<>();
                        }
                        adapter.setReservations(filterReservations(past));
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Reservation>>> call, Throwable throwable) {
                        reservas = new ArrayList<>();
                        adapter.setReservations(filterReservations(past));
                    }
                });
    }

    private void mostrarReservasProximas() {
        past = false;
        tvActividades.setText("Mis actividades proximas");
        adapter.setReservations(filterReservations(past));
    }

    private List<Reservation> filterReservations(boolean past) {
        if (reservas == null) return new ArrayList<>();
        Instant now = Instant.now();
        return reservas.stream().filter(r -> {
            if (r == null || isCancelled(r.getStatus())) {
                return false;
            }

            Instant selectedDate = parseInstant(r.getSelectedDate());
            if (selectedDate == null) {
                return false;
            }

            return past ? selectedDate.isBefore(now) : !selectedDate.isBefore(now);
        }).sorted(Comparator.comparing(r -> parseInstant(r.getSelectedDate())))
                .collect(Collectors.toList());
    }

    private void onCancelReservation(Reservation reservation) {
        if (reservation == null || reservation.getId() == null || reservation.getId().isEmpty()) {
            Toast.makeText(requireContext(), "No se pudo identificar la reserva a cancelar", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args = new Bundle();
        args.putString("activityName", reservation.getActivityName());
        args.putString("activityId", reservation.getActivityId());
        args.putString("date", reservation.getSelectedDate());
        args.putString("quantity", String.valueOf(reservation.getQuantity()));
        args.putString("reservationId", reservation.getId());
        args.putString("idSchedule", String.valueOf(reservation.getSelectedScheduleId()));
        
        Navigation.findNavController(requireView())
                .navigate(R.id.action_CancelarReservaFragment, args);
    }

    private void initViews(View view){
        tvActividades = view.findViewById(R.id.tvActividades);
        lvActividades = view.findViewById(R.id.lvActividades);
        btnActividadesProximas = view.findViewById(R.id.btnActividadesProximas);
        btnActividadesPasadas = view.findViewById(R.id.btnActividadesPasadas);

        adapter = new ReservationAdapter(requireContext(), new ArrayList<>(), false, this::onCancelReservation);
        lvActividades.setAdapter(adapter);

        btnActividadesProximas.setOnClickListener(v -> mostrarReservasProximas());

        btnActividadesPasadas.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.action_reservas_to_historial));
        
        mostrarReservasProximas();
    }

    private boolean isCancelled(String status) {
        if (status == null) {
            return false;
        }

        String normalized = status.trim().toLowerCase();
        return normalized.contains("cancel");
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
}
