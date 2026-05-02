package com.example.androidapp.ui.historial;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidapp.R;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.HistorialItem;
import com.example.androidapp.data.remote.ActivityApi;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class HistorialFragment extends Fragment {

    private static final String[] DESTINATIONS = {
            "Todos", "Buenos Aires", "Bariloche", "Mendoza", "Ushuaia", "Córdoba", "Salta"
    };

    @Inject
    ActivityApi activityApi;

    private HistorialAdapter adapter;
    private List<HistorialItem> allItems = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Spinner spinnerDestino;
    private Button btnDesde;
    private Button btnHasta;

    private LocalDate filterDesde = null;
    private LocalDate filterHasta = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        cargarHistorial();
    }

    private void initViews(View view) {
        progressBar = view.findViewById(R.id.progressBarHistorial);
        tvEmpty = view.findViewById(R.id.tvHistorialEmpty);
        spinnerDestino = view.findViewById(R.id.spinnerDestino);
        btnDesde = view.findViewById(R.id.btnDesde);
        btnHasta = view.findViewById(R.id.btnHasta);
        ListView lvHistorial = view.findViewById(R.id.lvHistorial);

        ArrayAdapter<String> destinosAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, DESTINATIONS);
        destinosAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDestino.setAdapter(destinosAdapter);

        adapter = new HistorialAdapter(requireContext());
        adapter.setOnRatingClickListener(item -> {
            Bundle args = new Bundle();
            args.putString("bookingId", item.getBookingId());
            args.putString("activityName", item.getActivityName());
            args.putString("activityDate", item.getSelectedDate());
            Navigation.findNavController(requireView()).navigate(R.id.action_historial_to_rating, args);
        });
        lvHistorial.setAdapter(adapter);

        lvHistorial.setOnItemClickListener((parent, v, position, id) -> {
            HistorialItem item = adapter.getItem(position);
            Bundle args = new Bundle();
            args.putString("activityId", item.getActivityId());
            args.putBoolean("showReserveButton", false);
            args.putBoolean("showSpotsField", false);
            Navigation.findNavController(requireView()).navigate(R.id.action_historial_to_detail, args);
        });

        btnDesde.setOnClickListener(v -> mostrarDatePicker(true));
        btnHasta.setOnClickListener(v -> mostrarDatePicker(false));

        view.findViewById(R.id.btnAplicarFiltro).setOnClickListener(v -> aplicarFiltros());
        view.findViewById(R.id.btnLimpiarFiltro).setOnClickListener(v -> limpiarFiltros());
    }

    private void mostrarDatePicker(boolean esDesde) {
        Calendar calendar = Calendar.getInstance();
        int anio = calendar.get(Calendar.YEAR);
        int mes = calendar.get(Calendar.MONTH);
        int dia = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(requireContext(), (datePicker, y, m, d) -> {
            LocalDate fecha = LocalDate.of(y, m + 1, d);
            String label = String.format("%02d/%02d/%04d", d, m + 1, y);
            if (esDesde) {
                filterDesde = fecha;
                btnDesde.setText(getString(R.string.historial_desde_con_fecha, label));
            } else {
                filterHasta = fecha;
                btnHasta.setText(getString(R.string.historial_hasta_con_fecha, label));
            }
        }, anio, mes, dia).show();
    }

    private void aplicarFiltros() {
        String destinoSeleccionado = spinnerDestino.getSelectedItem().toString();

        List<HistorialItem> filtrados = allItems.stream()
                .filter(item -> {
                    if (!destinoSeleccionado.equals("Todos")
                            && !destinoSeleccionado.equals(item.getDestination())) {
                        return false;
                    }
                    if (filterDesde != null || filterHasta != null) {
                        try {
                            LocalDate fechaItem = Instant.parse(item.getSelectedDate())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                            if (filterDesde != null && fechaItem.isBefore(filterDesde)) return false;
                            if (filterHasta != null && fechaItem.isAfter(filterHasta)) return false;
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        adapter.setItems(filtrados);
        tvEmpty.setVisibility(filtrados.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void limpiarFiltros() {
        filterDesde = null;
        filterHasta = null;
        spinnerDestino.setSelection(0);
        btnDesde.setText(R.string.historial_desde);
        btnHasta.setText(R.string.historial_hasta);
        adapter.setItems(allItems);
        tvEmpty.setVisibility(allItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void enrichWithImages(List<HistorialItem> items) {
        if (items == null || items.isEmpty()) return;
        AtomicInteger pending = new AtomicInteger(items.size());
        for (HistorialItem item : items) {
            activityApi.getActivityById(item.getActivityId()).enqueue(new retrofit2.Callback<ApiResponse<Activity>>() {
                @Override
                public void onResponse(retrofit2.Call<ApiResponse<Activity>> call, retrofit2.Response<ApiResponse<Activity>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Activity activity = response.body().getData();
                        if (activity != null && activity.getImageUrl() != null) {
                            item.setImageUrl(activity.getImageUrl());
                        }
                    }
                    if (pending.decrementAndGet() == 0 && isAdded()) {
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<ApiResponse<Activity>> call, Throwable t) {
                    if (pending.decrementAndGet() == 0 && isAdded()) {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    private void cargarHistorial() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);


        activityApi.getHistory(1, 100).enqueue(new Callback<ApiResponse<List<HistorialItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<HistorialItem>>> call,
                                   Response<ApiResponse<List<HistorialItem>>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<HistorialItem> data = response.body().getData();
                    allItems = data != null ? data : new ArrayList<>();
                } else {
                    allItems = new ArrayList<>();
                }

                adapter.setItems(allItems);
                tvEmpty.setVisibility(allItems.isEmpty() ? View.VISIBLE : View.GONE);
                enrichWithImages(allItems);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<HistorialItem>>> call, Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                allItems = new ArrayList<>();
                adapter.setItems(allItems);
                tvEmpty.setText(R.string.error_network);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}
