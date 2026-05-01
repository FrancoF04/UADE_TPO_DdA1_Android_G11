package com.example.androidapp.ui.news;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidapp.R;
import com.example.androidapp.data.local.NewsCache;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.News;
import com.example.androidapp.data.remote.NewsApi;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class NewsFragment extends Fragment {

    private static final String TAG = "NewsFragment";

    @Inject NewsApi newsApi;
    @Inject NewsCache newsCache;

    private NewsAdapter adapter;
    private ListView lvNews;
    private TextView offlineBanner;
    private ProgressBar loading;
    private LinearLayout emptyState;
    private LinearLayout errorState;
    private Button btnRetry;

    private boolean cachedShown = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lvNews = view.findViewById(R.id.lvNews);
        offlineBanner = view.findViewById(R.id.offlineBanner);
        loading = view.findViewById(R.id.loading);
        emptyState = view.findViewById(R.id.emptyState);
        errorState = view.findViewById(R.id.errorState);
        btnRetry = view.findViewById(R.id.btnRetry);

        adapter = new NewsAdapter(requireContext());
        lvNews.setAdapter(adapter);

        lvNews.setOnItemClickListener((parent, v, position, id) -> onItemClick(adapter.getItem(position)));
        btnRetry.setOnClickListener(v -> fetch());

        List<News> cached = newsCache.read();
        if (cached != null) {
            adapter.setItems(cached);
            cachedShown = true;
        } else {
            loading.setVisibility(View.VISIBLE);
        }

        fetch();
    }

    private void fetch() {
        errorState.setVisibility(View.GONE);
        newsApi.getNews(null, null).enqueue(new Callback<ApiResponse<List<News>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<News>>> call,
                                   @NonNull Response<ApiResponse<List<News>>> response) {
                if (!isAdded()) return;
                loading.setVisibility(View.GONE);
                ApiResponse<List<News>> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess() && body.getData() != null) {
                    List<News> items = body.getData();
                    adapter.setItems(items);
                    newsCache.save(items);
                    offlineBanner.setVisibility(View.GONE);
                    emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    handleFailure(null, "status=" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<News>>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loading.setVisibility(View.GONE);
                handleFailure(t, null);
            }
        });
    }

    private void handleFailure(Throwable t, String detail) {
        Log.e(TAG, "fetch failed " + (detail != null ? detail : ""), t);
        if (cachedShown) {
            offlineBanner.setVisibility(View.VISIBLE);
        } else {
            errorState.setVisibility(View.VISIBLE);
        }
    }

    private void onItemClick(News item) {
        if (item == null) return;
        Bundle args = new Bundle();
        if (item.hasRelatedActivity()) {
            args.putString("activityId", item.getActivityId());
            args.putBoolean("showReserveButton", true);
            args.putBoolean("showSpotsField", true);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_news_to_activity_detail, args);
        } else {
            args.putString("newsId", item.getId());
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_news_to_news_detail, args);
        }
    }
}
