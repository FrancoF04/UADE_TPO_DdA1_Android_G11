<div align="center">

# 🧭 XploreNow

**Descubrí, reservá y viví experiencias turísticas desde tu celular.**

App Android nativa (Java) para explorar tours, free tours y excursiones, reservarlos en segundos y llevar el voucher siempre con vos, incluso sin conexión.

![Android](https://img.shields.io/badge/Android-minSdk%2030-3DDC84?logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-11-ED8B00?logo=openjdk&logoColor=white)
![Hilt](https://img.shields.io/badge/DI-Hilt-4285F4)
![Retrofit](https://img.shields.io/badge/HTTP-Retrofit%20%2B%20OkHttp-48B983)
![License](https://img.shields.io/badge/license-CC0--1.0-lightgrey)

</div>

---

## ✨ ¿Qué podés hacer con la app?

| | |
|---|---|
| 🔐 **Ingresar fácil y seguro** | Login con usuario y contraseña, código OTP por email y acceso con **huella/biometría** para los próximos ingresos. |
| 🏝️ **Explorar actividades** | Catálogo paginado con imagen, destino, categoría, duración, precio y cupos. Filtros por destino, categoría, fecha y precio, y sugerencias según tus preferencias de viaje. |
| 📍 **Ver el detalle y cómo llegar** | Descripción, qué incluye, guía, idioma, política de cancelación, galería de fotos y mapa con el punto de encuentro. |
| 🎟️ **Reservar y cancelar** | Elegí fecha, horario y cantidad de participantes con validación de cupos en tiempo real. |
| 📱 **Voucher digital + check-in por QR** | Voucher con todos los datos de la actividad y escaneo del QR del guía para confirmar tu asistencia (verde ✅ / rojo ❌). |
| ⭐ **Calificar** | Puntuá la actividad y al guía (1 a 5 estrellas) con comentario opcional, durante las 48 hs posteriores. |
| 🕘 **Historial** | Todo lo que ya viviste, con filtros por fecha y destino y acceso a tu calificación. |
| ❤️ **Favoritos** | Guardá actividades y enterate cuando cambian de precio o liberan cupos. |
| 📰 **Noticias y ofertas** | Novedades, descuentos y nuevos destinos, con caché local. |
| 🔔 **Recordatorios y avisos** | Notificación 24 hs antes con acceso directo al voucher, y alertas inmediatas si una actividad se cancela o reprograma. |
| 📴 **Modo sin conexión** | Vouchers y próximas actividades disponibles offline, con sincronización al recuperar la red. |
| 👤 **Perfil** | Editá tus datos, tu foto y tus preferencias (aventura, cultura, gastronomía, naturaleza, relax). |

## 🛠️ Stack técnico

- **Lenguaje:** Java 11 · Views tradicionales + XML (ConstraintLayout, ListView, Material Components)
- **Arquitectura:** Single Activity + **Navigation Component** (nav graphs anidados)
- **Inyección de dependencias:** Hilt
- **Networking:** Retrofit + Gson + OkHttp, con interceptor de **refresh transparente de sesión**
- **Seguridad:** `EncryptedSharedPreferences` para tokens, `BiometricPrompt` para acceso biométrico
- **Imágenes:** Glide
- **Mapas:** osmdroid
- **QR:** CameraX + ML Kit Barcode Scanning
- **Notificaciones:** Long polling en un Foreground Service
- **Persistencia local:** archivos internos (caché de noticias y reservas offline)

## 🗂️ Estructura del proyecto

```
app/src/main/java/com/example/androidapp/
├── data/
│   ├── local/    Persistencia local (tokens, caché, preferencias)
│   ├── model/    Modelos de datos
│   └── remote/   Interfaces Retrofit de la API
├── di/           Módulos Hilt e interceptores de red
├── ui/           Pantallas: auth, home, reservation, historial, favorites, news, profile, qr
└── util/         Helpers (fechas, imágenes, biometría, notificaciones, red)
```

## 🚀 Cómo correrlo

### Requisitos

- Android Studio (versión reciente) con JDK 11+
- Un emulador o dispositivo con Android 11 (API 30) o superior
- Un backend de XploreNow accesible (la app consume una API REST)

### Pasos

1. Cloná el repositorio:
   ```bash
   git clone https://github.com/FrancoF04/UADE_TPO_DdA1_Android_G11.git
   cd UADE_TPO_DdA1_Android_G11
   ```
2. Creá tu archivo de entorno a partir de la plantilla:
   ```bash
   cp .env.example .env
   ```
3. Editá `.env` y configurá la URL de tu backend:
   ```properties
   API_BASE_URL=http://10.0.2.2:3000/api/
   ```
   - `10.0.2.2` es el `localhost` de tu PC visto desde el emulador de Android.
   - Para un dispositivo físico en tu red, usá la IP de tu PC y agregala a [`network_security_config.xml`](app/src/main/res/xml/network_security_config.xml).
   - Para un backend desplegado, usá su URL `https://.../api/`.
4. Abrí el proyecto en Android Studio, sincronizá Gradle y ejecutá la app. O por línea de comandos:
   ```bash
   ./gradlew assembleDebug
   ```

> El archivo `.env` está en el `.gitignore`: nunca se sube al repositorio.

### Tests

```bash
./gradlew test                  # tests unitarios (JUnit 4)
./gradlew connectedAndroidTest  # tests instrumentados (requiere emulador/dispositivo)
```

## 🎓 Contexto académico

Proyecto desarrollado como **Trabajo Práctico Obligatorio** de la materia *Desarrollo de Aplicaciones I* (UADE, 1er cuatrimestre 2026). Las consignas se resumen en [`docs/requirements/TPO.md`](docs/requirements/TPO.md). Este repositorio contiene únicamente el frontend Android; el backend es un servicio aparte.

## 📄 Licencia

Distribuido bajo licencia [CC0 1.0 Universal](LICENSE).
