package com.example.androidapp.util;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.androidapp.MainActivity;
import com.example.androidapp.R;
import com.example.androidapp.data.model.NotificationItem;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class NotificationHelper {

    public static final String CHANNEL_REMINDERS = "reminders_channel";
    public static final String CHANNEL_SERVICE_STATUS = "service_status_channel";
    public static final String CHANNEL_ALERTS = "alerts_channel";
    public static final int SERVICE_NOTIFICATION_ID = 1001;

    private NotificationHelper() {
    }

    public static void createChannels(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel reminders = new NotificationChannel(
                CHANNEL_REMINDERS, "Recordatorios de actividades", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationChannel serviceStatus = new NotificationChannel(
                CHANNEL_SERVICE_STATUS, "Estado de sincronización", NotificationManager.IMPORTANCE_MIN);
        NotificationChannel alerts = new NotificationChannel(
                CHANNEL_ALERTS, "Avisos de reservas", NotificationManager.IMPORTANCE_HIGH);

        manager.createNotificationChannel(reminders);
        manager.createNotificationChannel(serviceStatus);
        manager.createNotificationChannel(alerts);
    }

    public static Notification buildServiceStatusNotification(Context context) {
        return new NotificationCompat.Builder(context, CHANNEL_SERVICE_STATUS)
                .setContentTitle("XploreNow")
                .setContentText("Buscando novedades de tus actividades…")
                .setSmallIcon(R.drawable.ic_reservas)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    public static void showReminderNotification(Context context, NotificationItem event) {
        if (Build.VERSION.SDK_INT >= 33
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        int notificationId = event.getBookingId().hashCode();

        Intent actionIntent = new Intent(context, NotificationActionReceiver.class);
        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(
                context, notificationId, actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent contentIntent = new Intent(context, MainActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context, notificationId, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setContentTitle("Tu actividad es en menos de 24hs")
                .setContentText(event.getActivityName() + " — " + formatFriendlyDate(event.getSelectedDate()))
                .setSmallIcon(R.drawable.ic_reservas)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .addAction(0, "Ver Voucher", actionPendingIntent)
                .build();

        NotificationManagerCompat.from(context).notify(notificationId, notification);
    }

    // Feature 12.30: aviso inmediato cuando la operadora cancela o reprograma una actividad.
    public static void showBookingChangeNotification(Context context, String activityName,
                                                     boolean cancelled, String newDateIso,
                                                     String bookingId) {
        if (Build.VERSION.SDK_INT >= 33
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        int notificationId = ("change-" + bookingId).hashCode();

        Intent contentIntent = new Intent(context, MainActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context, notificationId, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String name = (activityName != null && !activityName.isEmpty())
                ? activityName : "Una de tus actividades";
        String title = cancelled ? "Tu actividad fue cancelada" : "Tu actividad fue reprogramada";
        String text = cancelled
                ? name + " fue cancelada por la operadora"
                : name + " se reprogramó para el " + formatFriendlyDate(newDateIso);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setSmallIcon(R.drawable.ic_reservas)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManagerCompat.from(context).notify(notificationId, notification);
    }

    private static String formatFriendlyDate(String iso) {
        try {
            Instant instant = Instant.parse(iso);
            return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(instant);
        } catch (Exception e) {
            return iso;
        }
    }
}
