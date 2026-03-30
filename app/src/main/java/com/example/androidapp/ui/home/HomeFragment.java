package com.example.androidapp.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidapp.R;
import com.example.androidapp.data.local.SessionManager;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.remote.RetrofitClient;
import com.example.androidapp.ui.adapter.ActivityAdapter;
import com.example.androidapp.ui.adapter.FeaturedAdapter;
import com.example.androidapp.ui.detail.ActivityDetailActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements ActivityAdapter.OnActivityClickListener {

    private static final int FEATURED_LIMIT = 5;

    private RecyclerView rvFeatured;
    private RecyclerView rvRecommended;
    private ProgressBar progressBar;
    private MaterialTextView tvEmpty;
    private FeaturedAdapter featuredAdapter;
    private ActivityAdapter recommendedAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFeatured = view.findViewById(R.id.rv_featured);
        rvRecommended = view.findViewById(R.id.rv_recommended);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmpty = view.findViewById(R.id.tv_empty);

        featuredAdapter = new FeaturedAdapter(this);
        rvFeatured.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFeatured.setAdapter(featuredAdapter);

        recommendedAdapter = new ActivityAdapter(this);
        rvRecommended.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecommended.setAdapter(recommendedAdapter);

        loadData();
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        loadFeatured();
        loadRecommended();
    }

    private void loadFeatured() {
        RetrofitClient.getInstance().getActivityApi()
                .getFeatured(FEATURED_LIMIT)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<Activity>>> call,
                                           @NonNull Response<ApiResponse<List<Activity>>> response) {
                        if (!isAdded()) return;

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess() && response.body().getData() != null) {
                            featuredAdapter.setActivities(response.body().getData());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<Activity>>> call,
                                          @NonNull Throwable t) {
                        if (!isAdded()) return;
                        showError(getString(R.string.error_network));
                    }
                });
    }

    private void loadRecommended() {
        SessionManager sessionManager = new SessionManager(requireContext());

        RetrofitClient.getInstance().getActivityApi()
                .getRecommended(sessionManager.getBearerToken())
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<List<Activity>>> call,
                                           @NonNull Response<ApiResponse<List<Activity>>> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess() && response.body().getData() != null) {
                            List<Activity> data = response.body().getData();
                            recommendedAdapter.setActivities(data);
                            tvEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                        } else {
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<List<Activity>>> call,
                                          @NonNull Throwable t) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
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
