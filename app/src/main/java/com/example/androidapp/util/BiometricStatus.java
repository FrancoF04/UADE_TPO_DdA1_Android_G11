package com.example.androidapp.util;

/**
 * Resultado consolidado de BiometricManager.canAuthenticate().
 * - AVAILABLE: hardware presente y al menos una credencial enrolada.
 * - NOT_ENROLLED: hardware presente pero el usuario no enrolo nada todavia.
 * - NO_HARDWARE: dispositivo sin hardware biometrico.
 * - UNAVAILABLE: hardware temporalmente inaccesible o requiere update de seguridad.
 */
public enum BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}
