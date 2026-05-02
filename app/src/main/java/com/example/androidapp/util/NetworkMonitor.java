package com.example.androidapp.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class NetworkMonitor {

    public interface OnNetworkChangeListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    private final ConnectivityManager connectivityManager;
    private final List<OnNetworkChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean connected;

    @Inject
    public NetworkMonitor(@ApplicationContext Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connected = checkConnected();

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (!connected) {
                    connected = true;
                    mainHandler.post(() -> {
                        for (OnNetworkChangeListener l : listeners) {
                            try { l.onNetworkAvailable(); } catch (Throwable ignored) {}
                        }
                    });
                }
            }

            @Override
            public void onLost(Network network) {
                if (connected && !checkConnected()) {
                    connected = false;
                    mainHandler.post(() -> {
                        for (OnNetworkChangeListener l : listeners) {
                            try { l.onNetworkLost(); } catch (Throwable ignored) {}
                        }
                    });
                }
            }
        });
    }

    public boolean isConnected() {
        return connected;
    }

    public void register(OnNetworkChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregister(OnNetworkChangeListener listener) {
        listeners.remove(listener);
    }

    private boolean checkConnected() {
        if (connectivityManager == null) return true;
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
