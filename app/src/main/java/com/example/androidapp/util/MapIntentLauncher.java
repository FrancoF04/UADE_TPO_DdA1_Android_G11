package com.example.androidapp.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class MapIntentLauncher {

    @Inject
    public MapIntentLauncher() {}

    public void openMap(Context context, double lat, double lng, String address) {
        String addressEncoded = address != null ? Uri.encode(address) : "";
        Uri uri = Uri.parse("geo:" + lat + "," + lng + "?q=" + addressEncoded);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "No tenés un app de mapas instalado", Toast.LENGTH_SHORT).show();
        }
    }
}
