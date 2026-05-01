package com.example.androidapp.util;

/**
 * Implementado por componentes UI que necesitan reaccionar a una sesion vencida
 * (refresh token invalido o ausente). El AuthRefreshInterceptor dispara la notificacion
 * a traves de SessionEventBus cuando detecta que ya no se puede recuperar la sesion.
 */
public interface SessionExpiredListener {
    void onSessionExpired();
}
