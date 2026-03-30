package com.example.androidapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.androidapp.R;
import com.example.androidapp.data.local.SessionManager;
import com.example.androidapp.ui.home.HomeActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateToHome();
            return;
        }

        setContentView(R.layout.activity_login);

        if (savedInstanceState == null) {
            showLogin();
        }
    }

    public void showLogin() {
        // Clear back stack and show login as root
        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        replaceFragment(new LoginFragment(), false);
    }

    public void showRegister() {
        replaceFragment(new RegisterFragment(), true);
    }

    public void showOtpRequest() {
        replaceFragment(new OtpRequestFragment(), true);
    }

    public void showOtpVerify(String email) {
        OtpVerifyFragment fragment = OtpVerifyFragment.newInstance(email);
        replaceFragment(fragment, true);
    }

    public void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out);
        transaction.replace(R.id.auth_fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}
