package com.example.androidapp.util;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.NotificationItem;
import com.example.androidapp.data.remote.NotificationsApi;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Response;

@AndroidEntryPoint
public class NotificationPollingService extends Service {

    private static final String TAG = "NotificationPolling";

    @Inject NotificationsApi notificationsApi;
    @Inject TokenManager tokenManager;
    @Inject NetworkMonitor networkMonitor;

    private final AtomicBoolean polling = new AtomicBoolean(false);
    private Thread pollThread;
    private volatile Call<ApiResponse<List<NotificationItem>>> currentCall;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, NotificationHelper.buildServiceStatusNotification(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (polling.compareAndSet(false, true)) {
            pollThread = new Thread(this::pollLoop, "notification-polling");
            pollThread.start();
        }
        return START_STICKY;
    }

    private void pollLoop() {
        while (polling.get()) {
            if (!tokenManager.isRefreshTokenValid()) {
                stopSelf();
                return;
            }

            try {
                Call<ApiResponse<List<NotificationItem>>> call = notificationsApi.poll();
                currentCall = call;
                Response<ApiResponse<List<NotificationItem>>> response = call.execute();

                if (response.code() == 200 && response.body() != null && response.body().isSuccess()) {
                    List<NotificationItem> events = response.body().getData();
                    if (events != null) {
                        for (NotificationItem event : events) {
                            NotificationHelper.showReminderNotification(this, event);
                        }
                    }
                }
                // 204 -> sin novedades, el server ya retuvo la respuesta ~25s; reintentar enseguida
            } catch (IOException e) {
                Log.e(TAG, "Error de red en long polling", e);
                waitBeforeRetry();
            }
        }
    }

    private void waitBeforeRetry() {
        long backoffMs = networkMonitor.isConnected() ? 3000 : 10000;
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        polling.set(false);
        if (currentCall != null) {
            currentCall.cancel();
        }
        if (pollThread != null) {
            pollThread.interrupt();
        }
        super.onDestroy();
    }

    // Android 15+ impone un límite de tiempo acumulado a los FGS dataSync; frenamos con gracia en vez de crashear
    @RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Override
    public void onTimeout(int startId, int fgsType) {
        stopSelf();
    }
}
