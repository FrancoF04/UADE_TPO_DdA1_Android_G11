package com.example.androidapp.ui.profile;

import android.content.Intent;
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
import com.example.androidapp.data.model.FilterOptions;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.model.UserPreferences;
import com.example.androidapp.data.remote.RetrofitClient;
import com.example.androidapp.ui.adapter.ActivityAdapter;
import com.example.androidapp.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private ChipGroup chipGroupCategories;
    private ChipGroup chipGroupDestinations;
    private ProgressBar progressBar;
    private SessionManager sessionManager;
    private List<String> userCategories = new ArrayList<>();
    private List<String> userDestinations = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        chipGroupCategories = view.findViewById(R.id.chip_group_categories);
        chipGroupDestinations = view.findViewById(R.id.chip_group_destinations);
        progressBar = view.findViewById(R.id.progress_bar);
        MaterialButton btnSave = view.findViewById(R.id.btn_save);
        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);

        btnSave.setOnClickListener(v -> savePreferences());
        btnLogout.setOnClickListener(v -> logout());

        loadUserProfile();
        loadFilters();
    }

    private void loadUserProfile() {
        RetrofitClient.getInstance().getAuthApi()
                .getMe(sessionManager.getBearerToken())
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<User>> call,
                                           @NonNull Response<ApiResponse<User>> response) {
                        if (!isAdded()) return;

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess() && response.body().getData() != null) {
                            User user = response.body().getData();

                            UserPreferences prefs = user.getPreferences();
                            if (prefs != null) {
                                userCategories = prefs.getCategories() != null
                                        ? prefs.getCategories() : new ArrayList<>();
                                userDestinations = prefs.getDestinations() != null
                                        ? prefs.getDestinations() : new ArrayList<>();
                                updateChipSelections();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<User>> call,
                                          @NonNull Throwable t) {
                        // Use cached data
                    }
                });
    }

    private void loadFilters() {
        RetrofitClient.getInstance().getActivityApi().getFilters().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<FilterOptions>> call,
                                   @NonNull Response<ApiResponse<FilterOptions>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    FilterOptions filters = response.body().getData();
                    populateChips(chipGroupCategories, filters.getCategories(), userCategories);
                    populateChips(chipGroupDestinations, filters.getDestinations(), userDestinations);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<FilterOptions>> call,
                                  @NonNull Throwable t) {
                // Silently handle
            }
        });
    }

    private void populateChips(ChipGroup chipGroup, List<String> items, List<String> selected) {
        chipGroup.removeAllViews();
        if (items == null) return;

        for (String item : items) {
            Chip chip = new Chip(requireContext());
            chip.setText(ActivityAdapter.formatFilterName(item));
            chip.setTag(item); // store raw value for API calls
            chip.setCheckable(true);
            chip.setChecked(selected != null && selected.contains(item));
            chipGroup.addView(chip);
        }
    }

    private void updateChipSelections() {
        updateChipGroupSelection(chipGroupCategories, userCategories);
        updateChipGroupSelection(chipGroupDestinations, userDestinations);
    }

    private void updateChipGroupSelection(ChipGroup chipGroup, List<String> selected) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                Object tag = chip.getTag();
                String rawValue = tag != null ? tag.toString() : chip.getText().toString();
                chip.setChecked(selected != null && selected.contains(rawValue));
            }
        }
    }

    private void savePreferences() {
        List<String> categories = getCheckedChips(chipGroupCategories);
        List<String> destinations = getCheckedChips(chipGroupDestinations);

        progressBar.setVisibility(View.VISIBLE);

        UserPreferences preferences = new UserPreferences(categories, destinations);
        RetrofitClient.getInstance().getUserApi()
                .updatePreferences(sessionManager.getBearerToken(), preferences)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<User>> call,
                                           @NonNull Response<ApiResponse<User>> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            Snackbar.make(requireView(), R.string.profile_saved,
                                    Snackbar.LENGTH_SHORT).show();
                        } else {
                            showError(getString(R.string.error_generic));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<User>> call,
                                          @NonNull Throwable t) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        showError(getString(R.string.error_network));
                    }
                });
    }

    private List<String> getCheckedChips(ChipGroup chipGroup) {
        List<String> checked = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                // Use tag (raw value) if available, fallback to text
                Object tag = child.getTag();
                checked.add(tag != null ? tag.toString() : ((Chip) child).getText().toString());
            }
        }
        return checked;
    }

    private void logout() {
        RetrofitClient.getInstance().getAuthApi()
                .logout(sessionManager.getBearerToken())
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<java.util.Map<String, String>>> call,
                                           @NonNull Response<ApiResponse<java.util.Map<String, String>>> response) {
                        // Always clear session regardless of response
                        performLogout();
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<java.util.Map<String, String>>> call,
                                          @NonNull Throwable t) {
                        performLogout();
                    }
                });
    }

    private void performLogout() {
        if (!isAdded()) return;
        sessionManager.clear();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
