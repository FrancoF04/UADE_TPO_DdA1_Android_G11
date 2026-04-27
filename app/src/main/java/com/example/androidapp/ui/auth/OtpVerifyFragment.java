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
import com.example.androidapp.data.model.AuthResponse;
import com.example.androidapp.data.model.OtpRequest;
import com.example.androidapp.data.model.OtpVerify;
import com.example.androidapp.data.remote.AuthApi;
import com.example.androidapp.data.remote.RetrofitClient;

import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
public class OtpVerifyFragment extends Fragment {

    private EditText etCode;
    private Button btnVerify;
    private Button btnResend;
    private ProgressBar progressBar;
    private TextView tvError;
    private String email;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp_verify, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etCode = view.findViewById(R.id.etCode);
        btnVerify = view.findViewById(R.id.btnVerify);
        btnResend = view.findViewById(R.id.btnResend);
        progressBar = view.findViewById(R.id.progressBar);
        tvError = view.findViewById(R.id.tvError);
        TextView tvSubtitle = view.findViewById(R.id.tvSubtitle);

        // Leer el email que viene del OtpRequestFragment via Bundle
        email = getArguments() != null
                ? getArguments().getString("email", "")
                : "";

        tvSubtitle.setText(getString(R.string.otp_verify_subtitle, email));

        btnVerify.setOnClickListener(v -> verifyOtp(view));
        btnResend.setOnClickListener(v -> resendOtp());
    }

    private void verifyOtp(View view) {
        String code = etCode.getText().toString().trim();

        if (code.isEmpty() || code.length() != 6) {
            tvError.setText(R.string.error_otp_invalid);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        tvError.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        btnVerify.setEnabled(false);

        OtpVerify request = new OtpVerify(email, code);
        AuthApi authApi = RetrofitClient.getInstance().create(AuthApi.class);

        authApi.verifyOtp(request).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                   Response<ApiResponse<AuthResponse>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnVerify.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    // Obtener username del AuthResponse
                    AuthResponse auth = response.body().getData();
                    String username = "";
                    if (auth != null && auth.getUser() != null) {
                        username = auth.getUser().getUsername();
                    }

                    // Navegar al home pasando el username via Bundle
                    Bundle args = new Bundle();
                    args.putString("username", username);
                    Navigation.findNavController(view)
                            .navigate(R.id.action_otp_verify_to_home, args);
                } else {
                    tvError.setText(R.string.error_generic);
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnVerify.setEnabled(true);
                tvError.setText(R.string.error_network);
                tvError.setVisibility(View.VISIBLE);
                Log.e("OtpVerifyFragment", "OTP verify failed", t);
            }
        });
    }

    private void resendOtp() {
        tvError.setVisibility(View.GONE);
        btnResend.setEnabled(false);

        OtpRequest request = new OtpRequest(email);
        AuthApi authApi = RetrofitClient.getInstance().create(AuthApi.class);

        authApi.resendOtp(request).enqueue(new Callback<ApiResponse<Map<String, String>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, String>>> call,
                                   Response<ApiResponse<Map<String, String>>> response) {
                if (!isAdded()) return;
                btnResend.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    tvError.setText(R.string.otp_resent_success);
                    tvError.setVisibility(View.VISIBLE);
                } else {
                    tvError.setText(R.string.error_generic);
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, String>>> call, Throwable t) {
                if (!isAdded()) return;
                btnResend.setEnabled(true);
                tvError.setText(R.string.error_network);
                tvError.setVisibility(View.VISIBLE);
                Log.e("OtpVerifyFragment", "OTP resend failed", t);
            }
        });
    }
}
