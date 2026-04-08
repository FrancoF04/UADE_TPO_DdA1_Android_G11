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
