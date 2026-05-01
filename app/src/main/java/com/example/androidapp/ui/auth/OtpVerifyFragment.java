package com.example.androidapp.ui.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidapp.R;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.AuthResponse;
import com.example.androidapp.data.model.OtpRequest;
import com.example.androidapp.data.model.OtpVerify;
import com.example.androidapp.data.remote.AuthApi;
import com.example.androidapp.util.ApiErrorParser;
import com.example.androidapp.util.OtpResendCooldown;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class OtpVerifyFragment extends Fragment {

    private static final long DEFAULT_ACCESS_TTL_MS = 60L * 60L * 1000L;
    private static final long DEFAULT_REFRESH_TTL_MS = 7L * 24L * 60L * 60L * 1000L;
    private static final String STATE_KEY_LAST_RESEND = "last_resend_at";

    @Inject AuthApi authApi;
    @Inject TokenManager tokenManager;

    private EditText etCode;
    private Button btnVerify;
    private Button btnResend;
    private ProgressBar progressBar;
    private TextView tvError;
    private String email;

    private final OtpResendCooldown cooldown = new OtpResendCooldown();
    private final Handler cooldownHandler = new Handler(Looper.getMainLooper());
    private Runnable cooldownTick;
    private CharSequence btnResendOriginalText;

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
        btnResendOriginalText = btnResend.getText();

        email = getArguments() != null
                ? getArguments().getString("email", "")
                : "";

        tvSubtitle.setText(getString(R.string.otp_verify_subtitle, email));

        btnVerify.setOnClickListener(v -> verifyOtp(view));
        btnResend.setOnClickListener(v -> resendOtp());

        if (savedInstanceState != null) {
            cooldown.restoreFrom(savedInstanceState.getLong(STATE_KEY_LAST_RESEND, 0L));
        }
        if (cooldown.secondsRemaining(System.currentTimeMillis()) > 0) {
            startCooldownTick();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_KEY_LAST_RESEND, cooldown.getLastResendAtEpochMs());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cooldownTick != null) cooldownHandler.removeCallbacks(cooldownTick);
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

        authApi.verifyOtp(request).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                   Response<ApiResponse<AuthResponse>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnVerify.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    AuthResponse auth = response.body().getData();
                    persistSession(auth);

                    String username = "";
                    if (auth.getUser() != null) {
                        username = auth.getUser().getUsername();
                    }

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

    private void persistSession(AuthResponse data) {
        long now = System.currentTimeMillis();
        String accessToken = data.getToken();
        String refreshToken = data.getRefreshToken() != null ? data.getRefreshToken() : accessToken;
        long accessExpiresAt = parseIsoToEpoch(data.getExpiresAt(), now + DEFAULT_ACCESS_TTL_MS);
        long refreshExpiresAt = parseIsoToEpoch(data.getRefreshExpiresAt(), now + DEFAULT_REFRESH_TTL_MS);
        tokenManager.saveSession(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);
    }

    private void resendOtp() {
        if (!cooldown.canResend(System.currentTimeMillis())) {
            Toast.makeText(getContext(), "Esperá unos segundos para reenviar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(getContext(), "No se puede reenviar sin un email válido", Toast.LENGTH_SHORT).show();
            return;
        }

        tvError.setVisibility(View.GONE);
        btnResend.setEnabled(false);

        OtpRequest request = new OtpRequest(email);

        authApi.resendOtp(request).enqueue(new Callback<ApiResponse<Map<String, String>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, String>>> call,
                                   Response<ApiResponse<Map<String, String>>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    cooldown.markResendNow(System.currentTimeMillis());
                    Toast.makeText(getContext(), "Código reenviado", Toast.LENGTH_SHORT).show();
                    startCooldownTick();
                } else {
                    btnResend.setEnabled(true);
                    Toast.makeText(getContext(), ApiErrorParser.extractMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, String>>> call, Throwable t) {
                if (!isAdded()) return;
                btnResend.setEnabled(true);
                Toast.makeText(getContext(), "Sin conexión, reintentá", Toast.LENGTH_SHORT).show();
                Log.e("OtpVerifyFragment", "OTP resend failed", t);
            }
        });
    }

    private void startCooldownTick() {
        btnResend.setEnabled(false);
        cooldownTick = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                long secs = cooldown.secondsRemaining(System.currentTimeMillis());
                if (secs <= 0) {
                    btnResend.setEnabled(true);
                    btnResend.setText(btnResendOriginalText);
                } else {
                    btnResend.setText("Reenviar en " + secs + "s");
                    cooldownHandler.postDelayed(this, 1000L);
                }
            }
        };
        cooldownHandler.post(cooldownTick);
    }

    private static long parseIsoToEpoch(String iso, long fallback) {
        if (iso == null || iso.trim().isEmpty()) return fallback;
        try {
            return java.time.Instant.parse(iso).toEpochMilli();
        } catch (Exception e) {
            return fallback;
        }
    }
}
