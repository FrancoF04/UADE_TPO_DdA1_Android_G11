package com.example.androidapp.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.androidapp.R;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.RegisterRequest;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.remote.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

public class RegisterFragment extends Fragment {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private TextInputLayout tilFullname;
    private TextInputLayout tilEmail;
    private TextInputLayout tilUsername;
    private TextInputLayout tilPassword;
    private TextInputEditText etFullname;
    private TextInputEditText etEmail;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilFullname = view.findViewById(R.id.til_fullname);
        tilEmail = view.findViewById(R.id.til_email);
        tilUsername = view.findViewById(R.id.til_username);
        tilPassword = view.findViewById(R.id.til_password);
        etFullname = view.findViewById(R.id.et_fullname);
        etEmail = view.findViewById(R.id.et_email);
        etUsername = view.findViewById(R.id.et_username);
        etPassword = view.findViewById(R.id.et_password);
        btnRegister = view.findViewById(R.id.btn_register);
        progressBar = view.findViewById(R.id.progress_bar);
        MaterialTextView tvLoginLink = view.findViewById(R.id.tv_login_link);

        btnRegister.setOnClickListener(v -> attemptRegister());

        tvLoginLink.setOnClickListener(v -> {
            if (getActivity() instanceof LoginActivity) {
                ((LoginActivity) getActivity()).showLogin();
            }
        });
    }

    private void attemptRegister() {
        String fullName = getText(etFullname);
        String email = getText(etEmail);
        String username = getText(etUsername);
        String password = getText(etPassword);

        tilFullname.setError(null);
        tilEmail.setError(null);
        tilUsername.setError(null);
        tilPassword.setError(null);

        boolean valid = true;

        if (fullName.isEmpty()) {
            tilFullname.setError(getString(R.string.error_field_required));
            valid = false;
        }
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_field_required));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            valid = false;
        }
        if (username.isEmpty()) {
            tilUsername.setError(getString(R.string.error_field_required));
            valid = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_field_required));
            valid = false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            tilPassword.setError(getString(R.string.error_password_short));
            valid = false;
        }

        if (!valid) return;

        setLoading(true);

        RegisterRequest request = new RegisterRequest(email, username, password, fullName);
        RetrofitClient.getInstance().getAuthApi().register(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call,
                                   @NonNull Response<ApiResponse<User>> response) {
                if (!isAdded()) return;
                setLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    showRegisterSuccessDialog(email);
                } else {
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                showError(getString(R.string.error_network));
            }
        });
    }

    private void showRegisterSuccessDialog(String email) {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.register_success_title)
                .setMessage(getString(R.string.register_success_message, email))
                .setPositiveButton(R.string.register_success_button, (dialog, which) -> {
                    if (getActivity() instanceof LoginActivity) {
                        ((LoginActivity) getActivity()).showLogin();
                    }
                })
                .setCancelable(false)
                .show();
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

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
