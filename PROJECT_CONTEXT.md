# PROJECT_CONTEXT.md

Contexto académico del TPO de Desarrollo de Apps I — UADE 2026, 1er Cuatrimestre.

> **IMPORTANTE para IAs:** Este archivo define las tecnologías habilitadas, patrones requeridos y restricciones impuestas por la cátedra. No sugerir ni implementar alternativas fuera de lo aquí definido sin consultar al usuario primero. Este archivo crece con el avance de las clases — nunca eliminar entradas existentes.

---

## Prácticas de desarrollo requeridas

- Ramas por funcionalidad: prefijos `feature/` y `fix/`
- Integración a `main` únicamente mediante Pull Requests (no se permiten pushes directos)
- Cada integrante debe tener al menos una PR mergeada por entrega

---

## Herramientas habilitadas

- **Android Studio** — IDE principal
- **scrcpy** — visualizar y controlar dispositivo Android desde la PC
- **Visual Studio Code** — editor alternativo

---

## Clase 1 — Primeros pasos en Android

### Configuración
- **AndroidManifest.xml**: declaración de Activities, Services, BroadcastReceivers y ContentProviders; permisos (ej. `INTERNET`, `ACCESS_FINE_LOCATION`); Activity principal y metadatos globales
- **Gradle**: agregar dependencias en `build.gradle (Module: app)`

### Componentes habilitados
- **Activity** — ciclo de vida completo (`onCreate`, `onStart`, `onResume`, etc.); vinculación con layouts XML via `setContentView()` y `findViewById()`
- **Eventos de UI** — `setOnClickListener` (clase anónima o lambda); `setOnLongClickListener`, `setOnCheckedChangeListener`, `setOnItemSelectedListener`
- **ListView** — con `ArrayAdapter` o `BaseAdapter`; selección con `setOnItemClickListener`
- **Service** — Started Service (`startService`, `stopService`, `stopSelf`) y Bound Service (`bindService`); ciclo de vida: `onCreate`, `onStartCommand`, `onBind`, `onDestroy`
- **BroadcastReceiver** — recepción de eventos del sistema (ej. `BOOT_COMPLETED`); declaración en `AndroidManifest.xml`
- **Intents**
  - *Explícitos*: abrir Activities dentro de la app, pasar datos con `putExtra` / `getStringExtra`
  - *Implícitos*: delegar acciones al sistema (`ACTION_VIEW`, `ACTION_SEND`, `ACTION_CALL`, `ACTION_IMAGE_CAPTURE`)

---

## Clase 3 — Fragments & Navigation Component

### Componentes habilitados
- **Fragment** — modulariza la UI dentro de una Activity; ciclo de vida: `onCreate`, `onCreateView`, `onViewCreated`, `onDestroyView`; gestionado con `FragmentManager` y `FragmentTransaction` (`add`, `replace`, `remove`, `commit`); insertado en `FragmentContainerView`
- **Navigation Component (Jetpack)**
  - Navigation Graph (`res/navigation/nav_graph.xml`)
  - `NavHostFragment` como contenedor en el layout de la Activity
  - `NavController` para navegar entre destinos y manejar el back stack
  - Dependencias:
    ```gradle
    implementation "androidx.navigation:navigation-fragment"
    implementation "androidx.navigation:navigation-ui"
    ```

### Implementación
- Navigation Graph define destinos (Fragments/Activities) y acciones entre ellos con argumentos (`Bundle`)
- `NavHostFragment` en Activity:
  ```xml
  <androidx.fragment.app.FragmentContainerView
      android:id="@+id/nav_host_fragment"
      android:name="androidx.navigation.fragment.NavHostFragment"
      app:navGraph="@navigation/nav_graph"
      app:defaultNavHost="true"
      android:layout_width="match_parent"
      android:layout_height="match_parent" />
  ```

---

## Clase 3 — API REST & Retrofit

### Herramientas habilitadas
- **Retrofit** — cliente HTTP; convierte JSON a objetos Java con **Gson** o **Moshi**; anotaciones `@GET`, `@POST`, `@PUT`, `@DELETE`; manejo de respuestas con `onResponse` / `onFailure`
- **OkHttp** — interceptores, logs, tokens, cache

### Implementación
```java
interface ApiService {
    @GET("clases")
    Call<List<Clase>> getClases();

    @GET("clases/{id}")
    Call<Clase> getClaseById(@Path("id") int id);

    @POST("reservas")
    Call<Reserva> crearReserva(@Body ReservaRequest body);
}
```

---

## Clase 5 — Inyección de Dependencias (Hilt) & Autenticación (JWT)

### Herramientas habilitadas
- **Hilt** (Google, sobre Dagger) — framework oficial de DI para Android; reemplaza el singleton manual de Retrofit; maneja automáticamente el ciclo de vida y creación de objetos
- **Retrofit + OkHttp** — integración con Hilt mediante módulos; interceptores para headers (ej. `Authorization`)
- **JWT (JSON Web Token)** — token devuelto por el servidor tras login; persistencia en `SharedPreferences` mediante un `TokenManager`; se envía en cada request con el header `Authorization: Bearer <token>`

### Implementación
- `@HiltAndroidApp` en la clase `Application`
- `@AndroidEntryPoint` en Activities y Fragments
- `@Module` + `@Provides` para definir cómo crear objetos (ej. Retrofit, ApiService)
- Scopes: `@Singleton`, `@ActivityScoped`

```java
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {
    @Provides @Singleton
    public Retrofit provideRetrofit(OkHttpClient client) {
        return new Retrofit.Builder()
            .baseUrl("https://api.tuapp.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    }

    @Provides @Singleton
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}
```

---

## Clase 6 — Persistencia de Datos & Galería

### Herramientas de persistencia habilitadas

| Herramienta | Casos de uso |
|---|---|
| **DataStore** | Configuraciones simples: modo oscuro, idioma, flags de onboarding, último usuario logueado |
| **Room (SQLite)** | Datos estructurados: notas, tareas, productos; apps offline |
| **Files internos** | Datos privados y caché; solo accesible por la app; se elimina al desinstalar |
| **Files externos** | Imágenes, PDFs, descargas; accesible por otras apps/usuario; requiere permisos |
| **EncryptedSharedPreferences** | Tokens JWT, credenciales temporales, claves API; cifra automáticamente los datos |
| **Glide** | Carga y display de imágenes con cache automático, resize inteligente y placeholders |

> **SharedPreferences** legado — no usar para código nuevo.

### Implementación

**DataStore**
```java
// Guardar
dataStore.edit(prefs -> prefs.set(KEY, value));

// Leer (Flow)
dataStore.data().map(prefs -> prefs.get(KEY));
```

**Room**
```java
@Entity
public class Nota { ... }

@Dao
public interface NotaDao {
    @Insert void insert(Nota nota);
    @Query("SELECT * FROM nota") List<Nota> getAll();
}
```

**Files internos**
```java
FileOutputStream fos = openFileOutput("datos.txt", Context.MODE_PRIVATE);
// getFilesDir() para la ruta base
```

**EncryptedSharedPreferences**
```java
SharedPreferences sp = EncryptedSharedPreferences.create(
    "prefs_seguras", masterKey, context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
);
sp.edit().putString("token", jwt).apply();
sp.getString("token", null);
```

**Glide**
```java
Glide.with(this)
    .load(new File(path))
    .placeholder(R.drawable.ic_default_avatar)
    .circleCrop()
    .into(imageView);
```

### Galería e imágenes

- **Permisos runtime**: Android ≤ 12 → `READ_EXTERNAL_STORAGE`; Android ≥ 13 → `READ_MEDIA_IMAGES`; pedir con `requestPermissions()`
- **Abrir galería**: `Intent.ACTION_PICK` con `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`; resultado en `onActivityResult()` devuelve `Uri`
- **Guardar imagen**: convertir `Bitmap` a JPEG con `compress()` → guardar en `getFilesDir()` → persistir ruta en DataStore
- **El `Uri` devuelto por la galería es una referencia, no la imagen** — siempre convertir/guardar antes de usar

---

## Clase 7 — Biometría y Seguridad en el Dispositivo

### Herramientas habilitadas

| Herramienta | Descripción |
|---|---|
| **ActivityResultLauncher** | Patrón moderno para lanzar acciones del sistema y recibir resultados tipados |
| **BiometricManager** | Consulta disponibilidad de hardware biométrico y credenciales enroladas |
| **PromptInfo** | Configura el diálogo de autenticación (título, subtítulo, autenticadores permitidos) |
| **BiometricPrompt** | Dispara la autenticación y recibe callbacks de resultado |

**Contratos disponibles para `ActivityResultLauncher`:**
- `GetContent` → elegir archivo (imagen, PDF, etc.), devuelve `Uri`
- `RequestPermission` → pedir un permiso en runtime, devuelve `boolean`
- `RequestMultiplePermissions` → pedir varios permisos juntos, devuelve `Map<String, Boolean>`
- `StartActivityForResult` → genérico, cuando no hay contrato específico

**Clasificación de biometría en Android:**
- **Class 3 (STRONG)** → permite operaciones criptográficas
- **Class 2 (WEAK)** → solo desbloqueo de pantalla
- **Class 1** → no usar en apps

### Implementación

**ActivityResultLauncher**
```java
// Registrar antes de STARTED (en onCreate)
private ActivityResultLauncher<String> pickImageLauncher =
    registerForActivityResult(new ActivityResultContracts.GetContent(),
        uri -> { if (uri != null) imageView.setImageURI(uri); });

pickImageLauncher.launch("image/*");
```

**BiometricManager**
```java
BiometricManager manager = BiometricManager.from(context);
int canAuth = manager.canAuthenticate(
    Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL);
```

**PromptInfo**
```java
PromptInfo info = new PromptInfo.Builder()
    .setTitle("Login")
    .setSubtitle("Ingresa con tu huella")
    .setAllowedAuthenticators(
        Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL)
    .build();
```

**BiometricPrompt**
```java
Executor exec = ContextCompat.getMainExecutor(this);
BiometricPrompt prompt = new BiometricPrompt(this, exec,
    new AuthenticationCallback() {
        @Override public void onAuthenticationSucceeded(result) { irAHome(); }
        @Override public void onAuthenticationError(code, msg) { ... }
        @Override public void onAuthenticationFailed() { ... }
    });
prompt.authenticate(info);
```

### Limitaciones / Consideraciones

- El launcher **debe registrarse antes de `STARTED`** (en `onCreate`); su callback es asíncrono
- Si el usuario no tiene ningún método biométrico enrolado, la app no puede usar biometría — opciones: deshabilitar el botón, o redirigir con `Settings.ACTION_BIOMETRIC_ENROLL`
- Si `PromptInfo` usa `DEVICE_CREDENTIAL`, **no se puede llamar a `setNegativeButtonText()`**

---

## Clase 8 — Material Design / Material Components

> **A diferencia del resto de las clases documentadas acá, esto no es un requisito de la consigna del TPO** (no se menciona en el enunciado oficial). Se deja documentado como herramienta **disponible y habilitada por si se decide usar** para mejorar la UI, no como tecnología obligatoria.

### Herramientas habilitadas

- **Material Components para Android** (`com.google.android.material:material`)
- **Navigation Component** (`NavController` + `FragmentContainerView`) para manejar el back stack — ya en uso en el proyecto independientemente de Material Design

### Theme global

- Se define en `res/values/themes.xml` (y su variante `values-night` para modo oscuro)
- Se activa con `android:theme="@style/TuTema"` en el tag `<application>` de `AndroidManifest.xml`
- Parent recomendado: `Theme.MaterialComponents.DayNight.NoActionBar` (usar Toolbar propia en vez de la ActionBar del sistema)
- Atributos de color a definir en el Theme: `colorPrimary`, `colorOnPrimary`, `colorSecondary`, `colorOnSecondary`, `colorSurface`, `colorOnSurface`, `colorError`
- Regla: **no hardcodear colores en los layouts**; todo componente Material debe tomar sus colores del Theme

### Componentes Material habilitados

- `MaterialButton` (`com.google.android.material.button.MaterialButton`)
- `TextInputLayout` + `TextInputEditText` (`com.google.android.material.textfield.*`)
- `MaterialCardView` (`com.google.android.material.card.MaterialCardView`)
- `MaterialToolbar` como TopAppBar, vinculada con `setSupportActionBar()`
- `TabLayout` (`com.google.android.material.tabs.TabLayout`), opcional, conectado a `ViewPager2`
- `BottomNavigationView` (`com.google.android.material.bottomnavigation.BottomNavigationView`), vinculado a `NavController`

### Jerarquía de estilos permitida

De menor a mayor prioridad — todas son válidas pero con criterio de uso:

1. **Theme** (`themes.xml`) — usar para el caso general (90% de los casos)
2. **Style** (`styles.xml`, `style="@style/..."` en el layout) — solo para variantes reutilizables que se repiten en más de 2 lugares
3. **Atributo XML directo** (ej. `android:backgroundTint="#..."`) — solo para casos realmente únicos; evitarlo como regla general porque dificulta el mantenimiento

### Safe Area / Insets

- `WindowInsetsCompat` de AndroidX
- Desde Android 15 (API 35) el edge-to-edge es obligatorio: si no se manejan los insets, el contenido queda tapado por las barras del sistema
- Componentes como `AppBarLayout` y `BottomNavigationView` ya manejan algunos insets automáticamente al usar un tema Material

---

## Clase 9 — Lectura de QR y Notificaciones (Long Polling)

### Lectura de códigos QR

- Librería habilitada: **ML Kit Barcode Scanning** (`com.google.mlkit:barcode-scanning`). Es un modelo on-device ya entrenado; no corresponde reimplementar el decodificado a mano
- Permisos: patrón estándar de runtime permissions de Android
  - `Manifest.permission.CAMERA`
  - Verificar previamente con `ContextCompat.checkSelfPermission(...)`
  - Pedir el permiso con `registerForActivityResult(new ActivityResultContracts.RequestPermission(), ...)` guardado en un `ActivityResultLauncher<String>`
- La implementación completa de la cámara/scanner no está en las diapositivas (se remite al repositorio del curso)

### Notificaciones vía Long Polling

- No hay push real habilitado (FCM/APNs no forman parte de este alcance) — la app se entera de novedades sondeando un endpoint propio
- Dónde correr el loop de sondeo:
  - **Foreground Service**: necesario si el sondeo debe seguir con la app en background. Requiere `startForeground()` y mostrar una notificación persistente mientras corre. No sobrevive si el usuario fuerza el cierre de la app desde Ajustes
  - **WorkManager**: para chequeos periódicos más espaciados y con ahorro de batería (no para sondeo continuo)
- Cliente HTTP habilitado en el ejemplo: **OkHttp** (`okHttpClient.newCall(request).execute()`)
- Parseo de la respuesta: **Gson** (`gson.fromJson(json, Clase.class)`)
- Notificaciones nativas: `NotificationCompat.Builder` + `NotificationManager`
  - Requiere crear un `NotificationChannel` en Android 8+ (API 26+)
- Reglas obligatorias de robustez:
  - Fijar un timeout en el cliente (el servidor puede no responder nunca)
  - Ante cualquier `IOException`/error de red, loguear y reintentar el ciclo — nunca terminar el loop silenciosamente
  - Evitar un `while` sin pausas (agota batería); usar backoff o espaciar con WorkManager
  - En redes móviles inestables, validar conectividad antes de reintentar (evita loops de error en cascada)
- Referencia del endpoint (server-side, no se implementa en la app pero define el contrato): el servidor retiene la respuesta hasta que hay novedades o se cumple un timeout (~25s en el ejemplo), y responde `204` si no hubo novedades
