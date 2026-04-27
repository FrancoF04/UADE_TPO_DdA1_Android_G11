package com.example.androidapp.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidapp.R;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.model.UserPreferencesRequest;
import com.example.androidapp.data.remote.RetrofitClient;
import com.example.androidapp.data.remote.UserApi;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvNombre, tvEmail, tvTelefono;
    private Button btnEditarPerfil, btnPreferencias;
    private User currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNombre = view.findViewById(R.id.tvNombre);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvTelefono = view.findViewById(R.id.tvTelefono);
        btnEditarPerfil = view.findViewById(R.id.btnEditarPerfil);
        btnPreferencias = view.findViewById(R.id.btnPreferencias);

        btnEditarPerfil.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_profile_to_editProfile);
        });

        btnPreferencias.setOnClickListener(v -> {
            showPreferencesDialog();
        });

        loadUserProfile();
    }

    private void showPreferencesDialog() {
        String[] items = getResources().getStringArray(R.array.travel_categories);
        boolean[] checkedItems = new boolean[items.length];

        // Marcar los que ya tiene el usuario
        if (currentUser != null && currentUser.getPreferences() != null && currentUser.getPreferences().getCategories() != null) {
            List<String> userCategories = currentUser.getPreferences().getCategories();
            for (int i = 0; i < items.length; i++) {
                if (userCategories.contains(items[i].toLowerCase())) {
                    checkedItems[i] = true;
                }
            }
        }
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Selecciona tus intereses")
                .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Guardar", (dialog, which) -> {
                    List<String> selected = new ArrayList<>();
                    for (int i = 0; i < items.length; i++) {
                        if (checkedItems[i]) {
                            selected.add(items[i].toLowerCase());
                        }
                    }
                    savePreferences(selected);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void savePreferences(List<String> categories) {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("auth_token", "");

        UserApi userApi = RetrofitClient.getInstance().create(UserApi.class);
        UserPreferencesRequest request = new UserPreferencesRequest(categories);

        userApi.updatePreferences(token, request).enqueue(new Callback<ApiResponse<User.UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<User.UserResponse>> call, Response<ApiResponse<User.UserResponse>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Preferencias actualizadas", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Error al guardar preferencias", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User.UserResponse>> call, Throwable t) {
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserProfile() {
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String rawToken = prefs.getString("auth_token", null);

        if (rawToken == null) {
            Toast.makeText(getContext(), "Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = "Bearer " + rawToken;
        UserApi userApi = RetrofitClient.getInstance().create(UserApi.class);

        userApi.getUser(token).enqueue(new Callback<ApiResponse<User.UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<User.UserResponse>> call, Response<ApiResponse<User.UserResponse>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    currentUser = response.body().getData().getUser();
                    if (currentUser != null) {
                        //que se muestre la info del usuario en las casillas
                        displayUserData(currentUser);
                    }
                } else {
                    Toast.makeText(getContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User.UserResponse>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserData(User user) {
        if (user != null) {
            tvNombre.setText(user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : user.getUsername());
            tvEmail.setText(user.getEmail());
            tvTelefono.setText(user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty() ? user.getPhoneNumber() : "No especificado");
        }
    }
}
