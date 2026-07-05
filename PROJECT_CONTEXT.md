# PROJECT_CONTEXT.md
**Versión:** 1.8.0  
**Última actualización:** Clase 8 — Cámara (QR) y Notificaciones mediante Long Polling (Android Nativo)  
**Contexto académico:** Desarrollo de Apps I — UADE 2026, 1er Cuatrimestre

> **IMPORTANTE para IAs:** Este archivo define las tecnologías habilitadas, patrones requeridos y restricciones impuestas por la cátedra. No sugerir ni implementar alternativas fuera de lo aquí definido sin consultar al usuario primero. Este archivo crece con el avance de las clases — nunca eliminar entradas existentes.

---

# Prácticas de desarrollo requeridas

- Ramas por funcionalidad: prefijos `feature/` y `fix/`
- Integración a `main` únicamente mediante Pull Requests (no se permiten pushes directos)
- Cada integrante debe tener al menos una PR mergeada por entrega

---

# Herramientas habilitadas

- **Android Studio** — IDE principal
- **scrcpy** — visualizar y controlar dispositivo Android desde la PC
- **Visual Studio Code** — editor alternativo

---

# Clase 1 — Primeros pasos en Android

## Configuración

- **AndroidManifest.xml**
  - Declaración de Activities
  - Services
  - BroadcastReceivers
  - ContentProviders
  - Permisos (`INTERNET`, `ACCESS_FINE_LOCATION`, etc.)
  - Activity principal
  - Metadatos globales

- **Gradle**
  - Agregar dependencias desde `build.gradle (Module: app)`

## Componentes habilitados

### Activity

- Ciclo de vida completo
- `onCreate()`
- `onStart()`
- `onResume()`
- `onPause()`
- `onStop()`
- `onDestroy()`

Asociación con XML mediante:

- `setContentView()`
- `findViewById()`

### Eventos de UI

- `setOnClickListener`
- `setOnLongClickListener`
- `setOnCheckedChangeListener`
- `setOnItemSelectedListener`

### ListView

- `ArrayAdapter`
- `BaseAdapter`
- `setOnItemClickListener`

### Service

Tipos:

- Started Service
- Bound Service

Métodos:

- `startService()`
- `stopService()`
- `stopSelf()`
- `bindService()`

### BroadcastReceiver

Recepción de eventos del sistema.

Ejemplo:

- `BOOT_COMPLETED`

### Intents

**Explícitos**

- Abrir Activities
- Enviar datos mediante `putExtra()`

**Implícitos**

- `ACTION_VIEW`
- `ACTION_SEND`
- `ACTION_CALL`
- `ACTION_IMAGE_CAPTURE`

---

# Clase 3 — Fragments & Navigation Component

## Fragment

- Modularización de UI
- Ciclo de vida:
  - `onCreate()`
  - `onCreateView()`
  - `onViewCreated()`
  - `onDestroyView()`

Administración mediante:

- `FragmentManager`
- `FragmentTransaction`

Operaciones:

- `add()`
- `replace()`
- `remove()`
- `commit()`

Uso de:

- `FragmentContainerView`

---

## Navigation Component

Componentes:

- Navigation Graph
- NavHostFragment
- NavController

Dependencias:

```gradle
implementation "androidx.navigation:navigation-fragment"
implementation "androidx.navigation:navigation-ui"
```

---

# Clase 4 — API REST & Retrofit

## Herramientas habilitadas

### Retrofit

- Cliente HTTP
- Conversión JSON mediante Gson o Moshi

Anotaciones:

- `@GET`
- `@POST`
- `@PUT`
- `@DELETE`

Callbacks:

- `onResponse()`
- `onFailure()`

### OkHttp

- Interceptores
- Logging
- Tokens
- Caché

---

# Clase 5 — Inyección de Dependencias (Hilt) & JWT

## Herramientas habilitadas

### Hilt

- Framework oficial de DI
- Reemplaza singleton manual de Retrofit

Anotaciones:

- `@HiltAndroidApp`
- `@AndroidEntryPoint`
- `@Module`
- `@Provides`

Scopes:

- `@Singleton`
- `@ActivityScoped`

### JWT

Persistencia mediante:

- `EncryptedSharedPreferences`

Uso:

```
Authorization: Bearer <token>
```

---

# Clase 6 — Persistencia de Datos & Galería

## Herramientas habilitadas

### DataStore

Para:

- Configuraciones
- Preferencias
- Flags

---

### Room

Persistencia estructurada mediante SQLite.

Componentes:

- Entity
- DAO
- Database

---

### Files internos

Uso:

- Caché
- Datos privados

---

### Files externos

Uso:

- PDFs
- Imágenes
- Descargas

---

### EncryptedSharedPreferences

Uso recomendado para:

- JWT
- Claves
- Tokens

---

### Glide

Carga eficiente de imágenes.

Características:

- Cache
- Resize
- Placeholder
- CircleCrop

---

## Galería

Permisos:

Android ≤12

```
READ_EXTERNAL_STORAGE
```

Android ≥13

```
READ_MEDIA_IMAGES
```

Selección mediante:

```
Intent.ACTION_PICK
```

El `Uri` devuelto por la galería es únicamente una referencia; debe copiarse a almacenamiento propio antes de persistirlo.

---

# Clase 7 — Biometría

## Herramientas habilitadas

### ActivityResultLauncher

Contratos:

- GetContent
- RequestPermission
- RequestMultiplePermissions
- StartActivityForResult

---

### BiometricManager

Consulta:

- Hardware
- Huellas registradas
- Disponibilidad

---

### PromptInfo

Configuración del diálogo biométrico.

---

### BiometricPrompt

Callbacks:

- `onAuthenticationSucceeded()`
- `onAuthenticationFailed()`
- `onAuthenticationError()`

---

## Restricciones

- Registrar launcher antes de `STARTED`
- Si se utiliza `DEVICE_CREDENTIAL` no puede llamarse a `setNegativeButtonText()`
- Si el usuario no posee biometría registrada debe redirigirse al enrolamiento o deshabilitar la funcionalidad.

---

---

# Clase 7.5 — Material Design & Material Components

## Herramientas habilitadas

### Material Design

Sistema oficial de diseño de Android.

Define:

- Colores
- Tipografía
- Elevaciones
- Animaciones
- Comportamiento de los componentes
- Consistencia visual de toda la aplicación

Material Design define **cómo debe verse y comportarse** una aplicación, pero **no implementa código**.

---

### Material Components

Librería oficial que implementa Material Design.

Dependencia:

```gradle
implementation "com.google.android.material:material:1.11.0"
```

Componentes principales habilitados:

- MaterialButton
- TextInputLayout
- TextInputEditText
- MaterialCardView
- MaterialToolbar
- TabLayout
- BottomNavigationView
- FloatingActionButton

Todos estos componentes obtienen automáticamente su apariencia desde el Theme.

---

## Theme

El Theme configura globalmente el aspecto de toda la aplicación.

Se define en:

```
res/values/themes.xml
```

Se aplica desde:

```xml
<application
    android:theme="@style/Theme.MiApp"/>
```

Tema recomendado:

```xml
Theme.MaterialComponents.DayNight.NoActionBar
```

Variantes disponibles:

- DayNight
- Light
- Dark

Barra superior:

- ActionBar
- NoActionBar
- DarkActionBar

---

## Sistema de colores

Los componentes Material **no poseen colores hardcodeados**.

Todos obtienen sus colores automáticamente desde el Theme.

Colores principales:

| Color | Uso |
|--------|-----|
| colorPrimary | Botones principales, AppBar y elementos destacados |
| colorOnPrimary | Texto e iconos sobre colorPrimary |
| colorSecondary | FloatingActionButton y elementos secundarios |
| colorSurface | Fondo de Cards y superficies |
| colorOnSurface | Texto sobre superficies |
| colorError | Validaciones y mensajes de error |

Regla:

Cada par `colorX` / `colorOnX` garantiza el contraste automáticamente.

Modificar `colorPrimary` actualiza automáticamente toda la aplicación.

---

## Material Components disponibles

### MaterialButton

Características:

- Ripple automático
- Elevación
- Colores heredados del Theme

---

### TextInputLayout

Características:

- Label flotante
- Color automático durante el foco
- Manejo automático de errores mediante `colorError`

Debe envolver un:

```
TextInputEditText
```

---

### MaterialCardView

Características:

- Bordes redondeados
- Elevación
- Fondo utilizando `colorSurface`

---

### MaterialToolbar

Barra superior recomendada.

Utiliza automáticamente:

- colorPrimary
- colorOnPrimary

Puede utilizarse junto con:

```java
setSupportActionBar(toolbar);
```

---

### TabLayout

Permite navegación entre secciones.

Generalmente se utiliza junto con:

- ViewPager2

---

### BottomNavigationView

Barra de navegación inferior.

Se integra con:

- Navigation Component
- NavController

El elemento seleccionado utiliza automáticamente `colorPrimary`.

---

## Organización recomendada de una pantalla

Jerarquía visual recomendada:

```
MaterialToolbar

↓

TabLayout (opcional)

↓

FragmentContainerView

↓

FloatingActionButton (opcional)

↓

BottomNavigationView
```

La navegación continúa siendo administrada mediante:

- Navigation Component
- NavController

---

## Styles

Los Styles permiten modificar únicamente determinados componentes.

Se definen en:

```
res/values/styles.xml
```

Ejemplo:

```xml
<style
    name="BotonDestacado"
    parent="Widget.MaterialComponents.Button">

    <item name="backgroundTint">#FF5722</item>
    <item name="android:textColor">#FFFFFF</item>

</style>
```

Aplicación:

```xml
<com.google.android.material.button.MaterialButton
    style="@style/BotonDestacado"/>
```

---

## Jerarquía de estilos (Override)

Prioridad (de menor a mayor):

1. Theme
2. Style
3. Atributos XML

Ejemplo:

```
Theme
↓

Style

↓

android:backgroundTint
```

El atributo definido directamente en el XML siempre tiene prioridad.

---

## Buenas prácticas

Utilizar:

- Theme para aproximadamente el 90% de la aplicación.
- Styles únicamente para variantes reutilizables.
- Atributos XML directos solamente para casos únicos.

Evitar:

- Hardcodear colores en layouts.
- Duplicar estilos entre pantallas.
- Mezclar innecesariamente Theme, Style y atributos directos.

---

## Safe Area (Window Insets)

Desde Android 15 (API 35) el modo **Edge-to-Edge** es obligatorio.

El contenido puede quedar detrás de:

- Status Bar
- Navigation Bar

Android nativo utiliza:

```
WindowInsetsCompat
```

Los componentes Material como:

- AppBarLayout
- BottomNavigationView

manejan automáticamente parte de estos insets cuando se utiliza un Theme Material.

---

## Restricciones impuestas por la cátedra

Para el desarrollo de interfaces deberán utilizarse preferentemente componentes Material.

Tecnologías habilitadas:

- Material Components
- Theme.MaterialComponents
- MaterialButton
- MaterialToolbar
- TextInputLayout
- MaterialCardView
- BottomNavigationView
- TabLayout
- FloatingActionButton

Se recomienda evitar hardcodear colores o estilos directamente en los layouts, priorizando la configuración mediante Themes y Styles.

# Clase 8 — Cámara (QR) y Notificaciones mediante Long Polling (Android Nativo)

## Lectura de códigos QR

### Herramienta habilitada

### ML Kit Barcode Scanning

Framework oficial de Google para reconocimiento de:

- QR
- Barcode

El procesamiento se realiza **on-device**, sin implementar algoritmos propios de detección.

Dependencia:

```gradle
implementation "com.google.mlkit:barcode-scanning"
```

---

## Permisos

Permiso requerido:

```xml
<uses-permission android:name="android.permission.CAMERA"/>
```

Solicitud en runtime mediante:

- `ActivityResultLauncher<String>`
- `ActivityResultContracts.RequestPermission`

Patrón requerido:

```java
if (ContextCompat.checkSelfPermission(...) != PackageManager.PERMISSION_GRANTED) {
    launcher.launch(Manifest.permission.CAMERA);
} else {
    iniciarCamara();
}
```

---

## Flujo esperado

1. Verificar permiso.
2. Solicitar permiso si es necesario.
3. Iniciar cámara.
4. Detectar código QR mediante ML Kit.
5. Obtener el contenido del QR.
6. Interpretar el contenido (por ejemplo JSON o ID).
7. Ejecutar la acción correspondiente.

La librería únicamente devuelve el contenido del código; el parseo y validación son responsabilidad de la aplicación.

---

## Notificaciones mediante Long Polling

### Concepto

El cliente realiza una única petición.

El servidor mantiene la conexión abierta hasta que:

- exista una novedad, o
- expire un timeout.

Cuando recibe la respuesta, el cliente vuelve inmediatamente a consultar.

---

## Comparación

### Polling tradicional

```
Cliente
↓

Pregunta

↓

Servidor responde inmediatamente

↓

Cliente espera

↓

Repite
```

Genera numerosas consultas sin datos.

---

### Long Polling

```
Cliente

↓

Pregunta

↓

Servidor mantiene abierta la conexión

↓

Existe novedad

↓

Servidor responde

↓

Cliente vuelve a consultar
```

Reduce considerablemente requests innecesarios.

---

## Implementación Android

Las consultas pueden ejecutarse desde:

### Foreground Service

Uso cuando:

- la aplicación debe continuar consultando incluso en background.

Ventajas:

- Sobrevive mientras la app permanezca abierta.
- Continúa con pantalla apagada.

Limitación:

No sobrevive a **Forzar detención** desde Ajustes.

Debe ejecutarse mediante:

```java
startForeground(...)
```

Mostrando obligatoriamente una notificación persistente.

---

### WorkManager

Uso recomendado para:

- chequeos periódicos
- menor consumo de batería

No mantiene conexiones abiertas continuamente.

---

## Cliente HTTP

Se utiliza:

- OkHttp
- Gson

Patrón:

```java
Response response = okHttpClient.newCall(request).execute();
```

Si la respuesta contiene novedades:

- deserializar
- mostrar notificación

---

## Notificaciones nativas

Construcción mediante:

```java
NotificationCompat.Builder
```

Componentes:

- icono
- título
- texto

En Android 8+ es obligatorio crear previamente un:

```
NotificationChannel
```

---

## Buenas prácticas

### Reconexión

Toda excepción de red debe provocar un nuevo intento.

Nunca finalizar silenciosamente el loop.

---

### Timeout

El cliente debe definir timeout además del timeout del servidor para evitar conexiones colgadas.

---

### Consumo de batería

Evitar:

```java
while(true)
```

sin pausas.

Preferir:

- WorkManager
- backoff
- tiempos de espera

---

### Conectividad

Antes de reintentar:

- verificar disponibilidad de red

para evitar ciclos infinitos de error.

---

## Restricciones impuestas por la cátedra

Para Android Nativo quedan habilitadas únicamente las siguientes tecnologías para esta funcionalidad:

- ML Kit Barcode Scanning
- ActivityResultLauncher
- ActivityResultContracts.RequestPermission
- Manifest.permission.CAMERA
- Foreground Service
- WorkManager
- OkHttp
- Gson
- NotificationCompat
- NotificationChannel

No sugerir alternativas (ZXing, CameraX con otros lectores, Firebase QR Scanner, WebSockets, FCM, etc.) salvo que el usuario lo solicite explícitamente.