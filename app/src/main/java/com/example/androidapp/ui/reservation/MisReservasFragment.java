package com.example.androidapp.ui.reservation;

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
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.Reservation;
import com.example.androidapp.data.remote.UserApi;

import java.util.ArrayList;
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
    private TextView tvEmpty;
    private List<Reservation> reservas;
    private ReservationAdapter adapter;
    private ListView lvActividades;
    private Button btnActividadesPasadas;

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
        userApi.getReservations()
                .enqueue(new Callback<ApiResponse<List<Reservation>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Reservation>>> call, Response<ApiResponse<List<Reservation>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            List<Reservation> data = response.body().getData();
                            reservas = data != null
                                    ? data.stream()
                                            .filter(r -> "confirmed".equals(r.getStatus()))
                                            .collect(Collectors.toList())
                                    : new ArrayList<>();
                        } else {
                            reservas = new ArrayList<>();
                        }
                        adapter.setReservations(reservas);
                        tvEmpty.setVisibility(reservas.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Reservation>>> call, Throwable throwable) {
                        reservas = new ArrayList<>();
                        adapter.setReservations(reservas);
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
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
        tvEmpty = view.findViewById(R.id.tvEmpty);
        lvActividades = view.findViewById(R.id.lvActividades);
        btnActividadesPasadas = view.findViewById(R.id.btnActividadesPasadas);

        adapter = new ReservationAdapter(requireContext(), new ArrayList<>(), false, this::onCancelReservation);
        lvActividades.setAdapter(adapter);

        lvActividades.setOnItemClickListener((parent, v, position, id) -> {
            Reservation reservation = adapter.getItem(position);
            if (reservation == null || reservation.getActivityId() == null) return;
            Bundle args = new Bundle();
            args.putString("activityId", reservation.getActivityId());
            args.putBoolean("showReserveButton", false);
            Navigation.findNavController(requireView()).navigate(R.id.action_reservas_to_detail, args);
        });

        tvActividades.setText(R.string.mis_actividades);

        btnActividadesPasadas.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigate(R.id.action_reservas_to_historial));
    }

}
