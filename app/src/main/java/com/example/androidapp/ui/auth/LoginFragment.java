package com.example.androidapp.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.androidapp.R;
import com.example.androidapp.data.local.SessionManager;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.AuthResponse;
import com.example.androidapp.data.model.LoginRequest;
import com.example.androidapp.data.remote.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private TextInputLayout tilUsername;
    private TextInputLayout tilPassword;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilUsername = view.findViewById(R.id.til_username);
        tilPassword = view.findViewById(R.id.til_password);
        etUsername = view.findViewById(R.id.et_username);
        etPassword = view.findViewById(R.id.et_password);
        btnLogin = view.findViewById(R.id.btn_login);
        progressBar = view.findViewById(R.id.progress_bar);
        MaterialTextView tvOtpLink = view.findViewById(R.id.tv_otp_link);
        MaterialTextView tvRegisterLink = view.findViewById(R.id.tv_register_link);

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvOtpLink.setOnClickListener(v -> {
            if (getActivity() instanceof LoginActivity) {
                ((LoginActivity) getActivity()).showOtpRequest();
            }
        });

        tvRegisterLink.setOnClickListener(v -> {
            if (getActivity() instanceof LoginActivity) {
                ((LoginActivity) getActivity()).showRegister();
            }
        });
    }

    private void attemptLogin() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        tilUsername.setError(null);
        tilPassword.setError(null);

        boolean valid = true;
        if (username.isEmpty()) {
            tilUsername.setError(getString(R.string.error_field_required));
            valid = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_field_required));
            valid = false;
        }
        if (!valid) return;

        setLoading(true);

        LoginRequest request = new LoginRequest(username, password);
        RetrofitClient.getInstance().getAuthApi().login(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AuthResponse>> call,
                                   @NonNull Response<ApiResponse<AuthResponse>> response) {
                if (!isAdded()) return;
                setLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthResponse authResponse = response.body().getData();
                    SessionManager sessionManager = new SessionManager(requireContext());
                    sessionManager.saveSession(authResponse.getToken(), authResponse.getUser());

                    if (getActivity() instanceof LoginActivity) {
                        ((LoginActivity) getActivity()).navigateToHome();
                    }
                } else {
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AuthResponse>> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                showError(getString(R.string.error_network));
            }
        });
    }

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String json = response.errorBody().string();
                JsonObject obj = new Gson().fromJson(json, JsonObject.class);
                if (obj.has("error") && !obj.get("error").isJsonNull()) {
                    return obj.get("error").getAsString();
                }
            }
        } catch (IOException ignored) { }
        return getString(R.string.error_generic);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
