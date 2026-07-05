package com.example.androidapp.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

// Target del botón "Ver Voucher" — la pantalla de voucher (Feature 11) todavía no existe
public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context.getApplicationContext(), "Disponible próximamente", Toast.LENGTH_SHORT).show();
    }
}
