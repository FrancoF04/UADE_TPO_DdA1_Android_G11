package com.example.androidapp.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
public class SearchFragment extends Fragment {
    @Inject
    ActivityApi api;
    private EditText etDestination;
    private EditText etCategory;
    private Button btnSearch;
    private ImageButton btnBack;
    private ListView listView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ActivityAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnBack = view.findViewById(R.id.btnBack);
        etDestination = view.findViewById(R.id.etDestination);
        etCategory = view.findViewById(R.id.etCategory);
        btnSearch = view.findViewById(R.id.btnSearch);
        listView = view.findViewById(R.id.listView);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        btnBack.setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        adapter = new ActivityAdapter(requireContext());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, v, position, id) -> {
            Activity activity = adapter.getItem(position);
            Bundle args = new Bundle();
            args.putString("activityId", activity.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_search_to_detail, args);
        });

        btnSearch.setOnClickListener(v -> search());
    }

    private void search() {
        String destination = etDestination.getText().toString().trim();
        String category = etCategory.getText().toString().trim();

        String destParam = destination.isEmpty() ? null : destination;
        String catParam = category.isEmpty() ? null : category;

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);


        api.searchActivities(1, 20, destParam, catParam)
                .enqueue(new Callback<ApiResponse<List<Activity>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Activity>>> call,
                                           Response<ApiResponse<List<Activity>>> response) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            List<Activity> results = response.body().getData();
                            if (results != null && !results.isEmpty()) {
                                adapter.setActivities(results);
                                tvEmpty.setVisibility(View.GONE);
                            } else {
                                adapter.setActivities(new ArrayList<>());
                                tvEmpty.setText(R.string.search_empty);
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
                        Log.e("SearchFragment", "Search failed", t);
                    }
                });
    }
}
