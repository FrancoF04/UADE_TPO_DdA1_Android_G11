package com.example.androidapp.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.viewpager.widget.ViewPager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidapp.R;
import com.example.androidapp.data.local.OfflineBookingCache;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.FavoriteRequest;
import com.example.androidapp.data.model.FavoriteResponse;
import com.example.androidapp.data.model.Schedule;
import com.example.androidapp.data.remote.ActivityApi;
import com.example.androidapp.data.remote.FavoritesApi;
import com.example.androidapp.util.DateTimeUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
@AndroidEntryPoint
public class ActivityDetailFragment extends Fragment {
    private static final String TAG = "ActivityDetail";

    @Inject
    ActivityApi api;
    @Inject
    OfflineBookingCache offlineBookingCache;

    @Inject
    FavoritesApi favoritesApi;

    private boolean noveltyResetInProgress = false;
    public static final Set<String> viewedNovelties = Collections.synchronizedSet(new HashSet<>());
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
    private Button btnReserve;
    private ImageButton btnFavorite;
    private FrameLayout galleryCarousel;
    private ViewPager galleryViewPager;
    private TextView tvGalleryCounter;

    private boolean isFavorite = false;
    private String currentActivityId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
        btnReserve = view.findViewById(R.id.btnReserve);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        galleryCarousel = view.findViewById(R.id.galleryCarousel);
        galleryViewPager = view.findViewById(R.id.galleryViewPager);
        tvGalleryCounter = view.findViewById(R.id.tvGalleryCounter);

        boolean showReserve = getArguments() == null
                || getArguments().getBoolean("showReserveButton", true);
        boolean showSpots = getArguments() == null
                || getArguments().getBoolean("showSpotsField", true);
        btnReserve.setVisibility(showReserve ? View.VISIBLE : View.GONE);
        tvSpots.setVisibility(showSpots ? View.VISIBLE : View.GONE);

        btnReserve.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("activityId", getArguments() != null
                    ? getArguments().getString("activityId", "")
                    : "");
            Navigation.findNavController(v).navigate(R.id.action_activityDetail_to_reservationForm, bundle);
        });

        currentActivityId = getArguments() != null
                ? getArguments().getString("activityId", "")
                : "";

        if (!currentActivityId.isEmpty()) {
            loadActivityDetail(currentActivityId);
            checkIfFavorite();
        }

        btnFavorite.setOnClickListener(v -> toggleFavorite());
    }

    private void checkIfFavorite() {
        favoritesApi.getFavorites(System.currentTimeMillis()).enqueue(new Callback<ApiResponse<List<Activity>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Activity>>> call, Response<ApiResponse<List<Activity>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Activity> favorites = response.body().getData();
                    Activity favoriteVersion = null;
                    for (Activity f : favorites) {
                        if (f.getId().equals(currentActivityId)) {
                            favoriteVersion = f;
                            break;
                        }
                    }
                    
                    isFavorite = favoriteVersion != null;
                    updateFavoriteUI();

                    if (isFavorite && favoriteVersion != null) {
                        boolean hasNovelty = favoriteVersion.getPriceChanged() || favoriteVersion.getSpotsChanged();
                        Log.d(TAG, "Favorito verificado. Novedad detectada: " + hasNovelty);
                        
                        if (hasNovelty) {
                            viewedNovelties.add(currentActivityId);
                            resetNoveltyFlags(currentActivityId);
                        } else {
                            // Si el servidor ya está limpio, nosotros también
                            viewedNovelties.remove(currentActivityId);
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Activity>>> call, Throwable t) {}
        });
    }

    private void toggleFavorite() {
        if (isFavorite) {
            favoritesApi.removeFavorite(currentActivityId).enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful()) {
                        isFavorite = false;
                        viewedNovelties.remove(currentActivityId);
                        updateFavoriteUI();
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), R.string.favorites_error_remove, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            favoritesApi.addFavorite(new FavoriteRequest(currentActivityId)).enqueue(new Callback<ApiResponse<FavoriteResponse>>() {
                @Override
                public void onResponse(Call<ApiResponse<FavoriteResponse>> call, Response<ApiResponse<FavoriteResponse>> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful()) {
                        isFavorite = true;
                        updateFavoriteUI();
                    }
                }
                @Override
                public void onFailure(Call<ApiResponse<FavoriteResponse>> call, Throwable t) {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), R.string.favorites_error_add, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateFavoriteUI() {
        btnFavorite.setColorFilter(isFavorite ?
                requireContext().getColor(R.color.price_color) :
                requireContext().getColor(android.R.color.darker_gray));
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
                    Activity activity = response.body().getData();
                    displayActivity(activity);

                    // Si el detalle ya trae flags, disparamos el reset (la guardia evitará duplicados)
                    if (activity.getPriceChanged() || activity.getSpotsChanged()) {
                        Log.i(TAG, "Novedad detectada en Detail API, reseteando...");
                        viewedNovelties.add(activity.getId());
                        resetNoveltyFlags(activity.getId());
                    } else {
                        // Limpiamos si ya no hay novedad
                        viewedNovelties.remove(activity.getId());
                    }
                } else {
                    tvError.setText(R.string.error_generic);
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Activity>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Activity cached = offlineBookingCache.getActivityById(activityId);
                if (cached != null) {
                    displayActivity(cached);
                } else {
                    tvError.setText(R.string.error_network);
                    tvError.setVisibility(View.VISIBLE);
                }
                Log.e("ActivityDetail", "Failed to load detail", t);
            }
        });
    }

    private void resetNoveltyFlags(String activityId) {
        if (noveltyResetInProgress || activityId == null || activityId.isEmpty()) return;
        noveltyResetInProgress = true;

        Log.i(TAG, "Iniciando reset de novedad (Remove -> Add) para: " + activityId);
        
        favoritesApi.removeFavorite(activityId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Removido de favoritos para reset. Procediendo a re-agregar...");
                    
                    favoritesApi.addFavorite(new FavoriteRequest(activityId)).enqueue(new Callback<ApiResponse<FavoriteResponse>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<FavoriteResponse>> call, Response<ApiResponse<FavoriteResponse>> response) {
                            if (response.isSuccessful()) {
                                Log.i(TAG, "Reset de novedad completado exitosamente en servidor.");
                            } else {
                                Log.e(TAG, "Error al re-agregar favorito post-reset: " + response.code());
                            }
                            noveltyResetInProgress = false;
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<FavoriteResponse>> call, Throwable t) {
                            Log.e(TAG, "Fallo de red al re-agregar favorito", t);
                            noveltyResetInProgress = false;
                        }
                    });
                } else {
                    Log.e(TAG, "Error al remover favorito para reset: " + response.code());
                    noveltyResetInProgress = false;
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Fallo de red al remover favorito", t);
                noveltyResetInProgress = false;
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

        int spotsToShow = getSpotsForNextAvailableDate(activity);
        tvSpots.setText(getString(R.string.detail_spots, spotsToShow));
        tvDescription.setText(activity.getDescription());
        if (activity.getMeetingPoint() != null) {
            tvMeetingPoint.setText(activity.getMeetingPoint().toDisplayString());
            double lat = activity.getMeetingPoint().getLatitude();
            double lng = activity.getMeetingPoint().getLongitude();
            if (lat != 0 || lng != 0) {
                View mapContainer = requireView().findViewById(R.id.mapContainer);
                mapContainer.setVisibility(View.VISIBLE);
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.mapContainer, MapFragment.newInstance(
                                lat, lng, activity.getMeetingPoint().getAddress(), 15.0f))
                        .commitAllowingStateLoss();
            }
        }

        if (activity.getGuide() != null) {
            String guideText = activity.getGuide().getName()
                    + " - " + getString(R.string.detail_rating, activity.getGuide().getRating());
            tvGuide.setText(guideText);
        }

        List<String> included = activity.getIncluded();
        if (included != null && !included.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String item : included) {
                sb.append("• ").append(item).append("\n");
            }
            tvIncluded.setText(sb.toString().trim());
        }

        String cancellation = activity.getCancellationPolicy();
        if (cancellation != null && !cancellation.isEmpty()) {
            tvCancellation.setText(cancellation);
        }

        boolean hasUpcomingDates = hasUpcomingSchedules(activity);
        btnReserve.setEnabled(hasUpcomingDates);
        if (!hasUpcomingDates) {
            btnReserve.setText(R.string.detail_no_dates);
        } else {
            btnReserve.setText(R.string.detail_reserve);
        }

        List<String> allPhotos = new ArrayList<>();
        if (activity.getImageUrl() != null && !activity.getImageUrl().isEmpty()) {
            allPhotos.add(activity.getImageUrl());
        }
        List<String> galleryUrls = activity.getGalleryUrls();
        if (galleryUrls != null) {
            allPhotos.addAll(galleryUrls);
        }
        int total = allPhotos.size();
        tvGalleryCounter.setVisibility(total > 1 ? View.VISIBLE : View.GONE);
        if (total > 1) {
            tvGalleryCounter.setText("1 / " + total);
        }
        galleryViewPager.setAdapter(new GalleryPagerAdapter(allPhotos));
        galleryViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                tvGalleryCounter.setText((position + 1) + " / " + total);
            }
        });
    }

    private int getSpotsForNextAvailableDate(Activity activity) {
        List<Schedule> schedules = activity.getSchedules();
        if (schedules != null && !schedules.isEmpty()) {
            Schedule nextSchedule = null;
            int nextScheduleSpots = activity.getAvailableSpots();

            for (Schedule schedule : schedules) {
                if (schedule == null || schedule.getDate() == null) {
                    continue;
                }

                if (!DateTimeUtils.isFutureOrNow(schedule.getDate())) {
                    continue;
                }

                if (nextSchedule == null || isBefore(schedule.getDate(), nextSchedule.getDate())) {
                    nextSchedule = schedule;
                    nextScheduleSpots = schedule.getAvailableSpots();
                }
            }

            if (nextSchedule != null) {
                return nextScheduleSpots;
            }
        }

        List<String> rawDates = activity.getDate();
        if (rawDates == null || rawDates.isEmpty()) return activity.getAvailableSpots();

        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");
        LocalDate latest = null;
        Integer spotsForLatest = null;
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        for (String raw : rawDates) {
            if (raw == null) continue;
            Matcher m = datePattern.matcher(raw);
            if (!m.find()) continue;
            String dateStr = m.group(1);
            try {
                LocalDate d = LocalDate.parse(dateStr, fmt);
                if (d.isBefore(LocalDate.now())) {
                    continue;
                }

                if (latest == null || d.isBefore(latest)) {
                    latest = d;
                    String after = raw.substring(m.end());
                    Matcher numM = numberPattern.matcher(after);
                    if (numM.find()) {
                        try {
                            spotsForLatest = Integer.parseInt(numM.group(1));
                        } catch (NumberFormatException ignored) {
                            spotsForLatest = null;
                        }
                    } else {
                        spotsForLatest = null;
                    }
                }
            } catch (DateTimeParseException ignored) {
            }
        }

        if (spotsForLatest != null) return spotsForLatest;
        return activity.getAvailableSpots();
    }

    private boolean hasUpcomingSchedules(Activity activity) {
        List<Schedule> schedules = activity.getSchedules();
        if (schedules != null && !schedules.isEmpty()) {
            for (Schedule schedule : schedules) {
                if (schedule != null && DateTimeUtils.isFutureOrNow(schedule.getDate())) {
                    return true;
                }
            }
            return false;
        }

        List<String> dates = activity.getDate();
        if (dates == null || dates.isEmpty()) {
            return false;
        }

        for (String date : dates) {
            if (DateTimeUtils.isFutureOrNow(date)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBefore(String firstDate, String secondDate) {
        try {
            return parseDateTime(firstDate).isBefore(parseDateTime(secondDate));
        } catch (RuntimeException ignored) {
            return firstDate.compareTo(secondDate) < 0;
        }
    }

    private LocalDateTime parseDateTime(String rawDate) {
        try {
            return OffsetDateTime.parse(rawDate).toLocalDateTime();
        } catch (RuntimeException ignored) {
            return LocalDateTime.parse(rawDate);
        }
    }
}
