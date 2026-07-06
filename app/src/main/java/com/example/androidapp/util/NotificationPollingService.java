package com.example.androidapp.util;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.androidapp.data.local.OfflineBookingCache;
import com.example.androidapp.data.local.SyncCursorStore;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.BookingChange;
import com.example.androidapp.data.model.NotificationItem;
import com.example.androidapp.data.model.NotificationPollResponse;
import com.example.androidapp.data.model.OfflineBundle;
import com.example.androidapp.data.model.Reservation;
import com.example.androidapp.data.model.SyncPollResponse;
import com.example.androidapp.data.remote.BookingsApi;
import com.example.androidapp.data.remote.NotificationsApi;
import com.example.androidapp.data.remote.SyncApi;
import com.example.androidapp.data.remote.UserApi;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Response;

@AndroidEntryPoint
public class NotificationPollingService extends Service {

    private static final String TAG = "NotificationPolling";

    @Inject NotificationsApi notificationsApi;
    @Inject SyncApi syncApi;
    @Inject UserApi userApi;
    @Inject BookingsApi bookingsApi;
    @Inject OfflineBookingCache offlineBookingCache;
    @Inject SyncCursorStore cursorStore;
    @Inject TokenManager tokenManager;
    @Inject NetworkMonitor networkMonitor;

    private final AtomicBoolean polling = new AtomicBoolean(false);
    private Thread pollThread;
    private Thread syncThread;
    private volatile Call<ApiResponse<NotificationPollResponse>> currentCall;
    private volatile Call<ApiResponse<SyncPollResponse>> currentSyncCall;

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
            syncThread = new Thread(this::syncPollLoop, "booking-sync-polling");
            syncThread.start();
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
                Call<ApiResponse<NotificationPollResponse>> call = notificationsApi.poll();
                currentCall = call;
                Response<ApiResponse<NotificationPollResponse>> response = call.execute();

                if (response.code() == 200 && response.body() != null && response.body().isSuccess()
                        && response.body().getData() != null) {
                    List<NotificationItem> events = response.body().getData().getEvents();
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

    // Feature 12.30: segundo stream de long-polling contra /bookings/sync/poll para avisar
    // de cancelaciones/reprogramaciones de la operadora sobre las reservas del usuario.
    private void syncPollLoop() {
        while (polling.get()) {
            if (!tokenManager.isRefreshTokenValid()) {
                stopSelf();
                return;
            }

            try {
                Map<String, Reservation> mine = fetchMyBookings();

                Call<ApiResponse<SyncPollResponse>> call = syncApi.syncPoll(cursorStore.getSince());
                currentSyncCall = call;
                Response<ApiResponse<SyncPollResponse>> response = call.execute();

                if (response.code() == 200 && response.body() != null && response.body().isSuccess()) {
                    SyncPollResponse data = response.body().getData();
                    if (data != null && data.getChanges() != null) {
                        boolean anyMine = false;
                        for (BookingChange change : data.getChanges()) {
                            Reservation res = mine.get(change.getBookingId());
                            if (res == null) continue; // el endpoint trae cambios de todos; solo las mías

                            boolean cancelled = "cancelled".equals(change.getChangeType());
                            boolean rescheduled = "updated".equals(change.getChangeType());
                            if (!cancelled && !rescheduled) continue; // finalized u otro tipo: ignorar

                            String key = change.getBookingId() + "|" + change.getChangeType()
                                    + "|" + change.getUpdatedAt();
                            if (cursorStore.isProcessed(key)) continue;

                            NotificationHelper.showBookingChangeNotification(
                                    this, res.getActivityName(), cancelled,
                                    change.getSelectedDate(), change.getBookingId());
                            cursorStore.markProcessed(key);
                            anyMine = true;
                        }
                        cursorStore.setSince(data.getServerTime());
                        if (anyMine) {
                            refreshOfflineCache(); // refleja el cambio en Mis Reservas / modo offline
                        }
                    }
                }
                // 204 -> sin novedades; el server ya retuvo la respuesta ~25s, reintentar enseguida
            } catch (IOException e) {
                Log.e(TAG, "Error de red en sync polling", e);
                waitBeforeRetry();
            }
        }
    }

    private Map<String, Reservation> fetchMyBookings() throws IOException {
        Map<String, Reservation> map = new HashMap<>();
        Response<ApiResponse<List<Reservation>>> r = userApi.getReservations().execute();
        if (r.isSuccessful() && r.body() != null && r.body().isSuccess() && r.body().getData() != null) {
            for (Reservation res : r.body().getData()) {
                if (res.getId() != null) {
                    map.put(res.getId(), res);
                }
            }
        }
        return map;
    }

    private void refreshOfflineCache() {
        try {
            Response<ApiResponse<OfflineBundle>> r = bookingsApi.getOfflineBundle().execute();
            if (r.isSuccessful() && r.body() != null && r.body().isSuccess() && r.body().getData() != null) {
                offlineBookingCache.save(r.body().getData());
            }
        } catch (IOException e) {
            Log.w(TAG, "No se pudo refrescar el cache offline", e);
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
        if (currentSyncCall != null) {
            currentSyncCall.cancel();
        }
        if (pollThread != null) {
            pollThread.interrupt();
        }
        if (syncThread != null) {
            syncThread.interrupt();
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
