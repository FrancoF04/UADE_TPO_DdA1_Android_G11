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
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.remote.ActivityApi;
import com.example.androidapp.data.remote.RetrofitClient;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
public class HomeFragment extends Fragment {

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
        tvWelcome.setText("Bienvenido, " + username + "!");

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

        loadActivities();
    }

    private void loadActivities() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        ActivityApi api = RetrofitClient.getInstance().create(ActivityApi.class);

        api.getActivities(1, 20).enqueue(new Callback<ApiResponse<List<Activity>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Activity>>> call,
                                   Response<ApiResponse<List<Activity>>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<Activity> activities = response.body().getData();
                    if (activities != null && !activities.isEmpty()) {
                        adapter.setActivities(activities);
                    } else {
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
}
