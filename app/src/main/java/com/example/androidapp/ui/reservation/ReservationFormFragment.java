package com.example.androidapp.ui.reservation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.androidapp.R;

import java.util.List;

public class ReservationFormFragment extends Fragment {
    private ImageButton btnBack;
    private EditText etTitulo;
    private List<String> fechasDisponibles;
    private Spinner sDate;
    private List<String> horariosDisponibles;
    private Spinner sTime;
    private int cantidad = 1; // Cantidad inicial de personas

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reservation_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnBack = view.findViewById(R.id.btnBack);
        etTitulo = view.findViewById(R.id.etTitulo);

    }
}
