package com.example.androidapp.ui.auth;

import android.app.AlertDialog;
import android.os.Bundle;
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
import com.example.androidapp.data.model.RegisterRequest;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.remote.AuthApi;
import com.example.androidapp.data.remote.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment {

    private EditText etFullName;
    private EditText etPhone;
    private EditText etEmail;
    private EditText etUsername;
    private EditText etPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvError;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etFullName = view.findViewById(R.id.etFullName);
        etPhone = view.findViewById(R.id.etPhone);
        etEmail = view.findViewById(R.id.etEmail);
        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
        progressBar = view.findViewById(R.id.progressBar);
        tvError = view.findViewById(R.id.tvError);

        TextView tvLoginLink = view.findViewById(R.id.tvLoginLink);

        btnRegister.setOnClickListener(v -> attemptRegister(view));

        // Volver al login
        tvLoginLink.setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
    }

    private void attemptRegister(View view) {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            tvError.setText(R.string.error_field_required);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        if (password.length() < 6) {
            tvError.setText(R.string.error_password_short);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        tvError.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        RegisterRequest request = new RegisterRequest(email, username, password, fullName, phone);
        AuthApi authApi = RetrofitClient.getInstance().create(AuthApi.class);

        authApi.register(request).enqueue(new Callback<ApiResponse<User.UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<User.UserResponse>> call,
                                   Response<ApiResponse<User.UserResponse>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    // Mostrar dialogo de exito y volver al login
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.register_success_title)
                            .setMessage(R.string.register_success_message)
                            .setPositiveButton(R.string.register_success_button, (d, w) ->
                                    Navigation.findNavController(view).navigateUp())
                            .setCancelable(false)
                            .show();
                } else {
                    String errorMsg = "Error desconocido";
                    if (response.body() != null && response.body().getError() != null) {
                        errorMsg = response.body().getError();
                    }
                    tvError.setText(errorMsg);
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User.UserResponse>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                tvError.setText(R.string.error_network);
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }
}
