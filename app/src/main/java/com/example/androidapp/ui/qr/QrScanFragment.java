package com.example.androidapp.ui.qr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.androidapp.R;
import com.example.androidapp.data.local.OfflineBookingCache;
import com.example.androidapp.data.model.OfflineBundle;
import com.example.androidapp.data.model.Reservation;
import com.example.androidapp.util.DateTimeUtils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QrScanFragment extends Fragment {

    //TAG para debugging
    private static final String TAG = "QrScanFragment";

    @Inject
    OfflineBookingCache offlineBookingCache;

    //launcher
    private ActivityResultLauncher<String> requestPermissionLauncher;

    //vista donde se renderiza el stream en tiempo real
    private PreviewView previewView;

    //Hilo dedicado para el analisis de imagenes
    private ExecutorService cameraExecutor;

    //reutilizado entre frames para no recrearlo en cada anlisis
    private BarcodeScanner barcodeScanner;

    //Texto para mostrar el resultado al escanear el QR
    private TextView tvResultado;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //launcher creado necesariamente en onCreate
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        iniciarCamara();
                    } else {
                        Toast.makeText(requireContext(),
                                "Se necesita el permiso de cámara para escanear QR",
                                Toast.LENGTH_LONG).show();
                    }
                }
        );



        // ML Kit: configurado solo para QR
        BarcodeScannerOptions opciones = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(opciones);


        cameraExecutor = Executors.newSingleThreadExecutor();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView = view.findViewById(R.id.previewView);
        tvResultado = view.findViewById(R.id.tvResultado);

        pedirPermisoCamara();
    }



    //Pedir permiso para la camara
    private void pedirPermisoCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // Permiso ya otorgado: ir directo a la cámara
            iniciarCamara();
        } else {
            // Solicitar al usuario; el resultado llega al launcher registrado en onCreate()
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    //Configurar camaraX y enlazar analizador QR
    private void iniciarCamara() {
        ListenableFuture<ProcessCameraProvider> futuro =
                ProcessCameraProvider.getInstance(requireContext());

        futuro.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = futuro.get();

                // Conecta el stream de la cámara al PreviewView del layout
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // STRATEGY_KEEP_ONLY_LATEST: descarta frames si el analyzer no terminó el anterior;
                // evita que se acumule una cola de imágenes pendientes
                ImageAnalysis analisis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analisis.setAnalyzer(cameraExecutor, this::analizarFrame);

                cameraProvider.unbindAll();
                // bindToLifecycle: CameraX libera la cámara automáticamente en onPause/onDestroy
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analisis
                );
            }
            catch (Exception e) {
                Log.w(TAG, "Error al enlazar camara", e);
            }

        }, ContextCompat.getMainExecutor(requireContext()));
    }

    //Procesar frames con ML kit
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analizarFrame(@NonNull ImageProxy imageProxy) {
        InputImage imagen = InputImage.fromMediaImage(
                Objects.requireNonNull(imageProxy.getImage()),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        barcodeScanner.process(imagen)
                .addOnSuccessListener(codigos -> {
                    for (Barcode codigo : codigos) {
                        String valor = codigo.getRawValue();
                        if (valor != null) procesarResultado(valor);
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error en ML Kit", e))
                .addOnCompleteListener(t -> {
                    // imageProxy.close() SIEMPRE en addOnCompleteListener, no en onSuccess.
                    // Si no se cierra, CameraX no puede entregar el siguiente frame
                    // y el pipeline se traba indefinidamente.
                    imageProxy.close();
                });
    }

    //Formatear JSON del qr y mostrar respuesta positiva o negativa
    //Validaciones de acuerdo a si es un QR valido, si se reservo la actividad y si se escanea en el dia de la actividad
    private void procesarResultado(String rawValue) {
        String mensaje;
        int colorFondo;
        try {
            JSONObject json = new JSONObject(rawValue);
            String activityId = json.optString("activityId", "");
            String qrDate = json.optString("selectedDate", "");

            if (!activityId.isEmpty() && !qrDate.isEmpty()) {
                int validacion = validarReserva(activityId, qrDate);
                if (validacion == 1) {
                    com.example.androidapp.ui.home.ActivityDetailFragment.confirmedAttendances.add(activityId);
                    mensaje = "Asistencia confirmada para la actividad.";
                    colorFondo = 0xFF4CAF50; // Verde
                } else {
                    colorFondo = 0xFFF44336; // Rojo
                    if (validacion == 0) {
                        mensaje = "Error: El código QR no corresponde al día de hoy.";
                    } else if (validacion == 2) {
                        mensaje = "Error: Tu reserva para esta actividad no es para el día de hoy.";
                    } else {
                        mensaje = "Error: No tienes una reserva para esta actividad.";
                    }
                }
            } else {
                colorFondo = 0xFFF44336; // Rojo
                if (activityId.isEmpty()) {
                    mensaje = "Error: QR inválido (activityId faltante)";
                } else {
                    mensaje = "Error: QR inválido (selectedDate faltante)";
                }
            }
        } catch (JSONException e) {
            mensaje = "Error: QR inválido. " + e.getMessage();
            colorFondo = 0xFFF44336; // Rojo
        }

        String mensajeFinal = mensaje;
        int colorFinal = colorFondo;
        requireActivity().runOnUiThread(() -> {
            tvResultado.setText(mensajeFinal);
            tvResultado.setBackgroundColor(colorFinal);
        });
    }


    // 1: OK, 0: QR no es hoy, 2: Reserva no es hoy, -1: No hay reserva
    private int validarReserva(String activityId, String qrDate) {
        // Condición 1: La fecha del QR debe ser hoy
        if (!DateTimeUtils.isToday(qrDate)) {
            return 0;
        }

        OfflineBundle bundle = offlineBookingCache.read();
        if (bundle == null || bundle.getBookings() == null) return -1;
        
        boolean tieneReservaDeActividad = false;
        for (Reservation r : bundle.getBookings()) {
            // Condición 2: El activityId debe coincidir
            if (activityId.equals(r.getActivityId())) {
                tieneReservaDeActividad = true;
                // Condición 3: La fecha de la reserva debe ser hoy
                if (DateTimeUtils.isToday(r.getSelectedDate())) {
                    return 1;
                }
            }
        }
        return tieneReservaDeActividad ? 2 : -1;
    }

    //liberar recursos al destruir fragment
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Apagar el executor para liberar el hilo de análisis
        cameraExecutor.shutdown();
        // Cerrar el cliente de ML Kit y liberar sus recursos nativos
        barcodeScanner.close();
    }

}
