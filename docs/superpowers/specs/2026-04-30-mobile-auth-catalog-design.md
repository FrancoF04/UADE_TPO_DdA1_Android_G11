# Mobile XploreNow — Consignas 1 y 3 — Design Spec

**Fecha**: 2026-04-30
**Repo**: `mobile-app-android` (FrancoF04/UADE_TPO_DdA1_Android_G11)
**Branch base**: `main`
**Alcance**: cierre completo de Consigna 1 (Autenticación y Registro) y Consigna 3 (Catálogo de Actividades) en mobile, más cambios mínimos al backend para destrabar imágenes reales.

---

## Restricciones (no negociables)

- **Universo tecnológico cerrado al PROJECT_CONTEXT (clases 1-7)**:
  - Clase 1: `Activity`, `ListView`, `Service`, `BroadcastReceiver`, `Intents` (explícitos e implícitos), eventos UI.
  - Clase 3: `Fragment`, `Navigation Component`, `Retrofit`, `OkHttp`.
  - Clase 5: `Hilt`, `JWT`/Bearer en header, `TokenManager`.
  - Clase 6: `DataStore`, `Room`, `Files`, `EncryptedSharedPreferences`, `Glide`, galería e imágenes con permisos runtime.
  - Clase 7: `ActivityResultLauncher`, `BiometricManager`, `PromptInfo`, `BiometricPrompt`.
- **APIs base del SDK Android** se asumen disponibles (no son tecnologías nuevas): `Toast`, `AlertDialog`, `DatePickerDialog`, `Switch`, `SeekBar`, `EditText`, `Button`, `LinearLayout`, `HorizontalScrollView`, `TextView`, `ImageView`, `Handler`, `Intent` (todas las acciones), constantes de `Settings`.
- **Java estándar** se asume disponible: `synchronized`, `volatile`, `interface`, threading básico.
- **Explícitamente NO se usa**: Compose, ViewModel, LiveData, Coroutines, Flow, RecyclerView, ViewPager2, WorkManager, BottomSheetDialogFragment, Material Chips/Tabs, Dagger directo (Hilt sí), librerías de testing externas (Mockito, MockK, MockWebServer, Truth, etc.), Maps SDK.
- **Idioma del código y commits**: inglés. Idioma del spec y UI strings: español.

---

## Decisiones tomadas durante el brainstorm

| Tema | Decisión |
|---|---|
| Alcance | Consignas 1 y 3 completas en mobile |
| Imágenes | Unsplash curado por actividad en `data.js` del backend |
| Galería del detalle | Stack vertical de fotos |
| Home | Lista única paginada + chips de filtro (incluyendo "Destacadas" y "Para vos" como chips, no shelves) |
| Filtros avanzados | `FiltersFragment` full-screen (Navigation Component) |
| Biometría opt-in | `AlertDialog` post-login + toggle permanente en Perfil |
| Disparo biometría | Auto-prompt al abrir la app si access vencido + bio activada + refresh válido |
| Sesión | Warm entre cierres si access vigente; refresh transparente con refresh-token vivo durante uso; user+pass obligatorio si refresh expirado o sin biometría |
| Bus de SESSION_EXPIRED | `SessionEventBus` (Java puro + Hilt singleton). NO BroadcastReceiver. |
| Testing | QA manual únicamente. Tests automáticos legacy del repo se mantienen pero no se planifican nuevos en este spec. |

---

## 1. Arquitectura general

### Mapa de paquetes resultante

```
com.example.androidapp/
├── data/
│   ├── local/
│   │   ├── TokenManager.java          [existente — se amplía con biometric_enabled flag]
│   │   └── PreferencesStore.java      [nuevo — cache local de preferences via DataStore]
│   ├── model/                         [existente — sin cambios estructurales]
│   └── remote/                        [existente — verificar AuthApi.resendOtp y AuthApi.refresh]
├── di/
│   └── NetworkModule.java             [modificado — interceptor 401 con refresh transparente]
├── ui/
│   ├── auth/
│   │   ├── LoginFragment.java         [modificado — auto-prompt biométrico en onViewCreated]
│   │   ├── OtpFragment.java           [modificado — botón Reenviar + cooldown 30s]
│   │   └── BiometricOptInDialog.java  [nuevo — AlertDialog post-login]
│   ├── home/
│   │   ├── HomeFragment.java          [modificado — chips, paginación, integración FiltersFragment]
│   │   ├── ActivityAdapter.java       [modificado — Glide]
│   │   ├── ActivityDetailFragment.java[modificado — galería vertical + Glide + botón mapa]
│   │   ├── FiltersFragment.java       [nuevo — full-screen con destino/categoría/fecha/precio]
│   │   └── SearchFragment.java        [existente — refactor para reusar flujo de filtros]
│   ├── historial/                     [sin cambios para este spec]
│   ├── reservation/                   [sin cambios para este spec]
│   └── profile/
│       ├── ProfileFragment.java       [modificado — toggle biometría + acceso a preferencias]
│       ├── EditProfileFragment.java   [existente]
│       └── PreferencesFragment.java   [nuevo — selector multi-choice categorías/destinos]
├── util/
│   ├── DateTimeUtils.java             [existente]
│   ├── BiometricHelper.java           [nuevo — wraps BiometricManager + BiometricPrompt]
│   ├── MapIntentLauncher.java         [nuevo — abre Intent geo: con coordenadas]
│   ├── SessionEventBus.java           [nuevo — interface listener + Hilt singleton]
│   ├── ApiErrorParser.java            [nuevo — extrae mensaje de error del response body]
│   ├── OtpResendCooldown.java         [nuevo — helper puro de cooldown]
│   ├── FilterQueryBuilder.java        [nuevo — convierte Filters a Map<String,String> para Retrofit]
│   └── BiometricCanAuthMapper.java    [nuevo — mapea códigos a enum BiometricStatus]
├── MainActivity.java                  [modificado — entry point que decide login vs home según token vigente]
└── MyApp.java                         [existente]
```

**Recursos nuevos** (en `app/src/main/res/`):
- `drawable/ic_placeholder_activity.xml` — vector drawable (silueta de cámara o paisaje neutro). Usado por Glide en `.placeholder()` y `.error()` en `ActivityAdapter`, `HistorialAdapter`, `ReservationAdapter`, `ActivityDetailFragment`.
- `drawable/chip_background.xml` — selector de fondo para los chips de filtro (estado normal / activo / dashed para CTA).
- `drawable/pill_background.xml` — selector de fondo para las pills de `FiltersFragment` y `PreferencesFragment`.

### Tradeoffs aceptados

- **Sin shelves horizontales en el Home**: la consigna pide "Sección de actividades destacadas o recomendadas". Se cumple funcionalmente con chips que filtran la lista. Es interpretación sostenible.
- **Galería vertical empuja la info de la actividad hacia abajo**: aceptado. Compensar con scroll suave; precio y botón reservar quedan visibles en el footer del detalle.
- **TTL del access token = 60 min en backend**: durante uso continuo la app va a recibir 401 con regularidad. El interceptor hace refresh transparente para no patear al usuario.

---

## 2. Cambios en el backend

### 2.1 Reemplazo de URLs en `src/data/data.js`

**Estado actual**: todas las URLs son `https://images.example.com/...` (no funcionan, retornan DNS fail / 404).

**Estado objetivo**: URLs públicas de Unsplash con photo IDs específicos:

```
https://images.unsplash.com/photo-{ID}?w=1200&q=80&auto=format&fit=crop
```

- `auto=format`: Glide recibe WebP en clientes que lo soportan.
- `fit=crop`: recorte sin deformar.
- `w=1200`: ancho razonable para galería full-width (ahorra ancho de banda).

**Política**:
- 15 actividades × 5 fotos = 75 URLs (1 `imageUrl` + 4 `galleryUrls` por actividad).
- 3 noticias × 1 `image` = 3 URLs adicionales.
- Total: ~78 URLs curadas.

**Cómo se eligen**: para cada actividad, buscar en Unsplash por keyword principal (ej. "san telmo buenos aires", "perito moreno glaciar", "mendoza wine vineyard"). Tomar 5 fotos representativas (panorámica, cercana, gente, paisaje, detalle). Hardcodear los IDs en `data.js`.

**Trabajo estimado**: 30-45 min de curaduría manual.

### 2.2 Endpoints que ya existen y NO se modifican

Verificado contra `auth.routes.js`, `activity.routes.js`, `profile.routes.js`:

- `POST /api/auth/otp/{request,verify,resend}` ✅
- `POST /api/auth/{login,refresh,logout,register}` ✅
- `GET /api/activities` con `page`, `limit`/`page_size`, `destination`, `category`, `date`, `priceMin`, `priceMax` ✅
- `GET /api/activities/{featured,recommended,filters}` ✅
- `GET /api/activities/:id` con `meetingPoint{lat,lng,address}`, `galleryUrls`, `guide`, `cancellationPolicy` ✅
- `GET /api/profile/preferences` y `PUT /api/profile/preferences` ✅

### 2.3 Lo que NO se hace en backend (boundary)

- NO se cambia OTP a email real (queda en `console.log` por decisión explícita del usuario).
- NO se migra a Postgres.
- NO se agrega `?q=` para búsqueda por texto (no está en consigna).
- NO se firma JWT.
- NO se agrega rate limiting.

### 2.4 Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Unsplash cambia un photo ID | Glide muestra `R.drawable.ic_placeholder_activity` via `.error()` |
| Hotlinking sin API key | URLs `images.unsplash.com/photo-{id}` son públicas y libres de uso según ToS |
| Datos en memoria del backend se pierden si Railway reinicia | Las URLs son parte de `data.js` que se recarga al boot — sobreviven |

---

## 3. Componentes mobile en detalle

### A. Auth flow

#### `MainActivity.java` (modificado)

- **Responsabilidad**: decidir el destino inicial al abrir la app. Lee `TokenManager` y elige entre 4 estados (ver flujo 4.1).
- **Primitivas**: `Activity` (Clase 1), `NavController.setGraph()` con `startDestination` dinámico (Clase 3).
- **Adicionalmente**: se registra como `SessionExpiredListener` en `onResume` y se desregistra en `onPause`. Cuando recibe el callback hace `runOnUiThread` y `navController.navigate(R.id.loginFragment)` con `forceUserPass=true`.
- **No hace**: networking, decisiones de UI complejas. Solo despacha al fragment correcto.

#### `data/local/TokenManager.java` (extendido)

- **Existente**: persiste `accessToken` y `refreshToken` cifrados (`EncryptedSharedPreferences`, Clase 6).
- **Se agrega**:
  - Campos: `biometricEnabled: boolean` (default false), `biometricOptInDismissed: boolean` (default false), `accessExpiresAt: long`, `refreshExpiresAt: long`.
  - Métodos: `isBiometricEnabled()`, `setBiometricEnabled(boolean)`, `isBiometricOptInDismissed()`, `setBiometricOptInDismissed(boolean)`, `isAccessTokenValid()`, `isRefreshTokenValid()` (comparan con `System.currentTimeMillis()`), `clear()` (borra todo excepto `biometricOptInDismissed`).
- **No hace**: validación criptográfica del token (no es JWT firmado), networking.

#### `util/BiometricHelper.java` (nuevo)

- **Responsabilidad**: encapsular `BiometricManager.canAuthenticate()` y `BiometricPrompt.authenticate()`.
- **API pública**:
  - `BiometricStatus checkAvailability()` → enum `{ AVAILABLE, NOT_ENROLLED, NO_HARDWARE, UNAVAILABLE }`
  - `void promptForAuth(FragmentActivity, OnSuccess, OnError)` → construye `PromptInfo` con `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`, dispara prompt
  - `Intent enrollIntent()` → devuelve `Intent(Settings.ACTION_BIOMETRIC_ENROLL)` con extras `BIOMETRIC_AUTHENTICATORS_ALLOWED`
- **Primitivas**: Clase 7.
- **No hace**: persistir flags, decidir flow de re-login.

#### `util/SessionEventBus.java` (nuevo) — reemplaza BroadcastReceiver

- **Responsabilidad**: bus interno para notificar `SESSION_EXPIRED` desde el interceptor a los componentes UI sin acoplarlos directamente.
- **Implementación**: clase Java `@Singleton` (Hilt, Clase 5). Mantiene una lista sincronizada (`Collections.synchronizedList`) de `SessionExpiredListener`. Métodos `register(listener)`, `unregister(listener)`, `notifySessionExpired()` (itera y llama callbacks).
- **Primitivas**: Hilt (Clase 5), Java estándar.
- **Por qué no `BroadcastReceiver`**: la cátedra enseñó `BroadcastReceiver` para eventos del sistema (`BOOT_COMPLETED` en `AndroidManifest.xml`), no como bus de mensajes interno. Mantenemos el uso strict de Clase 1 a su ejemplo enseñado.

#### `di/NetworkModule.java` (modificado)

- **Existente**: provee `Retrofit`, `OkHttpClient`, todas las APIs.
- **Se agrega**: dentro del `OkHttpClient` builder, un nuevo `Interceptor` "AuthRefreshInterceptor" — ver flujo 4.3 para el algoritmo completo.
- **Inyección**: el interceptor recibe `TokenManager` y `SessionEventBus` por constructor.
- **Primitivas**: `OkHttp Interceptor` (Clase 3), Hilt `@Provides` (Clase 5).

#### `ui/auth/LoginFragment.java` (modificado)

- **Existente**: pantalla con secciones para OTP y user+pass.
- **Se agrega**:
  - En `onViewCreated`, si `getArguments().getBoolean("autoPromptBiometric")`, dispara `BiometricHelper.promptForAuth()`. En `onSuccess` → `authApi.refresh(refreshToken)` → guarda nuevos tokens → `navController.navigate(R.id.action_login_to_home)`. En `onError`/cancel → deja la pantalla normal con todos los caminos visibles.
  - Si `getArguments().getBoolean("forceUserPass")`, oculta el botón "Ingresar con OTP" y muestra `TextView` "Tu sesión expiró, ingresá con tu usuario y contraseña" arriba del form.
- **Primitivas**: `Fragment` con argumentos vía `Bundle` (Clase 3), `BiometricPrompt` via helper (Clase 7).

#### `ui/auth/OtpFragment.java` (modificado)

- **Se agrega**: `Button` "Reenviar código" debajo del input. `setOnClickListener` (Clase 1) llama `authApi.resendOtp(email)`. Tras click, deshabilita el botón y muestra countdown ("Reenviar (29s)... 28s... etc.") usando `Handler.postDelayed` con un loop manual. `OtpResendCooldown` calcula los segundos restantes para sobrevivir rotación.
- **Primitivas**: `setOnClickListener`, `Handler` (Clase 1).

#### `ui/auth/BiometricOptInDialog.java` (nuevo)

- **Responsabilidad**: `AlertDialog` simple. Se invoca solo después de un login exitoso fresco si:
  1. `BiometricHelper.checkAvailability() == AVAILABLE`
  2. `tokenManager.isBiometricOptInDismissed() == false`
  3. `tokenManager.isBiometricEnabled() == false`
- **Acciones**:
  - "Activar" → `tokenManager.setBiometricEnabled(true)` + `Toast` "Listo"
  - "Ahora no" → `tokenManager.setBiometricOptInDismissed(true)` (no vuelve a aparecer; sigue accesible desde Perfil)
- **Primitivas**: `AlertDialog` (SDK base de Android).

### B. Catálogo

#### `ui/home/HomeFragment.java` (modificado)

- **Cambios**:
  1. Encima del `ListView`, un `HorizontalScrollView` con un `LinearLayout` horizontal de chips (Buttons con drawable de fondo redondeado): **"⭐ Destacadas"**, **"💙 Para vos"**, **"📍 Todas"** (default activo), **"⚙ Filtros"**.
  2. Click en chip → cambia el endpoint llamado:
     - **Destacadas** → `activityApi.getFeatured()`
     - **Para vos** → `activityApi.getRecommended()` (si `PreferencesStore` tiene categories y destinations vacíos → muestra CTA "Configurá tus preferencias en Perfil" sin pegarle al server)
     - **Todas** → `activityApi.getActivities(page, page_size, ...filters)`
     - **Filtros** → `navController.navigate(R.id.action_home_to_filters)`, recibe resultado vía `setFragmentResultListener`
  3. Paginación scroll-infinito: `setOnScrollListener` con `AbsListView.OnScrollListener`. Cuando `firstVisibleItem + visibleItemCount >= totalItemCount - 3` y `!loading` y `adapter.size() < meta.total`, llama `loadPage(page+1)` con append.
- **Primitivas**: `ListView` + `OnScrollListener` (Clase 1), `Navigation` + `setFragmentResult` (Clase 3), Retrofit (Clase 3).

#### `ui/home/ActivityAdapter.java` (modificado)

- **Cambio único**: en `getView`, reemplazar `ivImage.setImageDrawable(null)` por:

```java
Glide.with(holder.itemView)
    .load(activity.getImageUrl())
    .placeholder(R.drawable.ic_placeholder_activity)
    .error(R.drawable.ic_placeholder_activity)
    .centerCrop()
    .into(holder.ivImage);
```

- **Primitiva**: Glide (Clase 6).
- **Aplicar también a**: `HistorialAdapter` y `ReservationAdapter` (~3 líneas cada uno; complementario aunque fuera del scope estricto, evita inconsistencia visual).

#### `ui/home/ActivityDetailFragment.java` (modificado)

- **Cambios**:
  1. **Galería vertical**: en el layout, debajo de la imagen hero, un `LinearLayout` vertical (id `galleryContainer`) inicialmente vacío. En `onViewCreated`, después de recibir la actividad, iterar `activity.getGalleryUrls()` y por cada URL inflar un `ImageView` con `LayoutParams(MATCH_PARENT, 220dp)` y `marginTop=8dp`, agregarlo al container y cargarlo con Glide. Tap en imagen → `Intent(ACTION_VIEW, Uri.parse(url))` (Clase 1).
  2. **Glide en imagen hero**: igual que adapter.
  3. **Botón "Ver en mapa"** debajo del texto de meeting point: `setOnClickListener` → `mapIntentLauncher.openMap(lat, lng, address)`.
- **Primitivas**: `Fragment` (Clase 3), `LinearLayout` dinámico (Clase 1), Glide (Clase 6), `Intent` implícito (Clase 1).

#### `util/MapIntentLauncher.java` (nuevo)

- **Responsabilidad**: abrir el app de mapas del sistema con `Intent(ACTION_VIEW, Uri.parse("geo:{lat},{lng}?q={Uri.encode(address)}"))`. Si `geoIntent.resolveActivity(context.getPackageManager()) == null` → `Toast` "No tenés un app de mapas instalado".
- **Primitivas**: `Intent` implícito + `ACTION_VIEW` (Clase 1).
- **No hace**: usar Maps SDK (no habilitado), fallback a navegador.

#### `ui/home/FiltersFragment.java` (nuevo)

- **Responsabilidad**: pantalla full-screen con 4 secciones:
  - **Destino**: pills clickeables. Datos: `GET /api/activities/filters` → `destinations[]`.
  - **Categoría**: 5 pills fijas: `free_tour`, `guided_visit`, `excursion`, `gastronomic`, `adventure`.
  - **Fecha**: pill "📅 Cualquier día" / "📅 10/04/2026". Tap abre `DatePickerDialog` (SDK base).
  - **Precio**: 2 `EditText` con `inputType="number"` para min y max. Texto vivo "$0 — $35.000".
- **Footer**: `Button` "Aplicar (N actividades)". El N se actualiza en vivo cada vez que cambia un filtro, llamando a `GET /api/activities` con `page=1&page_size=1` para leer `meta.total` (poll económico, request mínimo).
- **Tap "Aplicar"** → `setFragmentResult("filters", bundle)` y `navController.popBackStack()`.
- **Primitivas**: `Fragment` (Clase 3), `LinearLayout`, `EditText`, `DatePickerDialog` (SDK base).

### C. Perfil / Preferencias

#### `ui/profile/ProfileFragment.java` (modificado)

- **Se agregan 2 ítems**:
  1. **"Ingresar con huella"** con `Switch`. Estado inicial = `tokenManager.isBiometricEnabled()`. Listener:
     - OFF → ON: dispara `biometricHelper.promptForAuth()`. Solo si succeed, `setBiometricEnabled(true)`. Si falla, el switch vuelve a OFF y `Toast`.
     - ON → OFF: `setBiometricEnabled(false)` directo.
     - Si `checkAvailability() == NO_HARDWARE` → switch deshabilitado con texto "Tu dispositivo no soporta biometría".
     - Si `checkAvailability() == NOT_ENROLLED` → tap dispara `AlertDialog` "Necesitás enrolar tu huella" con botón → `Intent(Settings.ACTION_BIOMETRIC_ENROLL)`.
  2. **"Mis preferencias"** → navega a `PreferencesFragment`.
- **Primitivas**: `Switch` (SDK base), `Fragment` + `Navigation` (Clase 3).

#### `ui/profile/PreferencesFragment.java` (nuevo)

- **Responsabilidad**: selector multi-choice:
  - **Categorías** (5 fijas): `free_tour`, `guided_visit`, `excursion`, `gastronomic`, `adventure`.
  - **Destinos** (6 fijas): `Buenos Aires`, `Bariloche`, `Mendoza`, `Ushuaia`, `Córdoba`, `Salta`.
- En `onViewCreated`: `GET /api/profile/preferences`, marca pills correspondientes.
- Botón "Guardar": `PUT /api/profile/preferences` con `{ categories: [...], destinations: [...] }`. Después actualiza `PreferencesStore` (DataStore local).
- **Primitivas**: `Fragment` (Clase 3), Retrofit (Clase 3), pills clickeables (`LinearLayout` + Buttons, Clase 1).

#### `data/local/PreferencesStore.java` (nuevo)

- **Responsabilidad**: cache local de `categories[]`, `destinations[]`, `lastSyncedAt`. Usado por `HomeFragment` para decidir el comportamiento del chip "Para vos".
- **Primitiva**: `DataStore` Preferences (Clase 6).
- **No hace**: ser fuente de verdad — el server lo es. Solo cache.

### Boundary explícito (lo que NO se hace en este spec)

- **Favoritos** (`/api/favorites`): backend lo tiene, mobile lo deja sin UI. No es consigna 1 ni 3.
- **Noticias** (`/api/news`): mismo caso.
- **Reservas, Historial, Calificaciones**: ya implementadas con bugs menores. No son consigna 1 ni 3.
- **Modo offline / sync bundle**: fuera de scope.
- **Mapa embebido**: solo `Intent` al app del sistema.

---

## 4. Data flow

### 4.1 Boot de la app

```
Usuario abre la app
   │
   ▼
MainActivity.onCreate
   │
   ▼
TokenManager.read()
   │
   ├─ accessToken == null              → LoginFragment (sin args)
   │
   ├─ now < accessExpiresAt            → HomeFragment (sesión warm)
   │
   ├─ accessExpired && refreshValid
   │    ├─ biometricEnabled == true    → LoginFragment(autoPromptBiometric=true)
   │    │      │
   │    │      ▼ (en LoginFragment.onViewCreated)
   │    │   biometricHelper.promptForAuth()
   │    │      │
   │    │      ├─ onSuccess → authApi.refresh() → save tokens → navigate(home)
   │    │      └─ onError/cancel → mostrar LoginFragment normal con todos los caminos
   │    │
   │    └─ biometricEnabled == false   → LoginFragment(forceUserPass=true) con mensaje "Tu sesión expiró"
   │
   └─ refreshExpired                   → LoginFragment(forceUserPass=true) con mensaje "Tu sesión expiró"
```

**Invariantes**:
- `MainActivity` no hace ningún request HTTP. Decisión 100% local con `TokenManager`.
- Si hay duda (clock skew), cae al lado conservador → más prompts, menos sesiones colgadas.

### 4.2 Login user+pass + opt-in biométrico

```
LoginFragment
   │ click "Ingresar"
   ▼
authApi.login(username, password)
   │
   ├─ onResponse 200 con { token, refreshToken }
   │      │
   │      ▼
   │   tokenManager.save(token, refreshToken, expiresAt, refreshExpiresAt)
   │      │
   │      ▼
   │   ¿biometricHelper.checkAvailability() == AVAILABLE
   │     && !tokenManager.isBiometricOptInDismissed()
   │     && !tokenManager.isBiometricEnabled()?
   │      │
   │      ├─ sí → BiometricOptInDialog.show()
   │      │       ├─ "Activar"   → setBiometricEnabled(true) + Toast
   │      │       └─ "Ahora no"  → setBiometricOptInDismissed(true)
   │      │
   │      └─ no → skip
   │      │
   │      ▼
   │   navController.navigate(R.id.action_login_to_home)
   │
   ├─ onResponse 401 → Toast "Credenciales inválidas"
   │
   └─ onFailure → Toast "Sin conexión, reintentá"
```

**Invariantes**:
- El opt-in solo aparece después de un login exitoso fresco. Auto-refresh biométrico no dispara opt-in.
- `biometricOptInDismissed` y `biometricEnabled` son flags separadas. Un usuario que dijo "Ahora no" no ve más el dialog automático, pero puede activar desde Perfil.

### 4.3 Refresh transparente durante uso

```
[Cualquier llamada Retrofit autenticada — ej. activityApi.getActivities(page=2)]
   │
   ▼
OkHttp ejecuta la chain de interceptors → AuthRefreshInterceptor.intercept(chain)
   │
   ▼
   request1 = chain.request().newBuilder()
       .addHeader("Authorization", "Bearer " + tokenManager.getAccessToken())
       .build()
   response1 = chain.proceed(request1)
   │
   ├─ response1.code != 401 → return response1
   │
   └─ response1.code == 401
         │
         ▼
      synchronized(refreshLock):
         │
         ▼
      ¿otro thread refrescó mientras esperaba el lock? (compare token actual vs el de request1)
         │
         ├─ sí → reintentar request1 con el nuevo token y return
         │
         └─ no
            │
            ▼
         ¿tokenManager.isRefreshTokenValid()?
            │
            ├─ no → sessionEventBus.notifySessionExpired() + return response1
            │
            └─ sí
               │
               ▼
            response_refresh = blocking call POST /api/auth/refresh con refreshToken
               │
               ├─ 200 con { token, refreshToken } nuevos
               │     │
               │     ▼
               │  tokenManager.save(...)
               │  request2 = request1.newBuilder()
               │      .header("Authorization", "Bearer " + newAccessToken)
               │      .build()
               │  return chain.proceed(request2)
               │
               └─ 401/4xx → tokenManager.clear() + sessionEventBus.notifySessionExpired() + return response1
```

**Cómo se entera la UI**:
- `MainActivity` (única `Activity`) implementa `SessionExpiredListener` y se registra en `SessionEventBus` en `onResume`. Desregistra en `onPause`.
- En el callback: `runOnUiThread(() -> navController.popBackStack(R.id.loginFragment, false))` con `forceUserPass=true`.

**Invariantes**:
- El interceptor reintenta una sola vez. Si el segundo request también es 401, sale.
- El lock evita N refreshes paralelos cuando hay N requests concurrentes con 401.
- El interceptor corre en threads de OkHttp (no UI). Bloquear ahí es válido.

### 4.4 Carga del Home con chips y filtros

```
HomeFragment.onViewCreated
   │
   ▼
1. Cargar chips (estáticos: Destacadas / Para vos / Todas / Filtros)
2. Cargar preferencias desde PreferencesStore (DataStore)
   │   └─ si categorías y destinos vacíos → chip "Para vos" muestra CTA al ser tocado
3. Aplicar chip default "Todas"
   │
   ▼
loadPage(page=1, filters=current)
   │
   ▼
   activityApi.getActivities(page, page_size=10, ...filters)
   │
   ├─ onResponse 200 → adapter.replace(items); meta.total guardado; loading = false
   │
   └─ onFailure → Toast "Sin conexión" + adapter vacío + botón "Reintentar"

[Usuario scrollea cerca del final]
   │
   ▼
OnScrollListener detecta firstVisibleItem + visibleItemCount >= totalItemCount - 3
   │
   ▼
¿!loading && adapter.size() < meta.total?
   │
   └─ sí → loadPage(page=N+1, filters=current) con adapter.append(items)

[Usuario tap chip "Para vos"]
   │
   ▼
¿preferencesStore tiene categorías y destinos no vacíos?
   │
   ├─ no → mostrar TextView "Configurá tus preferencias en Perfil"
   │
   └─ sí → activityApi.getRecommended() → adapter.replace(items)

[Usuario tap chip "Filtros"]
   │
   ▼
navController.navigate(R.id.action_home_to_filters, currentFilters as Bundle)
   │
   ▼
[FiltersFragment: usuario aplica → setFragmentResult + popBackStack]
   │
   ▼
HomeFragment recibe via setFragmentResultListener → currentFilters = updated
   │
   ▼
loadPage(page=1, currentFilters) con replace
```

**Invariantes**:
- Cambio de chip siempre resetea page=1 y reemplaza la lista.
- Filtros viajan como `Bundle` para sobrevivir rotación.
- Si "Para vos" se elige sin preferencias → no se hace request al server, CTA directo.

---

## 5. Error handling

Política general: **fallar visiblemente con mensaje accionable**, nunca silenciosamente. `try/catch` solo en bordes (network, permisos, intents externos).

### 5.1 Network (Retrofit `onFailure`)

| Donde | Política |
|---|---|
| Cualquier fragment con request HTTP | `Toast` "Sin conexión, reintentá". NO crash, NO retry automático. |
| Lista del Home falla en page=1 | `TextView` central "No pudimos cargar las actividades" + botón "Reintentar" → `loadPage(1)` |
| Lista del Home falla en page>1 | `Toast`, `loading=false`, próximo scroll permite reintentar. La lista cargada NO se rompe. |
| Detalle de actividad falla | `popBackStack()` + `Toast` "No pudimos cargar el detalle" |
| Login user+pass o OTP verify falla por red | `Toast`. Form mantiene los datos para reintentar. |
| `POST /api/auth/refresh` falla por red en interceptor | El interceptor devuelve 401 original. `MainActivity` recibe `SessionEventBus` y manda a Login con `forceUserPass=true`. (Trade-off: si server momentáneamente caído, usuario re-loggea — alternativa sería cachear y reintentar más tarde, fuera de scope). |

### 5.2 Errores HTTP de aplicación (4xx / 5xx con body)

Backend devuelve `{ "success": false, "error": "Mensaje" }`. Convención mobile:

| Código | UX |
|---|---|
| 400 (validación) | Mostrar el `error` del response como `Toast` o `TextView` debajo del campo. |
| 401 (token) | Manejado por interceptor. UI no debería ver 401 directo salvo refresh también haya fallado. |
| 404 | "No encontrado, refrescá la pantalla". En detalle → `popBackStack`. |
| 409 (negocio) | Mostrar mensaje del server tal cual (`Toast` o `AlertDialog`). |
| 500+ | "Algo falló del lado del servidor, reintentá en unos segundos". |

**Helper**: `util/ApiErrorParser.java` con `String extractMessage(Response<?>)` — toma el body de error y devuelve el `error` string, fallback genérico si no parsea.

### 5.3 Biometría — casos del `BiometricManager.canAuthenticate()`

| Resultado | Mobile hace |
|---|---|
| `BIOMETRIC_SUCCESS` | Flujo normal: dialog opt-in tras login, auto-prompt, toggle visible. |
| `BIOMETRIC_ERROR_NO_HARDWARE` | Toggle Perfil deshabilitado con texto "Tu dispositivo no soporta biometría". Dialog opt-in NO se muestra. |
| `BIOMETRIC_ERROR_HW_UNAVAILABLE` | Tratar como `NO_HARDWARE`. |
| `BIOMETRIC_ERROR_NONE_ENROLLED` | Toggle habilitado pero al activarlo `AlertDialog` "Necesitás enrolar tu huella en el sistema. ¿Vamos a Configuración?" → `Intent(Settings.ACTION_BIOMETRIC_ENROLL)`. |
| `BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED` | Tratar como `NO_HARDWARE`. Mensaje "Tu dispositivo necesita actualización de seguridad". |

### 5.4 `BiometricPrompt` callbacks durante el auto-prompt al abrir la app

| Callback | Acción |
|---|---|
| `onAuthenticationSucceeded` | `authApi.refresh(refreshToken)` → save tokens → navegar a Home. |
| `onAuthenticationFailed` (huella no reconocida una vez) | El sistema reintenta solo. No hacemos nada. |
| `onAuthenticationError(BIOMETRIC_ERROR_USER_CANCELED)` | NO mandar a user+pass automático. Dejar `LoginFragment` con todos los caminos visibles. |
| `onAuthenticationError(BIOMETRIC_ERROR_LOCKOUT)` | `Toast` "Demasiados intentos, esperá unos minutos o usá tu contraseña". Mantener LoginFragment. |
| `onAuthenticationError(BIOMETRIC_ERROR_LOCKOUT_PERMANENT)` | `Toast` "Desbloqueá tu dispositivo y reintentá". Mantener LoginFragment. |
| `onAuthenticationError(otro)` | `Log.e("LoginFragment", "biometric error", code)` + `Toast` genérico. Mantener LoginFragment. |

### 5.5 Glide — fallo al cargar imagen

| Caso | Visual |
|---|---|
| URL devuelve 404 | Glide muestra automáticamente el drawable de `.error()` → `R.drawable.ic_placeholder_activity` |
| Sin conexión | Idem |
| URL es null | `.placeholder()` se queda visible |

No se intenta cache custom ni swap de URLs. Glide cachea memoria + disco automáticamente.

### 5.6 `MapIntentLauncher` — sin app de mapas

```java
Intent geoIntent = new Intent(Intent.ACTION_VIEW,
    Uri.parse("geo:" + lat + "," + lng + "?q=" + Uri.encode(address)));
if (geoIntent.resolveActivity(context.getPackageManager()) != null) {
    context.startActivity(geoIntent);
} else {
    Toast.makeText(context, "No tenés un app de mapas instalado", Toast.LENGTH_SHORT).show();
}
```

### 5.7 Filtros sin resultados

`HomeFragment` con filtros aplicados que devuelven lista vacía → `TextView` central "No encontramos actividades con esos filtros. Probá ajustarlos" + botón "Limpiar filtros" → `currentFilters.reset()` + `loadPage(1)`.

### 5.8 Lo que NO se maneja explícitamente

- **Modo offline al abrir la app**: no hay cache estructurado más allá de Glide. El usuario verá toasts de "Sin conexión".
- **Tokens corruptos en `EncryptedSharedPreferences`**: si `decrypt` lanza, se trata como token inválido y se borra → flujo de login normal.
- **Server clock drift**: si `expiresAt` del server difiere del device clock, podemos creer que un token sigue vivo cuando expiró. El interceptor 401 lo absorbe.

---

## 6. QA manual

PROJECT_CONTEXT no incluye testing automatizado. El repo `mobile-app-android` ya tiene JUnit 4 y Espresso configurados (referenciado en `CLAUDE.md`) por el scaffold inicial — se mantienen pero no se planifican tests automatizados nuevos en este spec porque la cátedra no enseñó la disciplina.

### Checklist pre-merge a `main`

Correr en device físico (no emulador) con cuenta limpia.

#### Auth (Consigna 1)

- [ ] Cold start con tokens limpios → ver Login
- [ ] Login user+pass válido → ver dialog opt-in biometría
- [ ] Activar biometría desde dialog → cerrar app → reabrir → ver auto-prompt → entrar a Home
- [ ] Cerrar app con sesión warm (<1h) → reabrir → entrar directo a Home (sin prompt)
- [ ] Reabrir app después de >1h con bio activada → auto-prompt → refresh transparente → Home
- [ ] Reabrir app después de >7d (refresh expirado) → forzar user+pass + mensaje "tu sesión expiró"
- [ ] OTP request → ver código en logs Railway → verify → entrar
- [ ] OTP resend → cooldown 30s en botón
- [ ] Toggle bio en Perfil OFF → cerrar → reabrir → ver Login user+pass (sin auto-prompt)
- [ ] Login en device sin biometría enrolada → toggle Perfil greyed con mensaje
- [ ] Login en device con biometría no enrolada (`NONE_ENROLLED`) → toggle abre `Settings.ACTION_BIOMETRIC_ENROLL`

#### Catálogo (Consigna 3)

- [ ] Home → ver imágenes reales cargando (no placeholders)
- [ ] Detalle → galería vertical con 4-5 fotos visibles scrolleando
- [ ] Tap en foto de galería → abre visor del sistema
- [ ] Detalle → "Ver en mapa" → abre Google Maps con pin en coords correctas
- [ ] Detalle en device sin app de mapas → toast "No tenés un app de mapas instalado"
- [ ] Filtros → seteo destino + categoría + precio max → lista cambia
- [ ] Filtros → fecha específica → solo aparecen actividades con esa fecha en `dates[]`
- [ ] Filtros sin resultados → mensaje + botón "Limpiar filtros"
- [ ] Scroll en Home hasta el final → carga page 2 (verificar logcat o más items)
- [ ] Chip "Para vos" sin preferencias configuradas → CTA "Configurá tus preferencias"
- [ ] Configurar preferencias en Perfil → guardar → chip "Para vos" trae lista no vacía
- [ ] Chip "Destacadas" → solo actividades con `featured=true`
- [ ] Modo avión durante la app → toasts "Sin conexión" en lugar de crashes

#### Validación de cambios al backend

- [ ] `git diff src/data/data.js` muestra ~78 URLs reemplazadas (15 imageUrl + ~60 galleryUrls + 3 news.image)
- [ ] Cada URL responde 200 a `curl -I` (smoke test corre una vez antes del deploy a Railway)
- [ ] Response de `GET /api/activities/a1` contiene URLs de Unsplash, no `images.example.com`

---

## 7. Lo que queda fuera del spec (resumen consolidado)

- Favoritos (UI mobile)
- Noticias (UI mobile)
- Bug fixes en Reservas, Historial, Calificaciones (existentes pero no son consignas 1 ni 3)
- Modo offline / sync bundle
- Mapa embebido (solo Intent al sistema)
- Búsqueda por texto (`?q=`)
- Migración de backend a Postgres
- Envío real de OTP por email
- JWT firmado
- Rate limiting en backend
- Tests automatizados nuevos (la cátedra no los enseñó)

---

## 8. Próximos pasos

1. **Self-review** del spec (placeholders, contradicciones, ambigüedad, scope).
2. **Review del usuario** sobre este documento.
3. **Commit** del spec a `main` del repo mobile.
4. **Skill `writing-plans`** para generar el plan de implementación detallado paso a paso a partir de este spec.
