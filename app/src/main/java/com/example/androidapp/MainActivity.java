package com.example.androidapp;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.util.NetworkMonitor;
import com.example.androidapp.util.SessionEventBus;
import com.example.androidapp.util.SessionExpiredListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity
        implements SessionExpiredListener, NetworkMonitor.OnNetworkChangeListener {

    @Inject TokenManager tokenManager;
    @Inject SessionEventBus sessionEventBus;
    @Inject NetworkMonitor networkMonitor;

    private NavController navController;
    private TextView tvOfflineBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvOfflineBanner = findViewById(R.id.tvOfflineBanner);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        navController = navHostFragment.getNavController();

        NavInflater inflater = navController.getNavInflater();
        NavGraph graph = inflater.inflate(R.navigation.nav_graph);

        boolean hasLegacyTokenOnly = tokenManager.getToken() != null
                && tokenManager.getRefreshToken() == null
                && tokenManager.getAccessExpiresAt() == 0L;
        if (hasLegacyTokenOnly) {
            tokenManager.clearSession();
        }

        if (tokenManager.getToken() == null) {
            graph.setStartDestination(R.id.auth_nav_graph);
            navController.setGraph(graph);
        } else if (tokenManager.isAccessTokenValid()) {
            graph.setStartDestination(R.id.home_nav_graph);
            navController.setGraph(graph);
        } else if (tokenManager.isRefreshTokenValid() && tokenManager.isBiometricEnabled()) {
            graph.setStartDestination(R.id.auth_nav_graph);
            Bundle args = new Bundle();
            args.putBoolean("autoPromptBiometric", true);
            navController.setGraph(graph, args);
        } else {
            graph.setStartDestination(R.id.auth_nav_graph);
            Bundle args = new Bundle();
            args.putBoolean("forceUserPass", true);
            navController.setGraph(graph, args);
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        View navHostView = findViewById(R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(bottomNav, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            View focus = getCurrentFocus();
            if (focus != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            }
            if (destination.getId() == R.id.homeFragment
                    || destination.getId() == R.id.reservasFragment
                    || destination.getId() == R.id.newsFragment
                    || destination.getId() == R.id.favoritesFragment
                    || destination.getId() == R.id.profileFragment) {
                bottomNav.setVisibility(View.VISIBLE);
                bottomNav.post(() -> navHostView.setPadding(0, 0, 0, bottomNav.getHeight()));
            } else {
                bottomNav.setVisibility(View.GONE);
                navHostView.setPadding(0, 0, 0, 0);
            }
        });

        updateOfflineBanner(networkMonitor.isConnected());
    }

    @Override
    protected void onResume() {
        super.onResume();
        sessionEventBus.register(this);
        networkMonitor.register(this);
        updateOfflineBanner(networkMonitor.isConnected());
    }

    @Override
    protected void onPause() {
        super.onPause();
        sessionEventBus.unregister(this);
        networkMonitor.unregister(this);
    }

    @Override
    public void onNetworkAvailable() {
        updateOfflineBanner(true);
    }

    @Override
    public void onNetworkLost() {
        updateOfflineBanner(false);
    }

    private void updateOfflineBanner(boolean isOnline) {
        if (tvOfflineBanner != null) {
            tvOfflineBanner.setVisibility(isOnline ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onSessionExpired() {
        runOnUiThread(() -> {
            Bundle args = new Bundle();
            args.putBoolean("forceUserPass", true);
            navController.popBackStack(R.id.loginFragment, false);
            navController.navigate(R.id.loginFragment, args);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
