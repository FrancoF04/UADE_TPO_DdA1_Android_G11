package com.example.androidapp.ui.reservation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidapp.R;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.remote.UserApi;

import dagger.hilt.android.AndroidEntryPoint;
import jakarta.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class CancelReservationFragment extends Fragment {
    @Inject
    UserApi userApi;
    private ImageButton btnBack;
    private Button btnCancelar;
    private Button btnConfirmar;
    private TextView tvActivity;
    private TextView tvDate;
    private TextView tvQuantity;

    private String reservationId;
    private String activityId;
    private String reservationDate;
    private int reservationQuantity;
    private String idSchedule;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cancel_reservation, container, false);
    }
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnBack = view.findViewById(R.id.btnBack);
        btnCancelar = view.findViewById(R.id.btnCancelar);
        btnConfirmar = view.findViewById(R.id.btnConfirmar);
        tvActivity = view.findViewById(R.id.tvActivity);
        tvDate = view.findViewById(R.id.tvDate);
        tvQuantity = view.findViewById(R.id.tvQuantity);

        Bundle args = getArguments();
        if(args != null){
            tvActivity.setText(args.getString("activityName"));
            reservationDate = args.getString("date");
            activityId = args.getString("activityId");

            String quantityArg = args.getString("quantity");
            tvDate.setText(reservationDate);
            tvQuantity.setText(quantityArg);

            try {
                reservationQuantity = Integer.parseInt(quantityArg != null ? quantityArg : "0");
            } catch (NumberFormatException ignored) {
                reservationQuantity = 0;
            }

            reservationId = args.getString("reservationId");
            idSchedule = args.getString("idSchedule");
        }else{
            Toast.makeText(requireContext(), "Error al cargar la actividad", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        }

        btnBack.setOnClickListener(v -> Navigation.findNavController(requireView()).navigateUp());
        btnCancelar.setOnClickListener(v -> Navigation.findNavController(requireView()).navigateUp());

        btnConfirmar.setOnClickListener(v -> {
            if (reservationId == null || reservationId.isEmpty()) {
                Toast.makeText(requireContext(), "No se pudo identificar la reserva", Toast.LENGTH_SHORT).show();
                return;
            }

            String rawToken = TokenManager.getInstance(requireContext()).getToken();
            userApi.cancelReservation("Bearer " + rawToken, reservationId).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(requireContext(), "Reserva cancelada", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    } else {
                        cancelWithPostFallback(rawToken);
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    cancelWithPostFallback(rawToken);
                }
            });
        });
    }

    private void cancelWithPostFallback(String rawToken) {
        userApi.cancelReservationPost("Bearer " + rawToken, reservationId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (!isAdded()) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(requireContext(), "Reserva cancelada", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                } else {
                    Toast.makeText(requireContext(), "Error al cancelar la reserva. Intente nuevamente.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }

                Toast.makeText(requireContext(), "Error al cancelar la reserva. Intente nuevamente.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
