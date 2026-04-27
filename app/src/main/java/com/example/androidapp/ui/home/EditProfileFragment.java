package com.example.androidapp.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidapp.R;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.model.UserUpdate;
import com.example.androidapp.data.remote.RetrofitClient;
import com.example.androidapp.data.remote.UserApi;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
public class EditProfileFragment extends Fragment {
    @Inject
    UserApi userApi;

    private ImageView ivFotoPerfil;
    private EditText etFullName;
    private EditText etEmail;
    private EditText etPhoneNumber;
    private Button btnGuardar;
    private TextView tvError;
    private ProgressBar progressBar;
    private String currentUsername;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivFotoPerfil = view.findViewById(R.id.ivFotoPerfilEditar);
        etFullName = view.findViewById(R.id.etNombre);
        etEmail = view.findViewById(R.id.etEmail);
        etPhoneNumber = view.findViewById(R.id.etTelefono);
        btnGuardar = view.findViewById(R.id.btnGuardar);
        tvError = view.findViewById(R.id.tvError);
        progressBar = view.findViewById(R.id.pbCargando);

        loadUserData();

        btnGuardar.setOnClickListener(v -> attemptUpdate(view));
    }

    //loadUserData carga los datos del usuario en las casillas al iniciar
    private void loadUserData() {
        progressBar.setVisibility(View.VISIBLE);


        String token = "Bearer " + TokenManager.getInstance(requireContext()).getToken();

        userApi.getUser(token).enqueue(new Callback<ApiResponse<User.UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<User.UserResponse>> call, Response<ApiResponse<User.UserResponse>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    User user = response.body().getData().getUser();
                    if (user != null) {
                        currentUsername = user.getUsername();
                        etFullName.setText(user.getFullName());
                        etEmail.setText(user.getEmail());
                        etPhoneNumber.setText(user.getPhoneNumber());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User.UserResponse>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    //si se apreta en guardar cambios se hace una peticion PUT para actualizar los datos del usuario
    private void attemptUpdate(View view) {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (email.isEmpty() || fullName.isEmpty()) {
            tvError.setText(R.string.error_field_required);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        tvError.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        btnGuardar.setEnabled(false);

        UserUpdate request = new UserUpdate(email, phoneNumber, fullName);


        String token = "Bearer " + TokenManager.getInstance(requireContext()).getToken();

        userApi.updateUser(token, request).enqueue(new Callback<ApiResponse<User.UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<User.UserResponse>> call,
                                   Response<ApiResponse<User.UserResponse>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnGuardar.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    Navigation.findNavController(view).popBackStack();
                } else {
                    String errorMsg = "Error al actualizar";
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
                btnGuardar.setEnabled(true);
                tvError.setText(R.string.error_network);
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }
}
