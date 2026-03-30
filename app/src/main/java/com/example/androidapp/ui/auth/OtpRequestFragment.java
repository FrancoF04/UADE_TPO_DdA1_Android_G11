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
import com.example.androidapp.data.model.OtpRequest;
import com.example.androidapp.data.remote.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpRequestFragment extends Fragment {

    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnSend;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilEmail = view.findViewById(R.id.til_email);
        etEmail = view.findViewById(R.id.et_email);
        btnSend = view.findViewById(R.id.btn_send_otp);
        progressBar = view.findViewById(R.id.progress_bar);
        MaterialTextView tvLoginLink = view.findViewById(R.id.tv_login_link);

        btnSend.setOnClickListener(v -> requestOtp());

        tvLoginLink.setOnClickListener(v -> {
            if (getActivity() instanceof LoginActivity) {
                ((LoginActivity) getActivity()).showLogin();
            }
        });
    }

    private void requestOtp() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        tilEmail.setError(null);

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_field_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        setLoading(true);

        OtpRequest request = new OtpRequest(email);
        RetrofitClient.getInstance().getAuthApi().requestOtp(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Map<String, String>>> call,
                                   @NonNull Response<ApiResponse<Map<String, String>>> response) {
                if (!isAdded()) return;
                setLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    if (getActivity() instanceof LoginActivity) {
                        ((LoginActivity) getActivity()).showOtpVerify(email);
                    }
                } else {
                    showError(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Map<String, String>>> call,
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
        btnSend.setEnabled(!loading);
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
