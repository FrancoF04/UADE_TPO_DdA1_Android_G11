package com.example.androidapp.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidapp.R;
import com.example.androidapp.data.local.PreferencesStore;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.model.UserPreferencesRequest;
import com.example.androidapp.data.remote.UserApi;
import com.example.androidapp.util.ApiErrorParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class PreferencesFragment extends Fragment {

    private static final List<String> CATEGORIES = Arrays.asList(
            "free_tour", "guided_visit", "excursion", "gastronomic", "adventure"
    );
    private static final List<String> DESTINATIONS = Arrays.asList(
            "Buenos Aires", "Bariloche", "Mendoza", "Ushuaia", "Córdoba", "Salta"
    );

    @Inject UserApi userApi;
    @Inject PreferencesStore preferencesStore;

    private LinearLayout containerCategories;
    private LinearLayout containerDestinations;
    private Button btnSave;
    private Button btnBack;

    private final Set<String> selectedCategories = new HashSet<>();
    private final Set<String> selectedDestinations = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_preferences, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        containerCategories = view.findViewById(R.id.containerCategories);
        containerDestinations = view.findViewById(R.id.containerDestinations);
        btnSave = view.findViewById(R.id.btnSave);
        btnBack = view.findViewById(R.id.btnBack);

        selectedCategories.addAll(preferencesStore.getCategories());
        selectedDestinations.addAll(preferencesStore.getDestinations());

        renderCheckboxes(containerCategories, CATEGORIES, selectedCategories, this::prettyCategory);
        renderCheckboxes(containerDestinations, DESTINATIONS, selectedDestinations, label -> label);

        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        btnSave.setOnClickListener(v -> save());
    }

    private interface Labeller { String label(String key); }

    private void renderCheckboxes(LinearLayout container, List<String> items, Set<String> selectedSet, Labeller labeller) {
        container.removeAllViews();
        for (String key : items) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(labeller.label(key));
            cb.setChecked(selectedSet.contains(key));
            cb.setOnCheckedChangeListener((view, isChecked) -> {
                if (isChecked) selectedSet.add(key);
                else selectedSet.remove(key);
            });
            container.addView(cb);
        }
    }

    private String prettyCategory(String key) {
        switch (key) {
            case "free_tour": return "Free Tour";
            case "guided_visit": return "Visita Guiada";
            case "excursion": return "Excursión";
            case "gastronomic": return "Gastronómica";
            case "adventure": return "Aventura";
            default: return key;
        }
    }

    private void save() {
        List<String> cats = new ArrayList<>(selectedCategories);
        List<String> dests = new ArrayList<>(selectedDestinations);
        btnSave.setEnabled(false);

        UserPreferencesRequest req = new UserPreferencesRequest(cats, dests);
        userApi.updatePreferences(req).enqueue(new Callback<ApiResponse<User.UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<User.UserResponse>> call, Response<ApiResponse<User.UserResponse>> response) {
                if (!isAdded()) return;
                btnSave.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    preferencesStore.save(cats, dests);
                    Toast.makeText(getContext(), "Preferencias guardadas", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(PreferencesFragment.this).popBackStack();
                } else {
                    Toast.makeText(getContext(), ApiErrorParser.extractMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User.UserResponse>> call, Throwable t) {
                if (!isAdded()) return;
                btnSave.setEnabled(true);
                Toast.makeText(getContext(), "Sin conexión, reintentá", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
