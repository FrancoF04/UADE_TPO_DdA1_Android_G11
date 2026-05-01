package com.example.androidapp.ui.auth;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.example.androidapp.data.local.TokenManager;

public final class BiometricOptInDialog {

    public interface OnActivate { void onActivate(); }

    private BiometricOptInDialog() {}

    public static void show(Context context, TokenManager tokenManager, OnActivate onActivate) {
        new AlertDialog.Builder(context)
                .setTitle("¿Activar ingreso con huella?")
                .setMessage("La próxima vez que abras la app podés ingresar con tu huella en lugar de escribir usuario y contraseña.")
                .setPositiveButton("Activar", (dialog, which) -> {
                    tokenManager.setBiometricEnabled(true);
                    if (onActivate != null) onActivate.onActivate();
                })
                .setNegativeButton("Ahora no", (dialog, which) -> {
                    tokenManager.setBiometricOptInDismissed(true);
                })
                .setCancelable(false)
                .show();
    }
}
