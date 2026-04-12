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
import com.example.androidapp.data.model.LoginRequest;
import com.example.androidapp.data.remote.AuthApi;
import com.example.androidapp.data.remote.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvError;

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

        TextView tvOtpLink = view.findViewById(R.id.tvOtpLink);
        TextView tvRegisterLink = view.findViewById(R.id.tvRegisterLink);

        btnLogin.setOnClickListener(v -> attemptLogin(view));

        tvOtpLink.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_otp_request));

        tvRegisterLink.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_register));
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

        AuthApi authApi = RetrofitClient.getInstance().create(AuthApi.class);

        authApi.login(request).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                   Response<ApiResponse<AuthResponse>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
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

    private void navigateToHome(View view, String username) {
        Bundle args = new Bundle();
        args.putString("username", username);

        Navigation.findNavController(view)
                .navigate(R.id.action_login_to_home, args);
    }
}
