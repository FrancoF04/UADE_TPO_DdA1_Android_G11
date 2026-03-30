package com.example.androidapp.ui.detail;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.androidapp.R;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.Guide;
import com.example.androidapp.data.remote.RetrofitClient;
import com.example.androidapp.ui.adapter.ActivityAdapter;
import com.example.androidapp.ui.adapter.GalleryAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ACTIVITY_ID = "extra_activity_id";

    private ViewPager2 viewPagerGallery;
    private LinearLayout indicatorContainer;
    private MaterialTextView tvName;
    private Chip chipCategory;
    private MaterialTextView tvDestination;
    private MaterialTextView tvDuration;
    private MaterialTextView tvLanguage;
    private MaterialTextView tvPrice;
    private MaterialTextView tvSpots;
    private MaterialTextView tvDescription;
    private MaterialTextView tvIncluded;
    private MaterialTextView tvMeetingPoint;
    private MaterialTextView tvGuideName;
    private MaterialTextView tvGuideRating;
    private MaterialTextView tvCancellation;
    private ProgressBar progressBar;
    private GalleryAdapter galleryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        initViews();

        String activityId = getIntent().getStringExtra(EXTRA_ACTIVITY_ID);
        if (activityId != null) {
            loadActivity(activityId);
        }
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        viewPagerGallery = findViewById(R.id.view_pager_gallery);
        indicatorContainer = findViewById(R.id.indicator_container);
        tvName = findViewById(R.id.tv_name);
        chipCategory = findViewById(R.id.chip_category);
        tvDestination = findViewById(R.id.tv_destination);
        tvDuration = findViewById(R.id.tv_duration);
        tvLanguage = findViewById(R.id.tv_language);
        tvPrice = findViewById(R.id.tv_price);
        tvSpots = findViewById(R.id.tv_spots);
        tvDescription = findViewById(R.id.tv_description);
        tvIncluded = findViewById(R.id.tv_included);
        tvMeetingPoint = findViewById(R.id.tv_meeting_point);
        tvGuideName = findViewById(R.id.tv_guide_name);
        tvGuideRating = findViewById(R.id.tv_guide_rating);
        tvCancellation = findViewById(R.id.tv_cancellation);
        progressBar = findViewById(R.id.progress_bar);

        galleryAdapter = new GalleryAdapter();
        viewPagerGallery.setAdapter(galleryAdapter);

        findViewById(R.id.btn_book).setOnClickListener(v ->
                Snackbar.make(v, "Reserva en desarrollo", Snackbar.LENGTH_SHORT).show());
    }

    private void loadActivity(String id) {
        progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getInstance().getActivityApi()
                .getActivityById(id)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Activity>> call,
                                           @NonNull Response<ApiResponse<Activity>> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess() && response.body().getData() != null) {
                            populateViews(response.body().getData());
                        } else {
                            showError(getString(R.string.error_generic));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Activity>> call,
                                          @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        showError(getString(R.string.error_network));
                    }
                });
    }

    private void populateViews(Activity activity) {
        tvName.setText(activity.getName());
        chipCategory.setText(ActivityAdapter.formatFilterName(activity.getCategory()));
        tvDestination.setText(activity.getDestination());
        tvDuration.setText(getString(R.string.detail_duration, activity.getDuration()));
        tvLanguage.setText(activity.getLanguage() != null ? activity.getLanguage() : "");

        if (activity.getPrice() == 0) {
            tvPrice.setText(R.string.free_label);
        } else {
            tvPrice.setText(getString(R.string.price_format, activity.getPrice()));
        }

        tvSpots.setText(getString(R.string.detail_spots, activity.getAvailableSpots()));
        tvDescription.setText(activity.getDescription());
        tvMeetingPoint.setText(activity.getMeetingPoint());
        tvCancellation.setText(activity.getCancellationPolicy());

        // Included items as bulleted list
        List<String> included = activity.getIncluded();
        if (included != null && !included.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < included.size(); i++) {
                sb.append("\u2022 ").append(included.get(i));
                if (i < included.size() - 1) {
                    sb.append("\n");
                }
            }
            tvIncluded.setText(sb.toString());
        }

        // Guide info
        Guide guide = activity.getGuide();
        if (guide != null) {
            tvGuideName.setText(guide.getName());
            tvGuideRating.setText(getString(R.string.detail_rating, guide.getRating()));
        }

        // Gallery
        List<String> gallery = activity.getGalleryUrls();
        if (gallery == null || gallery.isEmpty()) {
            gallery = new ArrayList<>();
            if (activity.getImageUrl() != null) {
                gallery.add(activity.getImageUrl());
            } else {
                gallery.add("");
            }
        }
        galleryAdapter.setImageUrls(gallery);
        setupIndicators(gallery.size());
    }

    private void setupIndicators(int count) {
        indicatorContainer.removeAllViews();
        if (count <= 1) return;

        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            int size = (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            int margin = (int) (4 * getResources().getDisplayMetrics().density);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == 0
                    ? R.color.md_theme_primary
                    : R.color.md_theme_outline);
            indicatorContainer.addView(dot);
        }

        viewPagerGallery.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < indicatorContainer.getChildCount(); i++) {
                    indicatorContainer.getChildAt(i).setBackgroundResource(
                            i == position ? R.color.md_theme_primary : R.color.md_theme_outline);
                }
            }
        });
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}
