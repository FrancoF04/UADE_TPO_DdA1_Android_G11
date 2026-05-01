package com.example.androidapp.ui.news;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.androidapp.R;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.NewsDetail;
import com.example.androidapp.data.remote.NewsApi;
import com.example.androidapp.util.ImageLoader;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class NewsDetailFragment extends Fragment {

    private static final String TAG = "NewsDetailFragment";

    @Inject NewsApi newsApi;

    private ImageView ivImage;
    private TextView tvTitle;
    private TextView tvDate;
    private TextView tvContent;
    private ProgressBar loading;
    private TextView errorState;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ivImage = view.findViewById(R.id.ivImage);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvDate = view.findViewById(R.id.tvDate);
        tvContent = view.findViewById(R.id.tvContent);
        loading = view.findViewById(R.id.loading);
        errorState = view.findViewById(R.id.errorState);

        String newsId = getArguments() != null ? getArguments().getString("newsId") : null;
        if (newsId == null) {
            errorState.setVisibility(View.VISIBLE);
            return;
        }

        loading.setVisibility(View.VISIBLE);
        newsApi.getNewsById(newsId).enqueue(new Callback<ApiResponse<NewsDetail.Wrapper>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<NewsDetail.Wrapper>> call,
                                   @NonNull Response<ApiResponse<NewsDetail.Wrapper>> response) {
                if (!isAdded()) return;
                loading.setVisibility(View.GONE);
                ApiResponse<NewsDetail.Wrapper> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess()
                        && body.getData() != null && body.getData().getNews() != null) {
                    render(body.getData().getNews());
                } else {
                    errorState.setVisibility(View.VISIBLE);
                    Log.e(TAG, "load failed status=" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<NewsDetail.Wrapper>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loading.setVisibility(View.GONE);
                errorState.setVisibility(View.VISIBLE);
                Log.e(TAG, "load failed", t);
            }
        });
    }

    private void render(NewsDetail d) {
        tvTitle.setText(d.getTitle());
        tvDate.setText(d.getCreatedAt() != null ? d.getCreatedAt() : "");
        tvContent.setText(d.getContent() != null ? d.getContent() : d.getDescription());
        ImageLoader.load(ivImage, d.getImage());
    }
}
