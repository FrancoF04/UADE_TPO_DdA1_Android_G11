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
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidapp.R;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.AuthResponse;
import com.example.androidapp.data.model.LoginRequest;
import com.example.androidapp.data.remote.AuthApi;
import com.example.androidapp.util.BiometricHelper;
import com.example.androidapp.util.BiometricStatus;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    private static final long DEFAULT_ACCESS_TTL_MS = 60L * 60L * 1000L;
    private static final long DEFAULT_REFRESH_TTL_MS = 7L * 24L * 60L * 60L * 1000L;

    @Inject TokenManager tokenManager;
    @Inject AuthApi authApi;
    @Inject BiometricHelper biometricHelper;

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvError;
    private TextView tvSessionExpired;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        progressBar = view.findViewById(R.id.progressBar);
        tvError = view.findViewById(R.id.tvError);
        tvSessionExpired = view.findViewById(R.id.tvSessionExpired);

        TextView tvOtpLink = view.findViewById(R.id.tvOtpLink);
        TextView tvRegisterLink = view.findViewById(R.id.tvRegisterLink);

        btnLogin.setOnClickListener(v -> attemptLogin(view));

        tvOtpLink.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_otp_request));

        tvRegisterLink.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_register));

        boolean autoPromptBiometric = getArguments() != null && getArguments().getBoolean("autoPromptBiometric", false);
        boolean forceUserPass = getArguments() != null && getArguments().getBoolean("forceUserPass", false);

        if (forceUserPass && tvSessionExpired != null) {
            tvSessionExpired.setVisibility(View.VISIBLE);
        }

        if (autoPromptBiometric && biometricHelper.checkAvailability() == BiometricStatus.AVAILABLE) {
            biometricHelper.promptForAuth(
                    requireActivity(),
                    "Ingresar a XploreNow",
                    "Autenticate con tu huella",
                    this::onBiometricSuccess,
                    (code, msg) -> { /* dejar pantalla normal visible */ }
            );
        }
    }

    private void attemptLogin(View view) {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            tvError.setText(R.string.error_field_required);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        tvError.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        LoginRequest request = new LoginRequest(username, password);

        authApi.login(request).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                   Response<ApiResponse<AuthResponse>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    AuthResponse data = response.body().getData();
                    persistSession(data);
                    maybeShowOptInDialog();
                    navigateToHome(view, username);
                } else {
                    tvError.setText(R.string.error_generic);
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                tvError.setText(R.string.error_network);
                tvError.setVisibility(View.VISIBLE);
                Log.e("LoginFragment", "Login failed", t);
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

    private void maybeShowOptInDialog() {
        if (biometricHelper.checkAvailability() == BiometricStatus.AVAILABLE
                && !tokenManager.isBiometricEnabled()
                && !tokenManager.isBiometricOptInDismissed()) {
            BiometricOptInDialog.show(requireContext(), tokenManager, () -> {});
        }
    }

    private void onBiometricSuccess() {
        String refresh = tokenManager.getRefreshToken();
        if (refresh == null) return;

        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refresh);

        authApi.refresh(body).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                   Response<ApiResponse<AuthResponse>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    persistSession(response.body().getData());
                    NavHostFragment.findNavController(LoginFragment.this)
                            .navigate(R.id.action_login_to_home);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                // dejar pantalla normal visible
            }
        });
    }

    private static long parseIsoToEpoch(String iso, long fallback) {
        if (iso == null || iso.trim().isEmpty()) return fallback;
        try {
            return java.time.Instant.parse(iso).toEpochMilli();
        } catch (Exception e) {
            return fallback;
        }
    }

    private void navigateToHome(View view, String username) {
        Bundle args = new Bundle();
        args.putString("username", username);

        Navigation.findNavController(view)
                .navigate(R.id.action_login_to_home, args);
    }
}
