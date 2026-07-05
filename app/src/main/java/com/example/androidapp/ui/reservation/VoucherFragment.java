package com.example.androidapp.ui.reservation;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.example.androidapp.R;
import com.example.androidapp.data.local.OfflineBookingCache;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.remote.ActivityApi;
import com.example.androidapp.ui.home.MapFragment;
import com.example.androidapp.ui.home.GalleryPagerAdapter;
import com.example.androidapp.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.androidapp.util.NetworkMonitor;

@AndroidEntryPoint
public class VoucherFragment extends Fragment implements NetworkMonitor.OnNetworkChangeListener {
    @Inject
    ActivityApi activityApi;

    @Inject
    OfflineBookingCache offlineBookingCache;

    @Inject
    NetworkMonitor networkMonitor;

    // UI elements
    private FrameLayout galleryCarousel;
    private ViewPager galleryViewPager;
    private TextView tvGalleryCounter;
    private TextView tvName;
    private TextView tvDestination;
    private TextView tvLanguage;
    private TextView tvDateSelected;
    private TextView tvDuration;
    private TextView tvMeetingPoint;
    private TextView tvGuide;
    private TextView tvMembersCount;
    private TextView tvMembersReserved;

    // Error and progress UI elements
    private ProgressBar progressBar;
    private TextView tvError;
    private TextView tvOfflineBanner;

    // params
    private String currentActivityId;
    private String dateSelected;
    private String quantitySelected;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_voucher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        galleryCarousel = view.findViewById(R.id.galleryCarousel);
        galleryViewPager = view.findViewById(R.id.galleryViewPager);
        tvGalleryCounter = view.findViewById(R.id.tvGalleryCounter);
        tvName = view.findViewById(R.id.tvName);
        tvDestination = view.findViewById(R.id.tvDestination);
        tvLanguage = view.findViewById(R.id.tvLanguage);
        tvDateSelected = view.findViewById(R.id.tvDateSelected);
        tvDuration = view.findViewById(R.id.tvDuration);
        tvMeetingPoint = view.findViewById(R.id.tvMeetingPoint);
        tvGuide = view.findViewById(R.id.tvGuide);
        tvMembersCount = view.findViewById(R.id.tvMembersCount);
        tvMembersReserved = view.findViewById(R.id.tvMembersReserved);

        progressBar = view.findViewById(R.id.progressBar);
        tvError = view.findViewById(R.id.tvError);
        tvOfflineBanner = view.findViewById(R.id.tvOfflineBanner);

        Bundle args = getArguments();
        if (args != null) {
            currentActivityId = args.getString("activityId");
            dateSelected = args.getString("date");
            quantitySelected = args.getString("quantity");
        }

        if (currentActivityId != null && !currentActivityId.isEmpty()) {
            loadVoucherDetails(currentActivityId);
        } else {
            tvError.setText(R.string.error_generic);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        networkMonitor.register(this);
        // recargar en base al estado de la red
        if (currentActivityId != null && !currentActivityId.isEmpty()) {
            loadVoucherDetails(currentActivityId);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        networkMonitor.unregister(this);
    }

    @Override
    public void onNetworkAvailable() {
        if(!isAdded() || !isResumed()) return;
        Toast.makeText(requireContext(), R.string.network_available, Toast.LENGTH_SHORT).show();
        updateOfflineBanner(true);
        if (currentActivityId != null && !currentActivityId.isEmpty()) {
            loadVoucherDetails(currentActivityId);
        }
    }

    @Override
    public void onNetworkLost() {
        if(!isAdded() || !isResumed()) return;
        Toast.makeText(requireContext(), R.string.network_lost, Toast.LENGTH_SHORT).show();
        updateOfflineBanner(false);
        loadFromCache();
    }

    private void loadVoucherDetails(String activityId) {
        progressBar.setVisibility(View.VISIBLE);

        activityApi.getActivityById(activityId).enqueue(new Callback<ApiResponse<Activity>>(){
            @Override
            public void onResponse(Call<ApiResponse<Activity>> call, Response<ApiResponse<Activity>> response){
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null
                    && response.body().isSuccess() && response.body().getData() != null){
                        Activity activity = response.body().getData();
                        displayVoucher(activity);
                } else {
                    loadFromCache();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Activity>> call, Throwable t){
                if(!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                loadFromCache();
                Log.e("VoucherDetail", "Failed to load detail", t);
            }
        });
    }

    private void loadFromCache(){
        if (currentActivityId == null || currentActivityId.isEmpty()) return;
        Activity cached = offlineBookingCache.getActivityById(currentActivityId);
        if (cached != null){
            displayVoucher(cached);
        } else {
            tvError.setText(R.string.error_network);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private void displayVoucher(Activity activity){
        // Configurar carrusel de fotos
        List<String> allPhotos = new ArrayList<>();
        if (activity.getCoverUrl() != null && !activity.getCoverUrl().isEmpty()) {
            allPhotos.add(activity.getCoverUrl());
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

        tvName.setText(activity.getName());
        tvDestination.setText(activity.getDestination());
        tvLanguage.setText(getString(R.string.detail_language_value, activity.getLanguage()));

        tvDateSelected.setText(DateTimeUtils.formatFriendly(dateSelected));
        tvDuration.setText(getString(R.string.detail_duration, activity.getDuration()));

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

        tvMembersCount.setText(String.valueOf(activity.getTotalSpots() - activity.getAvailableSpots()));
        tvMembersReserved.setText(quantitySelected);
    }

    private void updateOfflineBanner(boolean isOnline){
        if (tvOfflineBanner != null){
            tvOfflineBanner.setVisibility(isOnline ? View.GONE : View.VISIBLE);
        }
    }
}
