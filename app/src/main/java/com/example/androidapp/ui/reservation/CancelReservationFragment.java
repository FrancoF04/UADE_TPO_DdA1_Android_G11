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

    private String bookingId;

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

        if(getArguments()!=null){
            tvActivity.setText(getArguments().getString("activityName"));
            tvDate.setText(getArguments().getString("date"));
            tvQuantity.setText(getArguments().getString("quantity"));
            bookingId = getArguments().getString("bookingId");
        }else{
            requireActivity().onBackPressed();
            Toast.makeText(requireContext(), "Error al cargar la actividad", Toast.LENGTH_SHORT).show();
        }

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        btnCancelar.setOnClickListener(v -> requireActivity().onBackPressed());

        btnConfirmar.setOnClickListener(v -> {

            userApi.cancelReservation(bookingId).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(requireContext(), "Reserva cancelada", Toast.LENGTH_SHORT).show();
                        requireActivity().onBackPressed();
                    } else {
                        Toast.makeText(requireContext(), "Error al cancelar la reserva. Intente nuevamente.", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    Toast.makeText(requireContext(), "Error al cancelar la reserva. Intente nuevamente.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
