# CLAUDE.md

Este archivo provee instrucciones a Claude Code (claude.ai/code) para trabajar en este repositorio.

## Contexto del Proyecto — lectura obligatoria

Al empezar a trabajar en este repositorio, leer siempre estos dos archivos completos (no solo cuando el cambio parezca tocarlos directamente):

- [`PROJECT_CONTEXT.md`](PROJECT_CONTEXT.md) — requerimientos académicos, tecnologías habilitadas por clase y restricciones de la cátedra. Aplica **solo al frontend**; el backend es libre y no está sujeto a estas restricciones ni es evaluado por la cátedra. Respetar todo lo definido ahí.
- [`FRONTEND_REFERENCE.md`](FRONTEND_REFERENCE.md) — referencia completa y actualizada del backend real (endpoints, modelos, quirks). Es la única documentación del backend en este repo.

## Descripción General

**android-app** — aplicación Android de un solo módulo escrita en **Java** (no Kotlin), usando Views tradicionales y layouts XML. Target API 36, minSdk 30.

El backend ya está hecho y deployado. El trabajo es exclusivamente el **frontend Android en Java**.

## Sistema de Build

- Gradle con **Kotlin DSL** (archivos `.gradle.kts`)
- Dependencias gestionadas mediante un **catálogo de versiones** (`gradle/libs.versions.toml`)
- Versión de AGP: 9.1.0, Gradle wrapper: 9.3.1
- `API_BASE_URL` definido en `app/build.gradle.kts` como `BuildConfig.API_BASE_URL`
- URL de producción: `https://uadetpodda1backend-production.up.railway.app/api/`

### Comandos Comunes

```bash
# Compilar APK debug
./gradlew assembleDebug

# Compilar APK release
./gradlew assembleRelease

# Ejecutar todos los tests unitarios
./gradlew test

# Ejecutar una clase de test unitario específica
./gradlew testDebugUnitTest --tests "com.example.androidapp.ExampleUnitTest"

# Ejecutar tests instrumentados (requiere emulador o dispositivo)
./gradlew connectedAndroidTest

# Limpiar build
./gradlew clean
```

## Arquitectura

- Módulo único `:app`
- **Single Activity Architecture** — `MainActivity` con Navigation Component
- Código fuente Java en `app/src/main/java/com/example/androidapp/`
- Layouts XML en `app/src/main/res/layout/`
- Navegación declarada en `app/src/main/res/navigation/` (múltiples nav graphs anidados)
- UI construida con ConstraintLayout, ListView y Views estándar de Android
- **Inyección de dependencias con Hilt** — `@HiltAndroidApp` en `MyApp`, `@AndroidEntryPoint` en Activities y Fragments (y en `NotificationPollingService`, ver nota abajo), módulo en `di/NetworkModule.java`
- Sin ViewModel ni LiveData (no habilitados por la cátedra)
- **Siempre usar `javax.inject.Inject`**, nunca `jakarta.inject.Inject` — causa crash en runtime en Android

> **Nota sobre `@AndroidEntryPoint` en `Service`**: `PROJECT_CONTEXT.md` (Clase 5) solo menciona explícitamente Activities y Fragments porque es lo que se vio en clase, pero Hilt soporta `@AndroidEntryPoint` oficialmente también en `Service`, `BroadcastReceiver` y `View` — es la misma herramienta ya habilitada, no una técnica nueva. Se usó en `NotificationPollingService` (Feature 12.29) para poder inyectar `NotificationsApi`, `TokenManager` y `NetworkMonitor` sin duplicar lógica de construcción manual. Decisión consultada y confirmada con el usuario.

### Estructura de paquetes

```
com.example.androidapp/
├── data/
│   ├── local/         TokenManager, NewsCache, PreferencesStore, SpotAdjustmentManager
│   ├── model/         POJOs con @SerializedName de Gson
│   │                  Activity, Reservation, Rating, RatingRequest, RatingData,
│   │                  News, NewsDetail, Filters, UserPreferencesRequest, Schedule,
│   │                  NotificationItem, etc.
│   └── remote/        ActivityApi, AuthApi, UserApi, RatingsApi, NewsApi, FavoritesApi,
│                      NotificationsApi
├── di/                NetworkModule, AuthRefreshInterceptor, LongPolling (qualifier)
├── ui/
│   ├── auth/          LoginFragment, OtpRequestFragment, OtpVerifyFragment,
│   │                  RegisterFragment, BiometricOptInDialog
│   ├── favorites/     FavoritesFragment
│   ├── historial/     HistorialFragment, HistorialAdapter, RatingFragment
│   ├── home/          HomeFragment, ActivityDetailFragment, SearchFragment,
│   │                  FiltersFragment, MapFragment, GalleryPagerAdapter,
│   │                  ProfileFragment, EditProfileFragment, ActivityAdapter
│   ├── news/          NewsFragment, NewsDetailFragment, NewsAdapter
│   ├── profile/       PreferencesFragment
│   └── reservation/   MisReservasFragment, ReservationFormFragment,
│                      CancelReservationFragment, ReservationAdapter
├── util/
│   │   DateTimeUtils, ImageLoader, FilterQueryBuilder, ApiErrorParser,
│   │   BiometricHelper, BiometricCanAuthMapper, BiometricStatus,
│   │   SessionEventBus, SessionExpiredListener, OtpResendCooldown, NetworkMonitor,
│   │   NotificationPollingService, NotificationHelper, NotificationActionReceiver
├── MainActivity.java
└── MyApp.java
```

## Networking

- **Retrofit** con **Gson** para serialización
- **OkHttp** con dos interceptores en `NetworkModule`:
  1. Bearer token (agrega `Authorization: Bearer <token>` a todos los requests)
  2. `AuthRefreshInterceptor` — intercepta 401, intenta refresh transparente via `POST /api/auth/refresh`; si el refresh también falla, dispara `SessionEventBus.notifySessionExpired()` que redirige al login
- El token se guarda con `TokenManager` (EncryptedSharedPreferences) — ahora incluye access token, refresh token, TTLs y flag de biometría
- Todas las respuestas siguen el formato `ApiResponse<T>`: `{ "success": bool, "data": T, "error": string, "meta": {...} }`
- Para parsear errores HTTP, usar `ApiErrorParser.getMessage(response)` — extrae el campo `error` del body

### Quirks conocidos del backend

- `meetingPoint`: verificado contra el código real del backend (`activityView.js` → `buildMeetingPoint`) — la API **siempre** serializa un objeto `{ latitude, longitude, address }`, nunca un string, para todas las actividades (incluso las que no tienen coordenadas propias, cae a un fallback por destino). `@JsonAdapter(MeetingPointAdapter.class)` sigue en el modelo como resguardo defensivo, pero la rama de "string" no se ejerce hoy contra la API real.
- El campo `date`/`dates` en `Activity.java` usa `@SerializedName(value="date", alternate={"dates"})` — **ambas claves llegan simultáneamente** en la respuesta real (`date` es un string único, `dates` es el array completo de fechas de todos los schedules). Gson asigna el valor de cada clave coincidente a medida que la encuentra en el JSON, así que la que aparece **última** en el objeto termina pisando a la anterior en el mismo campo Java. Hoy funciona (queda el array completo) porque el backend serializa `date` antes que `dates` en `data.js` — es un comportamiento correcto pero frágil, depende del orden de claves del backend. Si algún día `GET /api/activities` deja de traer el array completo en `date`, revisar este orden antes que el adapter.
- El token **no es JWT estándar** — es JSON en base64url. No validar localmente, confiar en el servidor. Si retorna 401 persistente, el server se reinició (datos en memoria).
- El backend guarda datos **en memoria** — si Railway reinicia el server, los tokens y reservas se pierden.
- **Ratings — respuesta anidada**: `GET /api/ratings/:bookingId` y `POST /api/ratings` devuelven `{ "data": { "rating": {...} } }`. Usar `RatingData` como tipo genérico de `ApiResponse` y llamar `.getData().getRating()`.
- **Ratings — campos snake_case/camelCase**: los modelos `Rating` y `RatingRequest` usan `@SerializedName(value="...", alternate="...")` para manejar ambos formatos.
- **Booking status — lógica de finalización**: el backend marca un booking como `finalized` cuando `selectedDate + duración_actividad < ahora (UTC-3)`. Las reservas en "Mis Actividades" se filtran por `status == "confirmed"` en el front para no mostrar las finalizadas.

## Testing

- **Tests unitarios**: JUnit 4 — `app/src/test/java/`
- **Tests instrumentados**: AndroidX Test + Espresso — `app/src/androidTest/java/`
- Test runner: `androidx.test.runner.AndroidJUnitRunner`
- `NewsCacheTest` — test instrumentado de round-trip de caché de noticias en almacenamiento interno

## Dependencias Clave

Todas gestionadas a través de `gradle/libs.versions.toml`:
- AndroidX AppCompat, Activity, ConstraintLayout
- Material Design Components
- Navigation Component (fragment + ui)
- Retrofit 2 + Gson converter
- OkHttp
- Hilt (Android DI) — annotationProcessor en Java (no kapt)
- Security Crypto (EncryptedSharedPreferences)
- **Glide** — carga de imágenes; usar siempre via `ImageLoader.load(imageView, url)`, nunca Glide directamente
- **osmdroid** — mapas offline para mostrar punto de encuentro en `ActivityDetailFragment`
- JUnit 4, AndroidX Test JUnit, Espresso (testing)

## Estado de Funcionalidades (al cierre de main — mayo 2026)

> **Qué significa "⚠️ Parcial"**: no es que el código Android esté incompleto — significa que el frontend está terminado y correcto, pero no está confirmado que el endpoint correspondiente funcione end-to-end contra el backend que esté corriendo *en este momento*. El backend tiene CI/CD que autodeploya a Railway, así que en general el deploy activo coincide con el código fuente (salvo que algún deploy haya fallado silenciosamente). El riesgo real y más probable es otro: el backend guarda todo **en memoria**, así que un reinicio de Railway (por redeploy, crash, o sleep del free tier) borra las reservas/favoritos previos necesarios para probar el flujo completo de punta a punta. Es una advertencia de "no verificado en la última prueba", no una dependencia obvia de "esto necesita al backend".

| Funcionalidad | Estado | Notas |
|---|---|---|
| Login (usuario/contraseña) | ✅ Funciona | Auto-prompt biométrico post-login si está habilitado |
| OTP | ✅ Funciona | Cooldown de 30s en reenvío; código en consola del server |
| Registro | ✅ Funciona | |
| Biometría | ✅ Funciona | `BiometricOptInDialog` post-login; toggle en Perfil; `BiometricHelper` |
| Refresh de token | ✅ Funciona | `AuthRefreshInterceptor` lo hace transparente en cada 401 |
| Home — listado de actividades | ✅ Funciona | Chips de filtro rápido + paginación; imágenes via Glide |
| Filtros avanzados | ✅ Funciona | `FiltersFragment` — destino, categoría, fecha, precio |
| Preferencias del usuario | ✅ Funciona | `PreferencesFragment` — categorías y destinos favoritos |
| Detalle de actividad | ✅ Funciona | Galería de fotos, mapa osmdroid con punto de encuentro |
| Búsqueda de actividades | ✅ Funciona | |
| Formulario de reserva | ⚠️ Parcial | UI funciona; depende de `/api/bookings` en Railway |
| Mis reservas | ✅ Funciona | Muestra reservas `confirmed`; muestra horas de cancelación |
| Cancelar reserva | ⚠️ Parcial | UI funciona; depende del endpoint en Railway |
| Historial | ✅ Funciona | Clickeable → detalle; botón "Calificar" por reserva |
| Calificaciones | ✅ Funciona | Formulario + read-only + ventana de 48hs desde la finalización (`selectedDate + duración`), calculada igual en front y back |
| Noticias | ✅ Funciona | `NewsFragment` + `NewsDetailFragment`; caché en storage interno (`NewsCache`) |
| Favoritos | ⚠️ Parcial | `FavoritesFragment` existe; funcionalidad de add/remove por verificar |
| Perfil | ✅ Funciona | Ver y editar datos; toggle biometría |
| Mapa / Punto de encuentro | ✅ Funciona | osmdroid en `ActivityDetailFragment`; solo si hay coordenadas |
| Modo sin conexión | ⚠️ Parcial | `NewsCache` en storage interno; `/api/bookings/offline-bundle` disponible pero no integrado en UI |
| Imágenes | ✅ Funciona | `ImageLoader.load()` con Glide en todas las listas y detalle |
| Recordatorio 24hs (Feature 12.29) | ⚠️ Parcial | Long polling + Foreground Service funcionando; botón "Ver Voucher" es no-op (Toast) hasta que exista la pantalla de voucher (Feature 11) |

## Bugs conocidos / cosas a revisar

- **Reservas con 404**: los endpoints `/api/bookings` pueden devolver 404 si la versión deployada en Railway no tiene `bookings.routes.js` activo. Verificar en Railway antes de cambiar endpoints.
- **Historial vacío**: `GET /api/activities/history` retorna solo reservas `finalized`. Si Railway reinició y no hay reservas finalizadas en memoria, el historial aparece vacío aunque funcione correctamente.
- **Botones en ListView items**: cualquier `Button` visible dentro de un ítem de `ListView` debe tener `android:focusable="false"` para que el `OnItemClickListener` de la fila funcione. Sin esto, el botón captura el touch y la fila no es presionable.
- **Favoritos UI**: `FavoritesFragment` existe pero la integración completa (agregar/quitar desde detalle de actividad) puede estar incompleta.

## Convenciones del proyecto

- Todo el código en **Java** — nunca Kotlin
- **Inyección**: siempre `javax.inject.Inject`, nunca `jakarta.inject.Inject`
- Fragments gestionados con **Navigation Component**, no con `FragmentManager` directo
- Callbacks de Retrofit: siempre verificar `isAdded()` antes de actualizar UI en `onResponse` y `onFailure`
- Logging: usar `Log.e("NombreClase", "mensaje", throwable)` para errores de red
- El `FRONTEND_REFERENCE.md` es la fuente de verdad del backend — actualizarlo si el backend cambia
- **Imágenes**: usar siempre `ImageLoader.load(imageView, url)` — centraliza Glide, maneja URLs relativas y absolutas, y aplica placeholder automáticamente
- **Detalle de actividad reutilizable**: `ActivityDetailFragment` acepta args `showReserveButton` (default `true`) y `showSpotsField` (default `true`). Pasar `false` desde historial o mis reservas
- **Comparación de fechas**: usar siempre `DateTimeUtils.isFutureOrNow(String)` o `Instant` UTC directamente. Nunca `LocalDate.now()` contra ISO con hora
- **Ventana de calificación**: `RatingFragment` necesita tanto `activityDate` como `activityDuration` (nav args desde `HistorialFragment`) para calcular el deadline de 48hs desde la finalización real de la actividad. Si se agrega otro punto de entrada a `RatingFragment`, pasar ambos argumentos — el cálculo replica el parseo de duración del backend (`parseDurationMs` en `data.js`)
- **Botones en ListView**: agregar `android:focusable="false"` a todo `Button` dentro de un `item_*.xml`
- **Notificaciones (Long Polling)**: `NotificationPollingService` (Foreground Service, `util/`) sondea `GET /api/notifications/poll` reusando `AuthRefreshInterceptor` vía un `OkHttpClient`/`Retrofit` separado calificados con `@LongPolling` (`di/LongPolling.java`) — necesitan un read timeout largo (~35s) que no debe aplicarse al resto de las Api. El service se arranca de forma idempotente desde `MainActivity` (en el listener de navegación existente y en `onResume()`) y se detiene en exactamente dos lugares: `MainActivity.onSessionExpired()` y `ProfileFragment.finishLogout()`. Requiere el permiso runtime `POST_NOTIFICATIONS` (API 33+, patrón `ActivityResultContracts.RequestPermission()` ya usado en el proyecto). El botón "Ver Voucher" de la notificación de recordatorio apunta a `NotificationActionReceiver`, que hoy solo muestra un Toast — es la plomería lista para cuando exista la pantalla de voucher (Feature 11)
- **Session expired**: si un request falla con 401 irrecuperable, `AuthRefreshInterceptor` dispara `SessionEventBus` → `MainActivity` escucha y navega al login. No manejar 401 manualmente en los fragments
- **Parseo de errores**: usar `ApiErrorParser.getMessage(response)` para extraer el string de error del body en lugar de hardcodear mensajes
