# CLAUDE.md

Este archivo provee instrucciones a Claude Code (claude.ai/code) para trabajar en este repositorio.

## Contexto del Proyecto

Antes de realizar cualquier cambio, leer [`PROJECT_CONTEXT.md`](PROJECT_CONTEXT.md). Contiene los requerimientos académicos, tecnologías obligatorias y restricciones de la cátedra. Respetar todo lo definido ahí.

## Descripción General

**android-app** — aplicación Android de un solo módulo escrita en **Java** (no Kotlin), usando Views tradicionales y layouts XML. Target API 36, minSdk 30.

## Sistema de Build

- Gradle con **Kotlin DSL** (archivos `.gradle.kts`)
- Dependencias gestionadas mediante un **catálogo de versiones** (`gradle/libs.versions.toml`)
- Versión de AGP: 9.1.0, Gradle wrapper: 9.3.1

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
- Código fuente Java en `app/src/main/java/com/example/androidapp/`
- Layouts XML en `app/src/main/res/layout/`
- Usa `AppCompatActivity` con manejo de EdgeToEdge window insets
- UI construida con ConstraintLayout, ListView y Views estándar de Android
- Sin framework de inyección de dependencias ni componentes de arquitectura (ViewModel, LiveData, etc.) actualmente

## Testing

- **Tests unitarios**: JUnit 4 — `app/src/test/java/`
- **Tests instrumentados**: AndroidX Test + Espresso — `app/src/androidTest/java/`
- Test runner: `androidx.test.runner.AndroidJUnitRunner`

## Dependencias Clave

Todas gestionadas a través de `gradle/libs.versions.toml`:
- AndroidX AppCompat, Activity, ConstraintLayout
- Material Design Components
- JUnit 4, AndroidX Test JUnit, Espresso (testing)
