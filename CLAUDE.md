# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**android-app** — a single-module Android application written in **Java** (not Kotlin) using traditional Android Views and XML layouts. It targets API 36 with minSdk 30.

## Build System

- Gradle with **Kotlin DSL** (`.gradle.kts` files)
- Dependencies managed via a **version catalog** (`gradle/libs.versions.toml`)
- AGP version: 9.1.0, Gradle wrapper: 9.3.1

### Common Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run all unit tests
./gradlew test

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.example.androidapp.ExampleUnitTest"

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

## Architecture

- Single `:app` module
- Java source code under `app/src/main/java/com/example/androidapp/`
- XML layouts in `app/src/main/res/layout/`
- Uses `AppCompatActivity` with EdgeToEdge window insets handling
- UI built with ConstraintLayout, ListView, and standard Android Views
- No DI framework, no architecture components (ViewModel, LiveData, etc.) currently in use

## Testing

- **Unit tests**: JUnit 4 — `app/src/test/java/`
- **Instrumented tests**: AndroidX Test + Espresso — `app/src/androidTest/java/`
- Test runner: `androidx.test.runner.AndroidJUnitRunner`

## Key Dependencies

All managed through `gradle/libs.versions.toml`:
- AndroidX AppCompat, Activity, ConstraintLayout
- Material Design Components
- JUnit 4, AndroidX Test JUnit, Espresso (testing)
