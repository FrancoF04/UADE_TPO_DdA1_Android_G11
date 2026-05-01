package com.example.androidapp.util;

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

        if (TextUtils.isEmpty(resolvedUrl)) {
            imageView.setImageResource(R.drawable.ic_image_placeholder);
            imageView.setBackgroundColor(ContextCompat.getColor(imageView.getContext(), R.color.placeholder_2));
            return;
        }

        Glide.with(imageView)
                .load(resolvedUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
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
