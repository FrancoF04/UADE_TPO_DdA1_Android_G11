public class VoucherFragment extends Fragment {
    @Inject
    UserApi userApi;
    @Inject
    ActivityApi activityApi;

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

        currentActivityId = getArguments().getInt("activityId");
        dateSelected = getArguments().getString("date");
        quantitySelected = getArguments().getString("quantity");

        if (!currentActivityId.isEmpty()) {
            loadVoucherDetails(currentActivityId);
        }
    }

    private void loadVoucherDetails(String activityId) {
        progressBar.setVisibility(View.VISIBLE);

        api.getActivityById(activityId).enqueque(new Callback<apiResponse<Activity>>(){
            @Override
            public void onResponse(Call<ApiResponse<Activity>> call, Response<ApiResponse<Activity>> response){
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null
                    && response.body().issuccess() && response.body().getData() != null){
                        Activity activity = response.body.getData();
                        displayVoucher(activity);

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
            public void onFailure(Call<ApiResponse<Activity>> call, Throwable t){
                if(!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Activity cached = offlineBooking.getActivityById(activityId);
                if(cached != null){
                    displayVoucher(cached);
                } else {
                    tvError.setText(R.string.error_network);
                    tvError.setVisibility(View.VISIBLE);
                }
                Log.e("VoucherDetail", "Failed to load detail", t);
            }
        });
    }

    private void displayVoucher(Activity activity){
        tvName.setText(activity.getName());
        tvDestination.setText(activity.getDestination());
        tvLanguage.setText(getString(R.string.detail_language_value, activity.getLanguage()));
        
        tvDateSelected.setText(dateSelected);
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

        tvMembersCount.setText(activity.getTotalSpots() - activity.getAvailableSpots());
        tvMembersReserved.setText(quantitySelected);
    }
}
