package com.example.androidapp.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidapp.R;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.remote.ActivityApi;
import com.example.androidapp.data.remote.UserApi;
import com.example.androidapp.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
public class HomeFragment extends Fragment {

    @Inject
    ActivityApi api;
    @Inject
    UserApi userApi;
    private TextView tvWelcome;
    private TextView tvEmpty;
    private ListView listView;
    private ProgressBar progressBar;
    private ActivityAdapter adapter;

    private Button btnPerfil;

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
        listView = view.findViewById(R.id.listView);
        progressBar = view.findViewById(R.id.progressBar);

        String username = getArguments() != null
                ? getArguments().getString("username", "")
                : "";
        setWelcome(username);

        Button btnSearch = view.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_home_to_search));

        // boton a perfil
        btnPerfil = view.findViewById(R.id.btnPerfil);
        btnPerfil.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_home_to_profile);
        });

        adapter = new ActivityAdapter(requireContext());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, v, position, id) -> {
            Activity activity = adapter.getItem(position);
            Bundle args = new Bundle();
            args.putString("activityId", activity.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_home_to_detail, args);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Post to the next layout pass so the ListView has its final measured
        // dimensions before we populate it (avoids a race with the Navigation
        // enter animation / bottom-nav padding changes on first entry).
        listView.post(this::loadActivities);
    }

    private void loadActivities() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);


        api.getActivities(1, 20).enqueue(new Callback<ApiResponse<List<Activity>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Activity>>> call,
                                   Response<ApiResponse<List<Activity>>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<Activity> activities = filterUpcomingActivities(response.body().getData());
                    if (activities != null && !activities.isEmpty()) {
                        adapter.setActivities(activities);
                    } else {
                        adapter.setActivities(new ArrayList<>());
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmpty.setText(R.string.error_generic);
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Activity>>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                tvEmpty.setText(R.string.error_network);
                tvEmpty.setVisibility(View.VISIBLE);
                Log.e("HomeFragment", "Failed to load activities", t);
            }
        });
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
        String token = TokenManager.getInstance(requireContext()).getToken();
        if (token == null || token.trim().isEmpty()) {
            return;
        }

        userApi.getUser("Bearer " + token).enqueue(new Callback<ApiResponse<User.UserResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<User.UserResponse>> call,
                                   Response<ApiResponse<User.UserResponse>> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    return;
                }

                User.UserResponse userResponse = response.body().getData();
                User user = userResponse != null ? userResponse.getUser() : null;
                if (user == null) {
                    return;
                }

                String username = user.getUsername();
                if (username == null || username.trim().isEmpty()) {
                    username = user.getFullName();
                }

                if (username != null && !username.trim().isEmpty()) {
                    tvWelcome.setText("Bienvenido, " + username.trim() + "!");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User.UserResponse>> call, Throwable t) {
            }
        });
    }

    private List<Activity> filterUpcomingActivities(List<Activity> activities) {
        if (activities == null) {
            return new ArrayList<>();
        }

        return activities.stream()
                .filter(this::hasUpcomingSchedule)
                .collect(Collectors.toList());
    }

    private boolean hasUpcomingSchedule(Activity activity) {
        if (activity == null) {
            return false;
        }

        if (activity.getSchedules() != null && !activity.getSchedules().isEmpty()) {
            return activity.getSchedules().stream()
                    .anyMatch(schedule -> schedule != null && DateTimeUtils.isFutureOrNow(schedule.getDate()));
        }

        List<String> rawDates = activity.getDate();
        if (rawDates == null || rawDates.isEmpty()) {
            return true;
        }

        return rawDates.stream().anyMatch(DateTimeUtils::isFutureOrNow);
    }
}
