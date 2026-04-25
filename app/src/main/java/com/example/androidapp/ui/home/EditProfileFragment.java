package com.example.androidapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidapp.R;

public class EditProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView ivFotoPerfil = view.findViewById(R.id.ivFotoPerfil);
        EditText etNombre = view.findViewById(R.id.etNombre);
        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etTelefono = view.findViewById(R.id.etTelefono);
        EditText btnGuardar = view.findViewById(R.id.btnGuardar);


        //guardar los cambios en la base de datos y volver a perfil de solo visulizacion
        btnGuardar.setOnClickListener(v -> {
            Bundle args = new Bundle();
            //pasar los atributos asi no hay que volver a llamar a la api para mostrarlos
            //args.putString("username", username);
            Navigation.findNavController(view).navigate(R.id.action_profile_to_editProfile, args);
        });
    }
}
