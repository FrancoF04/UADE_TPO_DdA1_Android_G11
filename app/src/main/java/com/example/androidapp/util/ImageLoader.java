package com.example.androidapp.util;

import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.androidapp.BuildConfig;
import com.example.androidapp.R;

import java.net.URI;

public final class ImageLoader {

    private ImageLoader() {
    }

    public static void load(ImageView imageView, @Nullable String rawImageUrl) {
        String resolvedUrl = resolveUrl(rawImageUrl);
        ColorDrawable placeholder = new ColorDrawable(
                ContextCompat.getColor(imageView.getContext(), R.color.placeholder_2));

        if (TextUtils.isEmpty(resolvedUrl)) {
            Glide.with(imageView).clear(imageView);
            imageView.setImageDrawable(placeholder);
            return;
        }

        Glide.with(imageView)
                .load(resolvedUrl)
                .placeholder(placeholder)
                .error(placeholder)
                .centerCrop()
                .into(imageView);
    }

    @Nullable
    private static String resolveUrl(@Nullable String rawImageUrl) {
        if (rawImageUrl == null) {
            return null;
        }

        String trimmed = rawImageUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        String normalizedBase = BuildConfig.API_BASE_URL.endsWith("/")
                ? BuildConfig.API_BASE_URL
                : BuildConfig.API_BASE_URL + "/";

        try {
            URI baseUri = URI.create(normalizedBase);
            if (baseUri.getScheme() == null || baseUri.getAuthority() == null) {
                return trimmed;
            }

            String origin = baseUri.getScheme() + "://" + baseUri.getAuthority();
            if (trimmed.startsWith("/")) {
                return origin + trimmed;
            }

            return origin + "/" + trimmed;
        } catch (IllegalArgumentException ex) {
            return trimmed;
        }
    }
}
