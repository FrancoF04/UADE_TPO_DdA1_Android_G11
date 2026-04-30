# CLAUDE.md

Este archivo provee instrucciones a Claude Code (claude.ai/code) para trabajar en este repositorio.

## Contexto del Proyecto

Antes de realizar cualquier cambio, leer [`PROJECT_CONTEXT.md`](PROJECT_CONTEXT.md). Contiene los requerimientos académicos, tecnologías obligatorias y restricciones de la cátedra. Respetar todo lo definido ahí.

## Descripción General

**android-app** — aplicación Android de un solo módulo escrita en **Java** (no Kotlin), usando Views tradicionales y layouts XML. Target API 36, minSdk 30.

El backend ya está hecho y deployado. El trabajo es exclusivamente el **frontend Android en Java**. Ver [`FRONTEND_REFERENCE.md`](FRONTEND_REFERENCE.md) para la referencia completa del backend antes de tocar cualquier cosa relacionada a endpoints, modelos o respuestas del servidor.

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
- **Inyección de dependencias con Hilt** — `@HiltAndroidApp` en `MyApp`, `@AndroidEntryPoint` en Activities y Fragments, módulo en `di/NetworkModule.java`
- Sin ViewModel ni LiveData (no habilitados por la cátedra)

### Estructura de paquetes

```
com.example.androidapp/
├── data/
│   ├── local/         TokenManager (EncryptedSharedPreferences)
│   ├── model/         Modelos de datos (POJOs con @SerializedName de Gson)
│   │                  Incluye: Rating, RatingRequest, RatingData (wrapper de respuesta)
│   └── remote/        Interfaces Retrofit (ActivityApi, AuthApi, UserApi, RatingsApi, etc.)
├── di/                NetworkModule (provee Retrofit, OkHttp, todas las APIs)
├── ui/
│   ├── auth/          Login, OTP, Register fragments
│   ├── historial/     HistorialFragment, HistorialAdapter, RatingFragment
│   ├── home/          HomeFragment, ActivityDetailFragment, SearchFragment, ProfileFragment, EditProfileFragment
│   └── reservation/   MisReservasFragment, ReservationFormFragment, CancelReservationFragment, ReservationAdapter
├── util/              DateTimeUtils (parsing y comparación de fechas/instants)
├── MainActivity.java
└── MyApp.java
```

## Networking

- **Retrofit** con **Gson** para serialización
- **OkHttp** con interceptor en `NetworkModule` que agrega `Authorization: Bearer <token>` a todos los requests automáticamente
- El token se guarda y lee con `TokenManager` (EncryptedSharedPreferences)
- Todas las respuestas siguen el formato `ApiResponse<T>`: `{ "success": bool, "data": T, "error": string, "meta": {...} }`

### Quirks conocidos del backend

- `meetingPoint` puede llegar como **string** o como **objeto** `{ latitude, longitude, address }` dependiendo del endpoint. Siempre usar `@JsonAdapter(MeetingPointAdapter.class)` en el campo.
- El campo `date`/`dates` en `Activity` puede ser string o array — manejado con `@JsonAdapter(StringListOrStringAdapter.class)`.
- El token **no es JWT estándar** — es JSON en base64url. No validar localmente, confiar en el servidor. Si retorna 401, el token expiró o el servidor se reinició (datos en memoria).
- El backend guarda datos **en memoria** — si Railway reinicia el server, los tokens y reservas se pierden.
- **Ratings — respuesta anidada**: `GET /api/ratings/:bookingId` y `POST /api/ratings` devuelven `{ "data": { "rating": {...} } }`, no `{ "data": { campos directos } }`. Usar `RatingData` como tipo genérico de `ApiResponse` y llamar `.getData().getRating()`.
- **Ratings — campos snake_case/camelCase**: el backend acepta ambos (`activityRating` o `activity_rating`). Los modelos `Rating` y `RatingRequest` usan `@SerializedName(value="...", alternate="...")` para manejar ambos formatos.
- **Booking status — lógica de finalización**: el backend marca un booking como `finalized` cuando `selectedDate + duración_actividad < ahora (UTC-3)`. Las reservas en "Mis Actividades" se filtran por `status == "confirmed"` en el front para no mostrar las finalizadas.

## Testing

- **Tests unitarios**: JUnit 4 — `app/src/test/java/`
- **Tests instrumentados**: AndroidX Test + Espresso — `app/src/androidTest/java/`
- Test runner: `androidx.test.runner.AndroidJUnitRunner`

## Dependencias Clave

Todas gestionadas a través de `gradle/libs.versions.toml`:
- AndroidX AppCompat, Activity, ConstraintLayout
- Material Design Components
- Navigation Component (fragment + ui)
- Retrofit 2 + Gson converter
- OkHttp
- Hilt (Android DI) — annotationProcessor en Java (no kapt)
- Security Crypto (EncryptedSharedPreferences)
- JUnit 4, AndroidX Test JUnit, Espresso (testing)

## Estado de Funcionalidades (al cierre de la branch `fix/reservations-and-activities-bugs`)

| Funcionalidad | Estado | Notas |
|---|---|---|
| Login (usuario/contraseña) | ✅ Funciona | |
| OTP | ✅ Funciona | El código se imprime en consola del server, no hay email real |
| Home — listado de actividades | ✅ Funciona | Filtra actividades pasadas por Instant UTC |
| Detalle de actividad | ✅ Funciona | Reutilizable desde Home, Historial y Mis Actividades con flags |
| Búsqueda de actividades | ✅ Funciona | |
| Formulario de reserva | ⚠️ Parcial | Carga y navega; depende de si `/api/bookings` está activo en Railway |
| Mis reservas | ⚠️ Parcial | Muestra reservas `confirmed`; cancelación depende de Railway |
| Cancelar reserva | ⚠️ Parcial | UI funciona; depende del endpoint en Railway |
| Historial | ✅ Funciona | Clickeable → detalle; botón "Calificar" por reserva |
| Calificaciones | ✅ Funciona | Formulario + read-only + ventana 48hs. Ver `RatingFragment` |
| Perfil | ❌ Sin probar | Sin cambios |
| Favoritos | ❌ Sin implementar | `FavoritesApi` existe, UI no implementada |
| Noticias | ❌ Sin implementar | `NewsApi` existe, UI no implementada |
| Mapa / Punto de encuentro | ❌ Sin implementar | |
| Modo sin conexión | ❌ Sin implementar | |
| Biometría | ❌ Sin implementar | |

## Bugs conocidos / cosas a revisar

- **Reservas con 404**: los endpoints `/api/bookings` pueden devolver 404 si la versión deployada en Railway no tiene `bookings.routes.js` activo. Verificar en Railway antes de cambiar endpoints.
- **Username vacío en Home**: si el usuario navega por el bottom nav en vez de venir del login, el argumento `username` no se pasa. Se intenta recuperar via `GET /api/profile` como fallback, pero depende de que el token sea válido.
- **BottomNavigationView**: actualmente solo muestra la barra en `homeFragment` y `reservasFragment`. Agregar los demás destinos principales al listener en `MainActivity`.
- **Imágenes**: `ActivityAdapter`, `HistorialAdapter` y `ActivityDetailFragment` usan `ivImage.setImageDrawable(null)` como placeholder. Falta integrar Glide para cargar `imageUrl` de las actividades.
- **Historial vacío**: `GET /api/activities/history` retorna solo reservas `finalized`. Si Railway reinició y no hay reservas finalizadas en memoria, el historial aparecerá vacío aunque funcione correctamente.
- **Botones en ListView items**: cualquier `Button` visible dentro de un ítem de `ListView` debe tener `android:focusable="false"` para que el `OnItemClickListener` de la fila funcione. Sin esto, el botón captura el touch y la fila no es presionable.

## Convenciones del proyecto

- Todo el código en **Java** — nunca Kotlin
- Fragments gestionados con **Navigation Component**, no con `FragmentManager` directo (salvo `CancelReservationFragment` que por ahora usa transaction manual)
- Callbacks de Retrofit: siempre verificar `isAdded()` antes de actualizar UI en `onResponse` y `onFailure`
- Logging: usar `Log.e("NombreClase", "mensaje", throwable)` para errores de red — facilita el diagnóstico en logcat
- El `FRONTEND_REFERENCE.md` es la fuente de verdad del backend — actualizarlo si el backend cambia
- **Detalle de actividad reutilizable**: `ActivityDetailFragment` acepta args `showReserveButton` (default `true`) y `showSpotsField` (default `true`). Pasar `false` desde historial o mis reservas para ocultar elementos irrelevantes sin duplicar el fragment.
- **Comparación de fechas**: usar siempre `DateTimeUtils.isFutureOrNow(String)` o comparar `Instant` UTC directamente. Nunca comparar con `LocalDate.now()` contra fechas ISO que incluyen hora — la fecha calendario puede diferir de la hora real por timezone.
- **Botones en ListView**: agregar `android:focusable="false"` a todo `Button` dentro de un `item_*.xml` de ListView para no romper el `OnItemClickListener` de la fila.
