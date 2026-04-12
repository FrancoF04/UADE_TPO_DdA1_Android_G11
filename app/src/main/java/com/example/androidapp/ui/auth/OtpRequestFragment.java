package com.example.androidapp.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidapp.R;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.OtpRequest;
import com.example.androidapp.data.remote.AuthApi;
import com.example.androidapp.data.remote.RetrofitClient;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpRequestFragment extends Fragment {

    private EditText etEmail;
    private Button btnSendOtp;
    private ProgressBar progressBar;
    private TextView tvError;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etEmail = view.findViewById(R.id.etEmail);
        btnSendOtp = view.findViewById(R.id.btnSendOtp);
        progressBar = view.findViewById(R.id.progressBar);
        tvError = view.findViewById(R.id.tvError);

        TextView tvLoginLink = view.findViewById(R.id.tvLoginLink);

        btnSendOtp.setOnClickListener(v -> sendOtp(view));

        // Volver al login
        tvLoginLink.setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
    }

    private void sendOtp(View view) {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            tvError.setText(R.string.error_field_required);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        tvError.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);

        OtpRequest request = new OtpRequest(email);
        AuthApi authApi = RetrofitClient.getInstance().create(AuthApi.class);

        authApi.requestOtp(request).enqueue(new Callback<ApiResponse<Map<String, String>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, String>>> call,
                                   Response<ApiResponse<Map<String, String>>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    // Navegar al fragment de verificacion, pasando el email via Bundle
                    Bundle args = new Bundle();
                    args.putString("email", email);
                    Navigation.findNavController(view)
                            .navigate(R.id.action_otp_request_to_verify, args);
                } else {
                    tvError.setText(R.string.error_generic);
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, String>>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnSendOtp.setEnabled(true);
                tvError.setText(R.string.error_network);
                tvError.setVisibility(View.VISIBLE);
                Log.e("OtpRequestFragment", "OTP request failed", t);
            }
        });
    }
}
