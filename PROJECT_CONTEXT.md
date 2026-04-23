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
