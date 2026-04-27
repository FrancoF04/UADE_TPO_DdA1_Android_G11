package com.example.androidapp.ui.reservation;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.androidapp.R;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.Schedule;
import com.example.androidapp.data.remote.ActivityApi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.hilt.android.AndroidEntryPoint;
import jakarta.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class ReservationFormFragment extends Fragment {
    @Inject
    ActivityApi activityApi;

    private ImageButton btnBack;
    private TextView tvTitulo;
    private Spinner sDate;
    private Spinner sTime;
    private TextView tvAvailableSpots;
    private int cantidad = 1;
    private TextView tvCantidadPersonas;
    private Button btnCantidadMas;
    private Button btnCantidadMenos;
    private Button btnConfirmar;

    // Map fecha -> lista de horarios (hora formato HH:mm o similar)
    private Map<String, List<String>> dateToTimes = new LinkedHashMap<>();
    // Map fecha -> cupos disponibles por schedule. Si no existe la fecha en el map, usar fallbackAvailableSpots
    private Map<String, Integer> dateToAvailableSpots = new LinkedHashMap<>();
    private int fallbackAvailableSpots = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reservation_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        // Leer activityId pasado por Navigation
        String activityId = getArguments() != null
                ? getArguments().getString("activityId", "")
                : "";

        if (!activityId.isEmpty()) {
            loadActivity(activityId);
        }else {
            showUnknownError();
        }
    }

    private void initViews(@NonNull View view) {
        btnBack = view.findViewById(R.id.btnBack);
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
            String sel = null;
            if (sDate.getSelectedItem() != null) sel = (String) sDate.getSelectedItem();
            int spots = getAvailableSpotsForDate(sel);
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
            // Aquí podrías construir el body de la reserva y llamar al endpoint correspondiente
        });

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        sDate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDate = (String) parent.getItemAtPosition(position);
                updateTimeSpinner(selectedDate);
                updateAvailableSpots(selectedDate);
                // ajustar cantidad si supera los cupos de la fecha seleccionada
                int spots = getAvailableSpotsForDate(selectedDate);
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
        dateToTimes.clear();
        dateToAvailableSpots.clear();

        if (schedules != null && !schedules.isEmpty()) {
            for (Schedule schedule : schedules) {
                if (schedule == null || schedule.getDate() == null) {
                    continue;
                }

                String key = extractDateKey(schedule.getDate());
                String timePart = extractTimePart(schedule.getDate());

                dateToAvailableSpots.put(key, schedule.getAvailableSpots());
                dateToTimes.computeIfAbsent(key, k -> new ArrayList<>());
                List<String> times = dateToTimes.get(key);
                if (!times.contains(timePart)) {
                    times.add(timePart);
                }
            }

            List<String> fechas = new ArrayList<>(dateToTimes.keySet());
            populateDateSpinner(fechas);
            return;
        }

        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Pattern timePattern = Pattern.compile("(\\d{1,2}:\\d{2})");
        Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");

        for (String raw : rawDates) {
            if (raw == null) continue;

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

            if (spots != null) dateToAvailableSpots.put(key, spots);

            // añadir horario al map
            dateToTimes.computeIfAbsent(key, k -> new ArrayList<>());
            List<String> times = dateToTimes.get(key);
            if (!times.contains(timePart)) times.add(timePart);
        }

        // Si no hay fechas parseadas, usar rawDates como fechas (fallback)
        if (dateToTimes.isEmpty() && !rawDates.isEmpty()) {
            for (String r : rawDates) {
                dateToTimes.put(r, new ArrayList<String>() {{ add("--"); }});
            }
        }

        List<String> fechas = new ArrayList<>(dateToTimes.keySet());
        populateDateSpinner(fechas);
    }

    private String extractDateKey(String rawDate) {
        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Matcher matcher = datePattern.matcher(rawDate);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return rawDate;
    }

    private String extractTimePart(String rawDate) {
        Pattern timePattern = Pattern.compile("(\\d{1,2}:\\d{2})");
        Matcher matcher = timePattern.matcher(rawDate);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "--";
    }

    private void populateDateSpinner(List<String> fechas) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, fechas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sDate.setAdapter(adapter);

        // Pre-select first and update times
        if (!fechas.isEmpty()) {
            sDate.setSelection(0);
            updateTimeSpinner(fechas.get(0));
            updateAvailableSpots(fechas.get(0));
            int spots = getAvailableSpotsForDate(fechas.get(0));
            if (spots >= 0 && cantidad > spots) {
                cantidad = spots;
                tvCantidadPersonas.setText(String.valueOf(cantidad));
            }
        } else {
            // vaciar sTime
            ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, new ArrayList<>());
            emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sTime.setAdapter(emptyAdapter);
        }
    }

    private int getAvailableSpotsForDate(String dateKey) {
        if (dateKey == null) return fallbackAvailableSpots;
        return dateToAvailableSpots.getOrDefault(dateKey, fallbackAvailableSpots);
    }

    private void updateTimeSpinner(String selectedDate) {
        List<String> times = dateToTimes.get(selectedDate);
        if (times == null) times = new ArrayList<>();
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, times);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sTime.setAdapter(timeAdapter);
    }

    private void updateAvailableSpots(String selectedDate) {
        int spots = getAvailableSpotsForDate(selectedDate);
        if (spots >= 0) {
            tvAvailableSpots.setText(getString(R.string.detail_spots, spots));
        } else {
            tvAvailableSpots.setText("");
        }
    }
}