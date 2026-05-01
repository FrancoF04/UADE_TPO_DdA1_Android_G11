package com.example.androidapp.util;

import androidx.biometric.BiometricManager;

/**
 * Mapea el codigo retornado por BiometricManager.canAuthenticate(int authenticators)
 * a un BiometricStatus consumible por la UI.
 */
public final class BiometricCanAuthMapper {

    private BiometricCanAuthMapper() {}

    public static BiometricStatus map(int code) {
        switch (code) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return BiometricStatus.AVAILABLE;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return BiometricStatus.NOT_ENROLLED;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return BiometricStatus.NO_HARDWARE;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
            default:
                return BiometricStatus.UNAVAILABLE;
        }
    }
}
