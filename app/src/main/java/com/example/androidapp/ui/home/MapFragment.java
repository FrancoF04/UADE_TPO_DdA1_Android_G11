package com.example.androidapp.ui.home;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import com.example.androidapp.R;

public class MapFragment extends Fragment {

    private static final String ARG_LAT   = "latitud";
    private static final String ARG_LNG   = "longitud";
    private static final String ARG_ADDRESS = "address";
    private static final String ARG_ZOOM  = "zoom";

    private MapView mapView;
    private double latitud;
    private double longitud;
    private String address;
    private float zoom;

    public static MapFragment newInstance(double latitud, double longitud) {
        return newInstance(latitud, longitud, null, 15.0f);
    }

    public static MapFragment newInstance(double latitud, double longitud, String address, float zoom) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LAT, latitud);
        args.putDouble(ARG_LNG, longitud);
        args.putString(ARG_ADDRESS, address);
        args.putFloat(ARG_ZOOM, zoom);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = requireContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        Configuration.getInstance().setUserAgentValue(context.getPackageName());

        if (getArguments() != null) {
            latitud = getArguments().getDouble(ARG_LAT);
            longitud = getArguments().getDouble(ARG_LNG);
            address  = getArguments().getString(ARG_ADDRESS);
            zoom     = getArguments().getFloat(ARG_ZOOM, 15.0f);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        mapView = view.findViewById(R.id.mapview);
        configurarMapa();

        view.findViewById(R.id.fabNavegar).setOnClickListener(v -> abrirNavegacion());
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDetach();
    }

    private void configurarMapa(){
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(false);

        IMapController controller = mapView.getController();
        controller.setZoom(zoom);
        controller.setCenter(new GeoPoint(latitud, longitud));

        agregarMarcador();
    }

    private void agregarMarcador(){
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(latitud, longitud));
        marker.setTitle(address != null ? address : "Ubicación");
        marker.setOnMarkerClickListener((m, mapView) -> {
            abrirNavegacion();
            return true;
        });
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    private void agregarEventoToque(){
        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                abrirNavegacion();
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
    }

    private void abrirNavegacion(){
        Uri uri = Uri.parse("geo:" + latitud + "," + longitud
                          + "?q=" + latitud + "," + longitud
                          + "(" + Uri.encode(address != null ? address : "") + ")");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(Intent.createChooser(intent, "Navegar con..."));
    }
}  
