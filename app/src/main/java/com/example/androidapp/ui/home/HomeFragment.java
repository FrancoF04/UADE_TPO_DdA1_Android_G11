package com.example.androidapp.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidapp.R;
import com.example.androidapp.data.local.PreferencesStore;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.Filters;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.remote.ActivityApi;
import com.example.androidapp.data.remote.UserApi;
import com.example.androidapp.util.FilterQueryBuilder;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private enum ChipMode { ALL, FEATURED, FOR_YOU }

    @Inject ActivityApi api;
    @Inject UserApi userApi;
    @Inject TokenManager tokenManager;
    @Inject PreferencesStore preferencesStore;

    private TextView tvWelcome;
    private TextView tvEmpty;
    private TextView tvEmptyState;
    private ListView listView;
    private ProgressBar progressBar;
    private ActivityAdapter adapter;
    private Button btnPerfil;
    private Button chipFeatured, chipForYou, chipAll, chipFilters;

    private ChipMode currentChip = ChipMode.ALL;
    private Filters currentFilters = new Filters();
    private int currentPage = 1;
    private int totalAvailable = 0;
    private boolean loading = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        listView = view.findViewById(R.id.listView);
        progressBar = view.findViewById(R.id.progressBar);
        chipFeatured = view.findViewById(R.id.chipFeatured);
        chipForYou = view.findViewById(R.id.chipForYou);
        chipAll = view.findViewById(R.id.chipAll);
        chipFilters = view.findViewById(R.id.chipFilters);

        String username = getArguments() != null
                ? getArguments().getString("username", "")
                : "";
        setWelcome(username);

        Button btnSearch = view.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_home_to_search));

        btnPerfil = view.findViewById(R.id.btnPerfil);
        btnPerfil.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_home_to_profile));

        adapter = new ActivityAdapter(requireContext());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, v, position, id) -> {
            Activity activity = adapter.getItem(position);
            Bundle args = new Bundle();
            args.putString("activityId", activity.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_home_to_detail, args);
        });

        setupChips();
        setupFiltersResultListener();
        setupScrollListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        listView.post(() -> loadCurrent(true));
    }

    private void setupChips() {
        chipAll.setOnClickListener(v -> { currentChip = ChipMode.ALL; refreshChipStyles(); loadCurrent(true); });
        chipFeatured.setOnClickListener(v -> { currentChip = ChipMode.FEATURED; refreshChipStyles(); loadCurrent(true); });
        chipForYou.setOnClickListener(v -> { currentChip = ChipMode.FOR_YOU; refreshChipStyles(); loadCurrent(true); });
        chipFilters.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putParcelable(FiltersFragment.ARG_FILTERS, currentFilters);
            NavHostFragment.findNavController(this).navigate(R.id.action_home_to_filters, args);
        });
        refreshChipStyles();
    }

    private void refreshChipStyles() {
        chipAll.setBackgroundResource(currentChip == ChipMode.ALL ? R.drawable.chip_background_active : R.drawable.chip_background);
        chipFeatured.setBackgroundResource(currentChip == ChipMode.FEATURED ? R.drawable.chip_background_active : R.drawable.chip_background);
        chipForYou.setBackgroundResource(currentChip == ChipMode.FOR_YOU ? R.drawable.chip_background_active : R.drawable.chip_background);
        chipAll.setTextColor(currentChip == ChipMode.ALL ? 0xFFFFFFFF : 0xFF334155);
        chipFeatured.setTextColor(currentChip == ChipMode.FEATURED ? 0xFFFFFFFF : 0xFF334155);
        chipForYou.setTextColor(currentChip == ChipMode.FOR_YOU ? 0xFFFFFFFF : 0xFF334155);
    }

    private void setupFiltersResultListener() {
        getParentFragmentManager().setFragmentResultListener(
                FiltersFragment.RESULT_KEY,
                getViewLifecycleOwner(),
                (key, bundle) -> {
                    Filters f = bundle.getParcelable(FiltersFragment.RESULT_KEY);
                    if (f != null) {
                        currentFilters = f;
                        currentChip = ChipMode.ALL;
                        refreshChipStyles();
                        loadCurrent(true);
                    }
                }
        );
    }

    private void setupScrollListener() {
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
            @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (currentChip != ChipMode.ALL) return;
                if (loading) return;
                if (totalItemCount == 0) return;
                if (firstVisibleItem + visibleItemCount >= totalItemCount - 3 && adapter.getCount() < totalAvailable) {
                    loadCurrent(false);
                }
            }
        });
    }

    private void loadCurrent(boolean replace) {
        if (replace) currentPage = 1;
        loading = true;
        tvEmptyState.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        switch (currentChip) {
            case FEATURED:
                api.getFeatured().enqueue(simpleListCallback(replace));
                break;
            case FOR_YOU:
                if (!preferencesStore.hasAnyPreference()) {
                    loading = false;
                    progressBar.setVisibility(View.GONE);
                    if (replace) adapter.setActivities(new ArrayList<>());
                    tvEmptyState.setText("Configurá tus preferencias en tu perfil para ver actividades recomendadas.");
                    tvEmptyState.setVisibility(View.VISIBLE);
                    return;
                }
                api.getRecommended().enqueue(simpleListCallback(replace));
                break;
            case ALL:
            default:
                api.getActivitiesFiltered(
                        replace ? 1 : currentPage,
                        10,
                        FilterQueryBuilder.build(currentFilters)
                ).enqueue(allCallback(replace));
                break;
        }
    }

    private Callback<ApiResponse<List<Activity>>> simpleListCallback(boolean replace) {
        return new Callback<ApiResponse<List<Activity>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Activity>>> call,
                                   Response<ApiResponse<List<Activity>>> response) {
                if (!isAdded()) return;
                loading = false;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Activity> data = response.body().getData();
                    if (replace) adapter.setActivities(data); else adapter.append(data);
                    if (data.isEmpty() && replace) {
                        tvEmptyState.setText("No hay actividades para mostrar.");
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(getContext(), "No pudimos cargar las actividades", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Activity>>> call, Throwable t) {
                if (!isAdded()) return;
                loading = false;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Sin conexión, reintentá", Toast.LENGTH_SHORT).show();
                Log.e("HomeFragment", "Failed to load list", t);
            }
        };
    }

    private Callback<ApiResponse<List<Activity>>> allCallback(boolean replace) {
        return new Callback<ApiResponse<List<Activity>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Activity>>> call,
                                   Response<ApiResponse<List<Activity>>> response) {
                if (!isAdded()) return;
                loading = false;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Activity> items = response.body().getData();
                    if (response.body().getMeta() != null) {
                        totalAvailable = response.body().getMeta().getTotal();
                    }
                    if (replace) {
                        adapter.setActivities(items);
                        currentPage = 2;
                    } else {
                        adapter.append(items);
                        currentPage++;
                    }
                    if (items.isEmpty() && replace) {
                        tvEmptyState.setText("No encontramos actividades con esos filtros. Probá ajustarlos.");
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(getContext(), "No pudimos cargar las actividades", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Activity>>> call, Throwable t) {
                if (!isAdded()) return;
                loading = false;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Sin conexión, reintentá", Toast.LENGTH_SHORT).show();
                Log.e("HomeFragment", "Failed to load activities", t);
            }
        };
    }

    private void setWelcome(String username) {
        if (username != null && !username.trim().isEmpty()) {
            tvWelcome.setText("Bienvenido, " + username.trim() + "!");
            return;
        }
        tvWelcome.setText("Bienvenido!");
        loadWelcomeFromProfile();
    }

    private void loadWelcomeFromProfile() {
        String token = tokenManager.getToken();
        if (token == null || token.trim().isEmpty()) return;

        userApi.getUser().enqueue(new Callback<ApiResponse<User.UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<User.UserResponse>> call,
                                   Response<ApiResponse<User.UserResponse>> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null || !response.body().isSuccess()) return;
                User.UserResponse userResponse = response.body().getData();
                User user = userResponse != null ? userResponse.getUser() : null;
                if (user == null) return;
                String username = user.getUsername();
                if (username == null || username.trim().isEmpty()) username = user.getFullName();
                if (username != null && !username.trim().isEmpty()) {
                    tvWelcome.setText("Bienvenido, " + username.trim() + "!");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User.UserResponse>> call, Throwable t) {}
        });
    }
}
