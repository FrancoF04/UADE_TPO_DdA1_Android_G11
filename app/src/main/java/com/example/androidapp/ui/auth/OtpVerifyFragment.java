package com.example.androidapp.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.example.androidapp.data.model.OtpRequest;
import com.example.androidapp.data.model.OtpVerify;
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

public class OtpVerifyFragment extends Fragment {

    private static final String ARG_EMAIL = "email";
    private static final long COUNTDOWN_MILLIS = 30000;
    private static final long COUNTDOWN_INTERVAL = 1000;
    private static final int OTP_CODE_LENGTH = 6;

    private String email;
    private TextInputLayout tilCode;
    private TextInputEditText etCode;
    private MaterialButton btnVerify;
    private MaterialButton btnResend;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;

    public static OtpVerifyFragment newInstance(String email) {
        OtpVerifyFragment fragment = new OtpVerifyFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            email = getArguments().getString(ARG_EMAIL, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp_verify, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialTextView tvSubtitle = view.findViewById(R.id.tv_subtitle);
        tilCode = view.findViewById(R.id.til_code);
        etCode = view.findViewById(R.id.et_code);
        btnVerify = view.findViewById(R.id.btn_verify);
        btnResend = view.findViewById(R.id.btn_resend);
        progressBar = view.findViewById(R.id.progress_bar);

        tvSubtitle.setText(getString(R.string.otp_verify_subtitle, email));

        btnVerify.setOnClickListener(v -> verifyOtp());
        btnResend.setOnClickListener(v -> resendOtp());

        startCountdown();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void verifyOtp() {
        String code = etCode.getText() != null ? etCode.getText().toString().trim() : "";

        tilCode.setError(null);

        if (code.isEmpty()) {
            tilCode.setError(getString(R.string.error_field_required));
            return;
        }
        if (code.length() != OTP_CODE_LENGTH) {
            tilCode.setError(getString(R.string.error_field_required));
            return;
        }

        setLoading(true);

        OtpVerify request = new OtpVerify(email, code);
        RetrofitClient.getInstance().getAuthApi().verifyOtp(request).enqueue(new Callback<>() {
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

    private void resendOtp() {
        btnResend.setEnabled(false);

        OtpRequest request = new OtpRequest(email);
        RetrofitClient.getInstance().getAuthApi().resendOtp(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Map<String, String>>> call,
                                   @NonNull Response<ApiResponse<Map<String, String>>> response) {
                if (!isAdded()) return;
                startCountdown();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Map<String, String>>> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                showError(getString(R.string.error_network));
                btnResend.setEnabled(true);
            }
        });
    }

    private void startCountdown() {
        btnResend.setEnabled(false);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(COUNTDOWN_MILLIS, COUNTDOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isAdded()) {
                    long seconds = millisUntilFinished / 1000;
                    btnResend.setText(getString(R.string.otp_verify_resend_countdown, seconds));
                }
            }

            @Override
            public void onFinish() {
                if (isAdded()) {
                    btnResend.setText(R.string.otp_verify_resend);
                    btnResend.setEnabled(true);
                }
            }
        }.start();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnVerify.setEnabled(!loading);
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
