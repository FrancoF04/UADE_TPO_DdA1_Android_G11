package com.example.androidapp.ui.reservation;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.androidapp.R;
import com.example.androidapp.data.local.OfflineBookingCache;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.OfflineBundle;
import com.example.androidapp.data.model.ReservationRequest;
import com.example.androidapp.data.model.Schedule;
import com.example.androidapp.data.remote.ActivityApi;
import com.example.androidapp.data.remote.BookingsApi;
import com.example.androidapp.data.remote.UserApi;
import com.example.androidapp.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class ReservationFormFragment extends Fragment {
    @Inject
    ActivityApi activityApi;
    @Inject
    UserApi userApi;
    @Inject
    BookingsApi bookingsApi;
    @Inject
    OfflineBookingCache offlineBookingCache;

    private String activityId;

    private TextView tvTitulo;
    private Spinner sDate;
    private Spinner sTime;
    private TextView tvAvailableSpots;
    private int cantidad = 1;
    private TextView tvCantidadPersonas;
    private Button btnCantidadMas;
    private Button btnCantidadMenos;
    private Button btnConfirmar;

    private Map<String, List<ScheduleOption>> dateToScheduleOptions = new LinkedHashMap<>();
    private int fallbackAvailableSpots = -1;

    private static class ScheduleOption {
        final String displayTime;
        final String selectedDate;
        final int availableSpots;

        ScheduleOption(String displayTime, String selectedDate, int availableSpots) {
            this.displayTime = displayTime;
            this.selectedDate = selectedDate;
            this.availableSpots = availableSpots;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reservation_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Leer activityId pasado por Navigation
        activityId = getArguments() != null
        ? getArguments().getString("activityId", "")
        : "";

        initViews(view);

        if (!activityId.isEmpty()) {
            loadActivity(activityId);
        } else {
            showUnknownError();
        }
    }

    private void initViews(@NonNull View view) {
        tvTitulo = view.findViewById(R.id.tvTitleForm);
        sDate = view.findViewById(R.id.sDate);
        tvAvailableSpots = view.findViewById(R.id.tvAvailableSpots);
        sTime = view.findViewById(R.id.sTime);
        btnCantidadMenos = view.findViewById(R.id.btnCantidadMenos);
        btnCantidadMas = view.findViewById(R.id.btnCantidadMas);
        tvCantidadPersonas = view.findViewById(R.id.tvCantidadPersonas);
        btnConfirmar = view.findViewById(R.id.btnConfirmar);

        tvCantidadPersonas.setText(String.valueOf(cantidad));

        btnCantidadMas.setOnClickListener(v -> {
            int spots = getAvailableSpotsForSelection();
            if (spots >= 0) {
                if (cantidad < spots) {
                    cantidad++;
                    tvCantidadPersonas.setText(String.valueOf(cantidad));
                } else {
                    Toast.makeText(requireContext(), "No hay más cupos disponibles", Toast.LENGTH_SHORT).show();
                }
            } else {
                cantidad++;
                tvCantidadPersonas.setText(String.valueOf(cantidad));
            }
        });

        btnCantidadMenos.setOnClickListener(v -> {
            if (cantidad > 1) {
                cantidad--;
                tvCantidadPersonas.setText(String.valueOf(cantidad));
            }else{
                Toast.makeText(requireContext(), "No puede solicitar menos cupos", Toast.LENGTH_SHORT).show();
            }
        });

        btnConfirmar.setOnClickListener(v -> {
            String fechaISO = reconstructDateToTimesFromSpinner();
            if (fechaISO == null || cantidad <= 0) {
                Toast.makeText(requireContext(), "Por favor seleccione fecha, horario y cantidad válidos", Toast.LENGTH_SHORT).show();
                return;
            }
            ReservationRequest req = new ReservationRequest(activityId, fechaISO, cantidad);


            userApi.createReservation(req).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(requireContext(), "Reserva guardada", Toast.LENGTH_SHORT).show();
                        refreshOfflineCache();

                        NavController navController = Navigation.findNavController(requireView());
                        navController.navigate(R.id.home_nav_graph, null,
                            new NavOptions.Builder()
                                .setPopUpTo(R.id.reservation_form_nav_graph, true)
                                .build()
                        );
                    } else {
                        int code = response.code();
                        Log.e("ReservationForm", "Error HTTP " + code + ": " + response.message());
                        try {
                            if (response.errorBody() != null)
                                Log.e("ReservationForm", "Body: " + response.errorBody().string());
                        } catch (Exception ignored) {}
                        Toast.makeText(requireContext(), "Error al guardar la reserva. Intente nuevamente.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    if (!isAdded()) return;
                    Log.e("ReservationForm", "onFailure: " + t.getMessage(), t);
                    Toast.makeText(requireContext(), "Error al guardar la reserva. Intente nuevamente.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        sDate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDate = (String) parent.getItemAtPosition(position);
                updateTimeSpinner(selectedDate);
                updateAvailableSpots();
                // ajustar cantidad si supera los cupos de la fecha seleccionada
                int spots = getAvailableSpotsForSelection();
                if (spots >= 0 && cantidad > spots) {
                    cantidad = spots;
                    tvCantidadPersonas.setText(String.valueOf(cantidad));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        sTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateAvailableSpots();
                int spots = getAvailableSpotsForSelection();
                if (spots >= 0 && cantidad > spots) {
                    cantidad = spots;
                    tvCantidadPersonas.setText(String.valueOf(cantidad));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadActivity(String activityId) {
        activityApi.getActivityById(activityId).enqueue(new Callback<ApiResponse<Activity>>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(Call<ApiResponse<Activity>> call, Response<ApiResponse<Activity>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    Activity activity = response.body().getData();

                    tvTitulo.setText("Reservar " + activity.getName());

                    List<Schedule> schedules = activity.getSchedules();
                    List<String> rawDates = activity.getDate();
                    // fallback global available spots
                    fallbackAvailableSpots = activity.getAvailableSpots();
                    if (rawDates == null) rawDates = new ArrayList<>();
                    parseDatesAndPopulate(schedules, rawDates);
                } else {
                    showUnknownError();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Activity>> call, Throwable t) {
                if (!isAdded()) return;
                showUnknownError();
            }
        });
    }

    private void showUnknownError() {
        tvTitulo.setText(R.string.error_generic);
        Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
        populateDateSpinner(new ArrayList<>());
    }

    private void parseDatesAndPopulate(List<Schedule> schedules, List<String> rawDates) {
        dateToScheduleOptions.clear();

        if (schedules != null && !schedules.isEmpty()) {
            for (Schedule schedule : schedules) {
                if (schedule == null || schedule.getDate() == null) {
                    continue;
                }

                if (!DateTimeUtils.isFutureOrNow(schedule.getDate())) {
                    continue;
                }

                String key = DateTimeUtils.extractDateKey(schedule.getDate());
                String timePart = DateTimeUtils.extractTimePart(schedule.getDate());

                int adjustedSpots = schedule.getAvailableSpots();

                dateToScheduleOptions.computeIfAbsent(key, k -> new ArrayList<>());
                List<ScheduleOption> options = dateToScheduleOptions.get(key);
                boolean exists = false;
                for (ScheduleOption option : options) {
                    if (option.displayTime.equals(timePart)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    options.add(new ScheduleOption(timePart, schedule.getDate(), adjustedSpots));
                }
            }

            List<String> fechas = new ArrayList<>(dateToScheduleOptions.keySet());
            populateDateSpinner(fechas);
            return;
        }

        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Pattern timePattern = Pattern.compile("(\\d{1,2}:\\d{2})");
        Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");

        for (String raw : rawDates) {
            if (raw == null) continue;

            if (!DateTimeUtils.isFutureOrNow(raw)) {
                continue;
            }

            // fecha como llave (fallback a raw completo si no hay yyyy-mm-dd)
            String key = raw;
            Matcher m = datePattern.matcher(raw);
            if (m.find()) {
                key = m.group(1);
            }

            // extraer horario HH:MM si existe
            String timePart = "--";
            Matcher tm = timePattern.matcher(raw);
            if (tm.find()) timePart = tm.group(1);

            // buscar primer número después de la fecha que pueda representar cupos
            Integer spots = null;
            int end = raw.indexOf(key) + key.length();
            if (end > 0 && end < raw.length()) {
                String after = raw.substring(end);
                Matcher num = numberPattern.matcher(after);
                if (num.find()) {
                    try { spots = Integer.parseInt(num.group(1)); } catch (NumberFormatException ignored) {}
                }
            } else {
                // si no hay yyyy-mm-dd o no hay contenido después, intentar buscar número global en el raw
                Matcher num = numberPattern.matcher(raw);
                if (num.find()) {
                    try { spots = Integer.parseInt(num.group(1)); } catch (NumberFormatException ignored) {}
                }
            }

            int availableSpots = spots != null ? spots : fallbackAvailableSpots;
            dateToScheduleOptions.computeIfAbsent(key, k -> new ArrayList<>());
            dateToScheduleOptions.get(key).add(new ScheduleOption(timePart, raw, availableSpots));
        }

        // Si no hay fechas parseadas, usar rawDates como fechas (fallback)
        if (dateToScheduleOptions.isEmpty() && !rawDates.isEmpty()) {
            for (String r : rawDates) {
                dateToScheduleOptions.put(r, new ArrayList<ScheduleOption>() {{ add(new ScheduleOption("--", r, fallbackAvailableSpots)); }});
            }
        }

        List<String> fechas = dateToScheduleOptions.keySet().stream().sorted().collect(Collectors.toList());
        populateDateSpinner(fechas);
    }

    private void populateDateSpinner(List<String> fechas) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, fechas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sDate.setAdapter(adapter);

        // Pre-select first and update times
        if (!fechas.isEmpty()) {
            sDate.setSelection(0);
            updateTimeSpinner(fechas.get(0));
            updateAvailableSpots();
            int spots = getAvailableSpotsForSelection();
            if (spots >= 0 && cantidad > spots) {
                cantidad = spots;
                tvCantidadPersonas.setText(String.valueOf(cantidad));
            }
        } else {
            // vaciar sTime
            ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
            emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sTime.setAdapter(emptyAdapter);
            btnConfirmar.setEnabled(false);
            tvAvailableSpots.setText("No hay fechas disponibles");
        }
    }

    private int getAvailableSpotsForSelection() {
        ScheduleOption selectedOption = getSelectedOption();
        if (selectedOption == null) {
            return fallbackAvailableSpots;
        }
        return selectedOption.availableSpots;
    }

    private void updateTimeSpinner(String selectedDate) {
        List<ScheduleOption> options = dateToScheduleOptions.get(selectedDate);
        List<String> times = new ArrayList<>();
        if (options != null) {
            for (ScheduleOption option : options) {
                times.add(option.displayTime);
            }
        }
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, times);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sTime.setAdapter(timeAdapter);
        btnConfirmar.setEnabled(!times.isEmpty());
    }

    private void updateAvailableSpots() {
        int spots = getAvailableSpotsForSelection();
        if (spots >= 0) {
            tvAvailableSpots.setText(getString(R.string.detail_spots, spots));
        } else {
            tvAvailableSpots.setText("");
        }
    }

    private String reconstructDateToTimesFromSpinner() {
        String dateISO;

        ScheduleOption selectedOption = getSelectedOption();
        if (selectedOption == null) {
            dateISO = null;
        } else if ("--".equals(selectedOption.displayTime)) {
            dateISO = null;
        } else {
            dateISO = selectedOption.selectedDate;
        }

        return dateISO;
    }

    private ScheduleOption getSelectedOption() {
        String selectedDate = sDate.getSelectedItem() != null ? (String) sDate.getSelectedItem() : null;
        String selectedTime = sTime.getSelectedItem() != null ? (String) sTime.getSelectedItem() : null;

        if (selectedDate == null || selectedTime == null) {
            return null;
        }

        List<ScheduleOption> options = dateToScheduleOptions.get(selectedDate);
        if (options == null) {
            return null;
        }

        for (ScheduleOption option : options) {
            if (selectedTime.equals(option.displayTime)) {
                return option;
            }
        }

        return null;
    }

    private void refreshOfflineCache() {
        bookingsApi.getOfflineBundle().enqueue(new Callback<ApiResponse<OfflineBundle>>() {
            @Override
            public void onResponse(Call<ApiResponse<OfflineBundle>> call,
                                   Response<ApiResponse<OfflineBundle>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess() && response.body().getData() != null) {
                    offlineBookingCache.save(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<OfflineBundle>> call, Throwable t) {
                // cache refresh is best-effort
            }
        });
    }
}