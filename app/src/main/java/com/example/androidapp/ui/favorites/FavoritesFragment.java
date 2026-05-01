package com.example.androidapp.ui.favorites;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.androidapp.R;
import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.remote.FavoritesApi;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class FavoritesFragment extends Fragment implements FavoritosAdapter.OnFavoriteActionListener {

    @Inject FavoritesApi favoritesApi;

    private ListView listView;
    private ProgressBar progressBar;
    private LinearLayout llEmptyState;
    private FavoritosAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mis_favoritos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listView = view.findViewById(R.id.lvFavoritos);
        progressBar = view.findViewById(R.id.pbFavoritos);
        llEmptyState = view.findViewById(R.id.llEmptyFavoritos);

        adapter = new FavoritosAdapter(requireContext(), this);
        listView.setAdapter(adapter);

        loadFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (llEmptyState != null) llEmptyState.setVisibility(View.GONE);

        // Usamos un timestamp para evitar cache y asegurar que vemos el reset del servidor
        favoritesApi.getFavorites(System.currentTimeMillis()).enqueue(new Callback<ApiResponse<List<Activity>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Activity>>> call, Response<ApiResponse<List<Activity>>> response) {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<Activity> data = response.body().getData();
                    adapter.setFavorites(data);
                    
                    if (data == null || data.isEmpty()) {
                        if (llEmptyState != null) llEmptyState.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(getContext(), "Error al cargar favoritos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Activity>>> call, Throwable t) {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRemoveFavorite(Activity activity) {
        favoritesApi.removeFavorite(activity.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    loadFavorites();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onReserveNow(Activity activity) {
        Bundle args = new Bundle();
        args.putString("activityId", activity.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_favorites_to_reservation, args);
    }

    @Override
    public void onItemClick(Activity activity) {
        Bundle args = new Bundle();
        args.putString("activityId", activity.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_favorites_to_detail, args);
    }
}
