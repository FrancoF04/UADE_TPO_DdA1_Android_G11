package com.example.androidapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidapp.R;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.model.UserPreferencesRequest;
import com.example.androidapp.data.remote.UserApi;
import com.example.androidapp.util.BiometricHelper;
import com.example.androidapp.util.BiometricStatus;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    @Inject
    UserApi userApi;
    @Inject
    TokenManager tokenManager;
    @Inject
    BiometricHelper biometricHelper;
    private TextView tvNombre, tvEmail, tvTelefono, tvBiometricSubtitle;
    private Button btnEditarPerfil, btnPreferencias;
    private Switch switchBiometric;
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
        switchBiometric = view.findViewById(R.id.switchBiometric);
        tvBiometricSubtitle = view.findViewById(R.id.tvBiometricSubtitle);

        btnEditarPerfil.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_profile_to_editProfile);
        });

        btnPreferencias.setOnClickListener(v -> {
            showPreferencesDialog();
        });

        wireBiometricToggle();
        loadUserProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (switchBiometric != null) wireBiometricToggle();
    }

    private void wireBiometricToggle() {
        BiometricStatus status = biometricHelper.checkAvailability();

        switch (status) {
            case AVAILABLE: {
                switchBiometric.setEnabled(true);
                switchBiometric.setOnCheckedChangeListener(null);
                switchBiometric.setChecked(tokenManager.isBiometricEnabled());
                tvBiometricSubtitle.setText("Activá la biometría para tu próximo ingreso");
                switchBiometric.setOnCheckedChangeListener((v, isChecked) -> {
                    if (isChecked) {
                        biometricHelper.promptForAuth(
                                requireActivity(),
                                "Activar huella",
                                "Confirmá tu huella para activar el ingreso biométrico",
                                () -> tokenManager.setBiometricEnabled(true),
                                (code, msg) -> wireBiometricToggle()
                        );
                    } else {
                        tokenManager.setBiometricEnabled(false);
                    }
                });
                break;
            }
            case NOT_ENROLLED: {
                switchBiometric.setEnabled(true);
                switchBiometric.setOnCheckedChangeListener(null);
                switchBiometric.setChecked(false);
                tvBiometricSubtitle.setText("Necesitás enrolar tu huella en el sistema");
                switchBiometric.setOnCheckedChangeListener((v, isChecked) -> {
                    if (isChecked) {
                        switchBiometric.setOnCheckedChangeListener(null);
                        switchBiometric.setChecked(false);
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Enrolá tu huella")
                                .setMessage("Para usar biometría tenés que enrolar tu huella en Configuración. ¿Vamos ahora?")
                                .setPositiveButton("Ir", (d, w) -> startActivity(biometricHelper.enrollIntent()))
                                .setNegativeButton("Cancelar", null)
                                .show();
                        wireBiometricToggle();
                    }
                });
                break;
            }
            case NO_HARDWARE:
            case UNAVAILABLE:
            default: {
                switchBiometric.setEnabled(false);
                switchBiometric.setOnCheckedChangeListener(null);
                switchBiometric.setChecked(false);
                tvBiometricSubtitle.setText("Tu dispositivo no soporta biometría");
                break;
            }
        }
    }

    private void showPreferencesDialog() {
        String[] items = getResources().getStringArray(R.array.travel_categories);
        boolean[] checkedItems = new boolean[items.length];

        // Marcar los que ya tiene el usuario
        if (currentUser != null && currentUser.getPreferences() != null && currentUser.getPreferences().getCategories() != null) {
            List<String> userCategories = currentUser.getPreferences().getCategories();
            for (int i = 0; i < items.length; i++) {
                for (String userCat : userCategories) {
                    if (userCat.equalsIgnoreCase(items[i])) {
                        checkedItems[i] = true;
                        break;
                    }
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

        UserPreferencesRequest request = new UserPreferencesRequest(categories);


        userApi.updatePreferences(request).enqueue(new Callback<ApiResponse<User.UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<User.UserResponse>> call, Response<ApiResponse<User.UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Actualizar el usuario local con la respuesta del servidor
                    currentUser = response.body().getData().getUser();
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
        String rawToken = tokenManager.getToken();

        if (rawToken == null) {
            Toast.makeText(getContext(), "Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }


        userApi.getUser().enqueue(new Callback<ApiResponse<User.UserResponse>>() {
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
