package com.example.androidapp.util;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class BiometricHelper {

    public interface OnSuccess { void onSuccess(); }
    public interface OnError { void onError(int errorCode, CharSequence errorMessage); }

    private final Context appContext;

    @Inject
    public BiometricHelper(@ApplicationContext Context appContext) {
        this.appContext = appContext;
    }

    public BiometricStatus checkAvailability() {
        BiometricManager manager = BiometricManager.from(appContext);
        int code = manager.canAuthenticate(allowedAuthenticators());
        return BiometricCanAuthMapper.map(code);
    }

    public void promptForAuth(FragmentActivity activity,
                              String title,
                              String subtitle,
                              OnSuccess onSuccess,
                              OnError onError) {
        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt prompt = new BiometricPrompt(
                activity,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        onSuccess.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errorMessage) {
                        onError.onError(errorCode, errorMessage);
                    }
                }
        );

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(allowedAuthenticators())
                .build();

        prompt.authenticate(info);
    }

    public Intent enrollIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
            intent.putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    allowedAuthenticators()
            );
            return intent;
        }
        return new Intent(Settings.ACTION_SECURITY_SETTINGS);
    }

    private static int allowedAuthenticators() {
        return Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL;
    }
}
