package com.example.androidapp.ui.home;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;

import com.example.androidapp.R;
import com.example.androidapp.util.ImageLoader;

import java.util.List;

public class GalleryPagerAdapter extends PagerAdapter {

    private final List<String> urls;

    public GalleryPagerAdapter(List<String> urls) {
        this.urls = urls;
    }

    @Override
    public int getCount() {
        // Siempre al menos 1 página para mostrar placeholder cuando no hay fotos
        return urls.isEmpty() ? 1 : urls.size();
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        ImageView imageView = new ImageView(container.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setBackgroundColor(ContextCompat.getColor(container.getContext(), R.color.placeholder_2));

        if (urls.isEmpty()) {
            // Sin fotos: mostrar ícono placeholder directamente, centrado
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setImageResource(R.drawable.ic_image_placeholder);
        } else {
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ImageLoader.load(imageView, urls.get(position));
        }

        container.addView(imageView);
        return imageView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
}
