package com.example.androidapp.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
public class ActivityDetailFragment extends Fragment {
    @Inject
    ActivityApi api;

    private ImageButton btnBack;
    private ImageView ivImage;
    private TextView tvName;
    private TextView tvDestination;
    private TextView tvCategory;
    private TextView tvDuration;
    private TextView tvPrice;
    private TextView tvDescription;
    private TextView tvSpots;
    private TextView tvMeetingPoint;
    private TextView tvGuide;
    private TextView tvLanguage;
    private TextView tvIncluded;
    private TextView tvCancellation;
    private TextView tvError;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnBack = view.findViewById(R.id.btnBack);
        ivImage = view.findViewById(R.id.ivImage);
        tvName = view.findViewById(R.id.tvName);
        tvDestination = view.findViewById(R.id.tvDestination);
        tvCategory = view.findViewById(R.id.tvCategory);
        tvDuration = view.findViewById(R.id.tvDuration);
        tvPrice = view.findViewById(R.id.tvPrice);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvSpots = view.findViewById(R.id.tvSpots);
        tvMeetingPoint = view.findViewById(R.id.tvMeetingPoint);
        tvGuide = view.findViewById(R.id.tvGuide);
        tvLanguage = view.findViewById(R.id.tvLanguage);
        tvIncluded = view.findViewById(R.id.tvIncluded);
        tvCancellation = view.findViewById(R.id.tvCancellation);
        tvError = view.findViewById(R.id.tvError);
        progressBar = view.findViewById(R.id.progressBar);

        // Flecha para volver atras usando el NavController
        btnBack.setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        // Leer el argumento activityId que viene del HomeFragment via Bundle
        String activityId = getArguments() != null
                ? getArguments().getString("activityId", "")
                : "";

        if (!activityId.isEmpty()) {
            loadActivityDetail(activityId);
        }
    }

    private void loadActivityDetail(String activityId) {
        progressBar.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);



        api.getActivityById(activityId).enqueue(new Callback<ApiResponse<Activity>>() {
            @Override
            public void onResponse(Call<ApiResponse<Activity>> call,
                                   Response<ApiResponse<Activity>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    displayActivity(response.body().getData());
                } else {
                    tvError.setText(R.string.error_generic);
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Activity>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                tvError.setText(R.string.error_network);
                tvError.setVisibility(View.VISIBLE);
                Log.e("ActivityDetail", "Failed to load detail", t);
            }
        });
    }

    private void displayActivity(Activity activity) {
        tvName.setText(activity.getName());
        tvDestination.setText(activity.getDestination());
        tvCategory.setText(activity.getCategory());
        tvDuration.setText(getString(R.string.detail_duration, activity.getDuration()));
        tvLanguage.setText(getString(R.string.detail_language_value, activity.getLanguage()));

        if (activity.getPrice() == 0) {
            tvPrice.setText(R.string.free_label);
        } else {
            tvPrice.setText(getString(R.string.price_format, activity.getPrice()));
        }

        tvSpots.setText(getString(R.string.detail_spots, activity.getAvailableSpots()));
        tvDescription.setText(activity.getDescription());
        tvMeetingPoint.setText(activity.getMeetingPoint());

        if (activity.getGuide() != null) {
            String guideText = activity.getGuide().getName()
                    + " - " + getString(R.string.detail_rating, activity.getGuide().getRating());
            tvGuide.setText(guideText);
        }

        // Mostrar lista de "que incluye" como texto con viñetas
        List<String> included = activity.getIncluded();
        if (included != null && !included.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String item : included) {
                sb.append("• ").append(item).append("\n");
            }
            tvIncluded.setText(sb.toString().trim());
        }

        // Mostrar politica de cancelacion
        String cancellation = activity.getCancellationPolicy();
        if (cancellation != null && !cancellation.isEmpty()) {
            tvCancellation.setText(cancellation);
        }

        // Placeholder — no image loading library available (limited knowledge).
        ivImage.setImageDrawable(null);
    }
}
