package com.example.androidapp.ui.home;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidapp.R;
import com.example.androidapp.data.model.Filters;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FiltersFragment extends Fragment {

    public static final String RESULT_KEY = "filters_result";
    public static final String ARG_FILTERS = "filters_initial";

    private static final List<String> DESTINATIONS = Arrays.asList(
            "Buenos Aires", "Bariloche", "Mendoza", "Ushuaia", "Córdoba", "Salta"
    );
    private static final List<String> CATEGORIES = Arrays.asList(
            "free_tour", "guided_visit", "excursion", "gastronomic", "adventure"
    );

    private Filters current = new Filters();

    private LinearLayout containerDestinations;
    private LinearLayout containerCategories;
    private Button btnBack;
    private Button btnClear;
    private Button btnDate;
    private Button btnApply;
    private EditText etPriceMin;
    private EditText etPriceMax;
    private TextView tvPriceLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        containerDestinations = view.findViewById(R.id.containerDestinations);
        containerCategories = view.findViewById(R.id.containerCategories);
        btnBack = view.findViewById(R.id.btnBack);
        btnClear = view.findViewById(R.id.btnClear);
        btnDate = view.findViewById(R.id.btnDate);
        btnApply = view.findViewById(R.id.btnApply);
        etPriceMin = view.findViewById(R.id.etPriceMin);
        etPriceMax = view.findViewById(R.id.etPriceMax);
        tvPriceLabel = view.findViewById(R.id.tvPriceLabel);

        Filters initial = getArguments() != null ? getArguments().getParcelable(ARG_FILTERS) : null;
        if (initial != null) current = initial;

        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        btnClear.setOnClickListener(v -> {
            current.reset();
            populatePills();
            updateDateButton();
            updatePriceLabel();
            etPriceMin.setText("");
            etPriceMax.setText("");
        });

        populatePills();
        updateDateButton();
        updatePriceLabel();

        if (current.priceMin != null) etPriceMin.setText(String.valueOf(current.priceMin));
        if (current.priceMax != null) etPriceMax.setText(String.valueOf(current.priceMax));

        btnDate.setOnClickListener(v -> showDatePicker());

        TextWatcher priceWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                current.priceMin = parseInt(etPriceMin.getText().toString());
                current.priceMax = parseInt(etPriceMax.getText().toString());
                updatePriceLabel();
            }
        };
        etPriceMin.addTextChangedListener(priceWatcher);
        etPriceMax.addTextChangedListener(priceWatcher);

        btnApply.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putParcelable(RESULT_KEY, current);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            NavHostFragment.findNavController(this).popBackStack();
        });
    }

    private Integer parseInt(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private void populatePills() {
        containerDestinations.removeAllViews();
        for (String d : DESTINATIONS) {
            addPill(containerDestinations, d, d.equals(current.destination), v -> {
                current.destination = d.equals(current.destination) ? null : d;
                populatePills();
            });
        }

        containerCategories.removeAllViews();
        for (String c : CATEGORIES) {
            addPill(containerCategories, prettyCategory(c), c.equals(current.category), v -> {
                current.category = c.equals(current.category) ? null : c;
                populatePills();
            });
        }
    }

    private void addPill(LinearLayout container, String label, boolean selected, View.OnClickListener onClick) {
        Button btn = new Button(requireContext());
        btn.setText(label);
        btn.setBackgroundResource(selected ? R.drawable.pill_background_selected : R.drawable.pill_background);
        btn.setTextColor(selected ? 0xFF2563EB : 0xFF334155);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin);
        btn.setLayoutParams(params);
        btn.setOnClickListener(onClick);
        container.addView(btn);
    }

    private String prettyCategory(String key) {
        switch (key) {
            case "free_tour": return "Free Tour";
            case "guided_visit": return "Visita Guiada";
            case "excursion": return "Excursión";
            case "gastronomic": return "Gastronómica";
            case "adventure": return "Aventura";
            default: return key;
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, day) -> {
                    current.date = String.format("%04d-%02d-%02d", year, month + 1, day);
                    updateDateButton();
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void updateDateButton() {
        btnDate.setText(current.date != null ? current.date : "Cualquier día");
    }

    private void updatePriceLabel() {
        String min = current.priceMin != null ? "$" + current.priceMin : "Sin mínimo";
        String max = current.priceMax != null ? "$" + current.priceMax : "Sin máximo";
        tvPriceLabel.setText(min + " — " + max);
    }
}
