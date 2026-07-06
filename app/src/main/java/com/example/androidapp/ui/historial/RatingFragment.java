package com.example.androidapp.ui.historial;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.androidapp.R;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.Rating;
import com.example.androidapp.data.model.RatingData;
import com.example.androidapp.data.model.RatingRequest;
import com.example.androidapp.data.remote.RatingsApi;
import com.example.androidapp.util.DateTimeUtils;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class RatingFragment extends Fragment {

    @Inject
    RatingsApi ratingsApi;

    private RatingBar rbActivity;
    private RatingBar rbGuide;
    private EditText etComment;
    private Button btnEnviar;
    private TextView tvActivityName;
    private TextView tvCharCount;
    private TextView tvMessage;
    private ProgressBar progressBar;

    private static final Pattern DURATION_HOURS_PATTERN = Pattern.compile("([\\d.]+)\\s*horas?", Pattern.CASE_INSENSITIVE);

    private String bookingId;
    private String activityName;
    private String activityDate;
    private String activityDuration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rating, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvActivityName = view.findViewById(R.id.tvActivityName);
        rbActivity = view.findViewById(R.id.rbActivity);
        rbGuide = view.findViewById(R.id.rbGuide);
        etComment = view.findViewById(R.id.etComment);
        tvCharCount = view.findViewById(R.id.tvCharCount);
        tvMessage = view.findViewById(R.id.tvMessage);
        btnEnviar = view.findViewById(R.id.btnEnviar);
        progressBar = view.findViewById(R.id.progressBar);

        Bundle args = getArguments();
        if (args != null) {
            bookingId = args.getString("bookingId", "");
            activityName = args.getString("activityName", "");
            activityDate = args.getString("activityDate", "");
            activityDuration = args.getString("activityDuration", "");
        }

        tvActivityName.setText(activityName);

        etComment.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tvCharCount.setText(s.length() + "/300");
            }
        });

        btnEnviar.setOnClickListener(v -> submitRating());

        loadExistingRating();
    }

    private void loadExistingRating() {
        if (bookingId == null || bookingId.isEmpty()) {
            showError("No se pudo identificar la reserva.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        ratingsApi.getRating(bookingId).enqueue(new Callback<ApiResponse<RatingData>>() {
            @Override
            public void onResponse(Call<ApiResponse<RatingData>> call, Response<ApiResponse<RatingData>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null
                        && response.body().getData().getRating() != null) {
                    showReadOnly(response.body().getData().getRating());
                } else {
                    showForm();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<RatingData>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Log.e("RatingFragment", "Error cargando calificación", t);
                showForm();
            }
        });
    }

    private void showForm() {
        if (!isWithin48Hours()) {
            setFormEnabled(false);
            tvMessage.setText("El período de calificación ha vencido (48 horas desde la finalización de la actividad).");
            tvMessage.setVisibility(View.VISIBLE);
            btnEnviar.setVisibility(View.GONE);
        }
    }

    private void showReadOnly(Rating rating) {
        rbActivity.setRating(rating.getActivityRating());
        rbGuide.setRating(rating.getGuideRating());
        if (!rating.getComment().isEmpty()) {
            etComment.setText(rating.getComment());
            tvCharCount.setText(rating.getComment().length() + "/300");
        }

        setFormEnabled(false);
        btnEnviar.setVisibility(View.GONE);

        String fechaFormateada = formatDate(rating.getCreatedAt());
        tvMessage.setText("Ya calificaste esta actividad" + (fechaFormateada != null ? " el " + fechaFormateada : "") + ".");
        tvMessage.setVisibility(View.VISIBLE);
    }

    private void submitRating() {
        int activityRating = (int) rbActivity.getRating();
        int guideRating = (int) rbGuide.getRating();

        if (activityRating == 0 || guideRating == 0) {
            Toast.makeText(requireContext(),
                    "Por favor calificá la actividad y el guía.", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = etComment.getText().toString().trim();
        RatingRequest request = new RatingRequest(
                bookingId,
                activityRating,
                guideRating,
                comment.isEmpty() ? null : comment
        );

        progressBar.setVisibility(View.VISIBLE);
        btnEnviar.setEnabled(false);

        ratingsApi.submitRating(request).enqueue(new Callback<ApiResponse<RatingData>>() {
            @Override
            public void onResponse(Call<ApiResponse<RatingData>> call, Response<ApiResponse<RatingData>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnEnviar.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(requireContext(), "¡Calificación enviada!", Toast.LENGTH_SHORT).show();
                    Rating saved = response.body().getData() != null ? response.body().getData().getRating() : null;
                    if (saved != null) {
                        showReadOnly(saved);
                    } else {
                        setFormEnabled(false);
                        btnEnviar.setVisibility(View.GONE);
                        tvMessage.setText("¡Calificación enviada!");
                        tvMessage.setVisibility(View.VISIBLE);
                    }
                } else {
                    String errorMsg = "No se pudo enviar la calificación.";
                    if (response.code() == 409) {
                        try {
                            String body = response.errorBody() != null ? response.errorBody().string() : "";
                            if (body.contains("ya fue calificada")) {
                                errorMsg = "Esta reserva ya fue calificada.";
                            } else if (body.contains("48 horas")) {
                                errorMsg = "El período de calificación ha vencido (48 horas).";
                            } else if (body.contains("no finalizo") || body.contains("finalizo")) {
                                errorMsg = "La actividad aún no ha finalizado.";
                            }
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                    Log.e("RatingFragment", "Error HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<RatingData>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                btnEnviar.setEnabled(true);
                Log.e("RatingFragment", "Error enviando calificación", t);
                Toast.makeText(requireContext(), "Error de conexión. Intentá de nuevo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setFormEnabled(boolean enabled) {
        rbActivity.setIsIndicator(!enabled);
        rbGuide.setIsIndicator(!enabled);
        etComment.setEnabled(enabled);
        etComment.setFocusable(enabled);
        etComment.setFocusableInTouchMode(enabled);
    }

    private boolean isWithin48Hours() {
        if (activityDate == null || activityDate.isEmpty()) return true;
        try {
            Instant activityInstant = Instant.parse(activityDate);
            Instant finalization = activityInstant.plus(parseDurationMinutes(activityDuration), ChronoUnit.MINUTES);
            Instant deadline = finalization.plus(48, ChronoUnit.HOURS);
            return Instant.now().isBefore(deadline);
        } catch (Exception e) {
            return true;
        }
    }

    // Replica el parseo de duración del backend ("2.5 horas" -> minutos) para que el deadline coincida
    private long parseDurationMinutes(String duration) {
        if (duration == null || duration.isEmpty()) return 0;
        Matcher matcher = DURATION_HOURS_PATTERN.matcher(duration);
        if (!matcher.find()) return 0;
        try {
            double hours = Double.parseDouble(matcher.group(1));
            return Math.round(hours * 60);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return null;
        try {
            Instant inst = Instant.parse(isoDate);
            ZonedDateTime zdt = ZonedDateTime.ofInstant(inst, DateTimeUtils.ARGENTINA_ZONE);
            return zdt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return null;
        }
    }

    private void showError(String message) {
        tvMessage.setText(message);
        tvMessage.setVisibility(View.VISIBLE);
        btnEnviar.setVisibility(View.GONE);
    }
}
