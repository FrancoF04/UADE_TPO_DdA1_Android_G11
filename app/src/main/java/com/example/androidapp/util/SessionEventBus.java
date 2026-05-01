package com.example.androidapp.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Bus interno para SESSION_EXPIRED. El AuthRefreshInterceptor llama
 * notifySessionExpired() cuando el refresh token esta invalido. Los
 * listeners registrados (tipicamente MainActivity en onResume) reciben
 * el callback en el thread donde se disparo (los receptores deben usar
 * runOnUiThread si tocan UI).
 */
@Singleton
public class SessionEventBus {

    private final List<SessionExpiredListener> listeners = new CopyOnWriteArrayList<>();

    @Inject
    public SessionEventBus() {
        // Hilt-managed
    }

    public void register(SessionExpiredListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregister(SessionExpiredListener listener) {
        listeners.remove(listener);
    }

    public void notifySessionExpired() {
        for (SessionExpiredListener listener : listeners) {
            try {
                listener.onSessionExpired();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
