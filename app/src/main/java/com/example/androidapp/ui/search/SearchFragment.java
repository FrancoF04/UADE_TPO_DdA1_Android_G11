package com.example.androidapp.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidapp.R;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.FilterOptions;
import com.example.androidapp.data.remote.RetrofitClient;
import com.example.androidapp.ui.adapter.ActivityAdapter;
import com.example.androidapp.ui.detail.ActivityDetailActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment implements ActivityAdapter.OnActivityClickListener {

    private static final int PAGE_SIZE = 10;

    private AutoCompleteTextView actDestination;
    private ChipGroup chipGroupCategories;
    private MaterialButton btnDate;
    private RangeSlider rangeSliderPrice;
    private RecyclerView rvResults;
    private ProgressBar progressBar;
    private MaterialTextView tvEmpty;
    private ActivityAdapter adapter;

    private String selectedDestination;
    private String selectedCategory;
    private String selectedDate;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMorePages = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        actDestination = view.findViewById(R.id.act_destination);
        chipGroupCategories = view.findViewById(R.id.chip_group_categories);
        btnDate = view.findViewById(R.id.btn_date);
        rangeSliderPrice = view.findViewById(R.id.range_slider_price);
        rangeSliderPrice.setValues(0f, 50000f);
        rvResults = view.findViewById(R.id.rv_results);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);
        MaterialButton btnClearFilters = view.findViewById(R.id.btn_clear_filters);

        adapter = new ActivityAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvResults.setLayoutManager(layoutManager);
        rvResults.setAdapter(adapter);

        // Pagination scroll listener
        rvResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int totalItemCount = layoutManager.getItemCount();
                int lastVisible = layoutManager.findLastVisibleItemPosition();

                if (!isLoading && hasMorePages && lastVisible >= totalItemCount - 2) {
                    currentPage++;
                    loadActivities(false);
                }
            }
        });

        // Destination filter
        actDestination.setOnItemClickListener((parent, v, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            selectedDestination = getString(R.string.search_all_destinations).equals(selected)
                    ? null : selected;
            resetAndSearch();
        });

        // Date picker
        btnDate.setOnClickListener(v -> showDatePicker());

        // Price range
        rangeSliderPrice.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                resetAndSearch();
            }
        });

        // Clear filters
        btnClearFilters.setOnClickListener(v -> clearFilters());

        loadFilters();
        loadActivities(true);
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
                    populateDestinations(filters.getDestinations());
                    populateCategories(filters.getCategories());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<FilterOptions>> call,
                                  @NonNull Throwable t) {
                // Silently handle - filters are optional
            }
        });
    }

    private void populateDestinations(List<String> destinations) {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.search_all_destinations));
        if (destinations != null) {
            items.addAll(destinations);
        }
        ArrayAdapter<String> destAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, items);
        actDestination.setAdapter(destAdapter);
    }

    private void populateCategories(List<String> categories) {
        chipGroupCategories.removeAllViews();
        if (categories == null) return;

        for (String category : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(ActivityAdapter.formatFilterName(category));
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                selectedCategory = isChecked ? category : null;
                resetAndSearch();
            });
            chipGroupCategories.addView(chip);
        }
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.search_date))
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedDate = sdf.format(new Date(selection));
            btnDate.setText(selectedDate);
            resetAndSearch();
        });

        picker.show(getParentFragmentManager(), "date_picker");
    }

    private void clearFilters() {
        selectedDestination = null;
        selectedCategory = null;
        selectedDate = null;
        actDestination.setText("", false);
        chipGroupCategories.clearCheck();
        rangeSliderPrice.setValues(0f, 50000f);
        btnDate.setText(R.string.search_date);
        resetAndSearch();
    }

    private void resetAndSearch() {
        currentPage = 1;
        hasMorePages = true;
        loadActivities(true);
    }

    private void loadActivities(boolean resetList) {
        if (isLoading) return;
        isLoading = true;

        if (resetList) {
            progressBar.setVisibility(View.VISIBLE);
        }

        List<Float> priceValues = rangeSliderPrice.getValues();
        Double priceMin = priceValues.size() >= 2 && priceValues.get(0) > 0
                ? (double) priceValues.get(0) : null;
        Double priceMax = priceValues.size() >= 2 && priceValues.get(1) < 50000
                ? (double) priceValues.get(1) : null;

        RetrofitClient.getInstance().getActivityApi()
                .getActivities(currentPage, PAGE_SIZE, selectedDestination,
                        selectedCategory, selectedDate, priceMin, priceMax)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<Activity>>> call,
                                           @NonNull Response<ApiResponse<List<Activity>>> response) {
                        if (!isAdded()) return;
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess() && response.body().getData() != null) {
                            List<Activity> data = response.body().getData();
                            hasMorePages = data.size() >= PAGE_SIZE;

                            if (resetList) {
                                adapter.setActivities(data);
                                tvEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                            } else {
                                adapter.addActivities(data);
                            }
                        } else {
                            if (resetList) {
                                adapter.setActivities(new ArrayList<>());
                                tvEmpty.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<Activity>>> call,
                                          @NonNull Throwable t) {
                        if (!isAdded()) return;
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        showError(getString(R.string.error_network));
                    }
                });
    }

    @Override
    public void onActivityClick(Activity activity) {
        Intent intent = new Intent(getContext(), ActivityDetailActivity.class);
        intent.putExtra(ActivityDetailActivity.EXTRA_ACTIVITY_ID, activity.getId());
        startActivity(intent);
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
