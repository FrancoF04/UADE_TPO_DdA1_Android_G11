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
│   └── remote/        Interfaces Retrofit (ActivityApi, AuthApi, UserApi, etc.)
├── di/                NetworkModule (provee Retrofit, OkHttp, todas las APIs)
├── ui/
│   ├── auth/          Login, OTP, Register fragments
│   ├── historial/     HistorialFragment + HistorialAdapter
│   ├── home/          HomeFragment, ActivityDetailFragment, SearchFragment, ProfileFragment, EditProfileFragment
│   └── reservation/   MisReservasFragment, ReservationFormFragment, CancelReservationFragment, ReservationAdapter
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

### Endpoints de reservas — estado a verificar

Los endpoints bajo `/api/bookings` (`POST /api/bookings`, `DELETE /api/bookings/:id`) devolvían 404 al momento de este análisis. Actualmente `UserApi` apunta a `/api/users/reservations` como alias. **Verificar en Railway si `bookings.routes.js` está deployado** antes de cambiar los endpoints. El archivo `Backend/src/app.js` local tiene las rutas registradas pero puede no coincidir con la versión deployada.

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

## Estado de Funcionalidades (al cierre de la branch `fix/multiple-endpoints-fix`)

| Funcionalidad | Estado | Notas |
|---|---|---|
| Login (usuario/contraseña) | ✅ Funciona | |
| OTP | ✅ Funciona | El código se imprime en consola del server, no hay email real |
| Home — listado de actividades | ✅ Funciona | Requirió fix de `MeetingPointAdapter` |
| Detalle de actividad | ✅ Funciona | |
| Búsqueda de actividades | ✅ Funciona | |
| Formulario de reserva | ⚠️ Parcial | Carga y navega, pero el endpoint del backend devuelve 404 |
| Mis reservas | ⚠️ Parcial | Igual — problema de endpoint en backend |
| Cancelar reserva | ⚠️ Parcial | Igual — problema de endpoint en backend |
| Historial | ⚠️ Sin probar | Modelo corregido para mapear respuesta real del backend |
| Perfil | ❌ Sin probar | Sin cambios |
| Favoritos | ❌ Sin implementar | `FavoritesApi` existe, UI no implementada |
| Noticias | ❌ Sin implementar | `NewsApi` existe, UI no implementada |
| Mapa / Punto de encuentro | ❌ Sin implementar | |
| Modo sin conexión | ❌ Sin implementar | |
| Biometría | ❌ Sin implementar | |
| Calificaciones | ❌ Sin implementar | `RatingsApi` existe, UI no implementada |

## Bugs conocidos / cosas a revisar

- **Reservas con 404**: el endpoint `/api/users/reservations` (alias de `/api/bookings`) devuelve 404. Verificar si el backend deployado en Railway tiene las rutas de bookings activas.
- **Username vacío en Home**: si el usuario navega por el bottom nav en vez de venir del login, el argumento `username` no se pasa y aparece "Bienvenido, !". Habría que leerlo del token o de una llamada a `/api/profile`.
- **BottomNavigationView**: actualmente solo muestra la barra en `homeFragment` y `reservasFragment`. Agregar los demás destinos principales al listener en `MainActivity`.
- **Imágenes**: `ActivityAdapter` y `ActivityDetailFragment` tienen `ivImage.setImageDrawable(null)` como placeholder. Falta integrar Glide (habilitado por la cátedra en Clase 6) para cargar `imageUrl` de las actividades.
- **Historial**: el endpoint `GET /api/activities/history` retorna solo actividades `finalized`. Si el backend no tiene reservas finalizadas (datos en memoria, server reiniciado), el historial aparecerá vacío aunque funcione correctamente.

## Convenciones del proyecto

- Todo el código en **Java** — nunca Kotlin
- Fragments gestionados con **Navigation Component**, no con `FragmentManager` directo (salvo `CancelReservationFragment` que por ahora usa transaction manual)
- Callbacks de Retrofit: siempre verificar `isAdded()` antes de actualizar UI en `onResponse` y `onFailure`
- Logging: usar `Log.e("NombreClase", "mensaje", throwable)` para errores de red — facilita el diagnóstico en logcat
- El `FRONTEND_REFERENCE.md` es la fuente de verdad del backend — actualizarlo si el backend cambia
