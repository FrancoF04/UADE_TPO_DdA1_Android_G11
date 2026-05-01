# Plan B — Mobile Catálogo (Consigna 3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cerrar la consigna 3 del TPO XploreNow en mobile: imágenes reales con Glide en cards y galería vertical del detalle, chips de filtro rápido (Destacadas/Para vos/Todas/Filtros) en Home, paginación scroll-infinito, FiltersFragment full-screen con destino/categoría/fecha/precio, PreferencesFragment para configurar categorías y destinos del usuario, y botón "Ver en mapa" con Intent implícito.

**Architecture:** Java + Hilt + Retrofit + Glide. Se cambian las URLs fake de `data.js` del backend por URLs curadas de Unsplash. En mobile se agrega Glide para cargar imágenes en `ActivityAdapter`, `HistorialAdapter`, `ReservationAdapter` y `ActivityDetailFragment`. La galería del detalle se renderea como `LinearLayout` vertical inflado dinámicamente. `HomeFragment` se reescribe para soportar 3 fuentes (featured/recommended/all) con chips, paginación scroll-infinito y filtros aplicables vía `FiltersFragment` (Navigation Component + `setFragmentResult`). `PreferencesFragment` permite multi-choice de categorías/destinos persistidas en cache local + sincronizadas vía `PUT /api/profile/preferences`. `MapIntentLauncher` abre el app de mapas del sistema.

**Tech Stack:** Java 11, Hilt 2.59.2, Retrofit 2.11.0, Glide 4.16.0 (nueva), Navigation 2.8.9, AndroidX AppCompat. Min SDK 30, Target SDK 36.

**Spec base:** `docs/superpowers/specs/2026-04-30-mobile-auth-catalog-design.md`

**Working directories:**
- Mobile: `C:/Users/a950839/OneDrive - ATOS/Dev/desapp/mobile-app-android`
- Backend (solo Task 2): `C:/Users/a950839/OneDrive - ATOS/Dev/desapp/backend-app-node`

**Branches:**
- Mobile: `feature/catalog-consigna-3` (desde `main`)
- Backend: `feature/unsplash-curated-images` (PR separado)

**Pre-requisito:** Plan A (`feature/auth-consigna-1`) debería estar mergeado o cerca, especialmente porque este plan asume `TokenManager.saveSession`, `BiometricHelper`, `ApiErrorParser` ya existentes.

---

## File structure

**Nuevos en mobile** (`app/src/main/java/com/example/androidapp/`):

| Archivo | Responsabilidad |
|---|---|
| `data/local/PreferencesStore.java` | Cache local de `categories[]` y `destinations[]` |
| `data/model/Filters.java` | POJO Parcelable: destination, category, date, priceMin, priceMax |
| `util/FilterQueryBuilder.java` | Convierte `Filters` en `Map<String,String>` para `@QueryMap` |
| `util/MapIntentLauncher.java` | Abre Intent `geo:lat,lng?q=address` |
| `ui/home/FiltersFragment.java` | Pantalla full-screen de filtros |
| `ui/profile/PreferencesFragment.java` | Multi-choice de categorías + destinos |

**Recursos nuevos** (`app/src/main/res/`):

| Archivo | Responsabilidad |
|---|---|
| `drawable/ic_placeholder_activity.xml` | Vector neutro para `.placeholder()` y `.error()` de Glide |
| `drawable/chip_background.xml` | Chip normal del Home |
| `drawable/chip_background_active.xml` | Chip seleccionado |
| `drawable/pill_background.xml` | Pill normal de Filters/Preferences |
| `drawable/pill_background_selected.xml` | Pill seleccionada |
| `layout/fragment_filters.xml` | Layout de FiltersFragment |
| `layout/fragment_preferences.xml` | Layout de PreferencesFragment |

**Modificados:**

| Archivo | Cambio |
|---|---|
| `gradle/libs.versions.toml` | Agregar Glide |
| `app/build.gradle.kts` | Agregar `implementation(libs.glide)` y `annotationProcessor(libs.glide.compiler)` |
| `ui/home/ActivityAdapter.java` | Glide en `getView` |
| `ui/historial/HistorialAdapter.java` | Glide |
| `ui/reservation/ReservationAdapter.java` | Glide |
| `ui/home/ActivityDetailFragment.java` | Glide hero + galería vertical + botón "Ver en mapa" |
| `ui/home/HomeFragment.java` | Chips bar, scroll-infinito, integración con FiltersFragment |
| `ui/profile/ProfileFragment.java` | Item "Mis preferencias" → navega a PreferencesFragment |
| `res/navigation/nav_graph.xml` | Agregar `filtersFragment` y `preferencesFragment` con sus actions |
| `data/remote/ProfileApi.java` | Verificar/agregar `getPreferences` y `putPreferences` |

**Backend** (`backend-app-node/src/data/data.js`): reemplazar ~78 URLs fake por curadas de Unsplash.

**Notas:**
- TDD no aplica (PROJECT_CONTEXT no incluye testing). Cada task termina con build verification + smoke manual.
- Commits en inglés con `git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" commit ...`.
- Plan asume Plan A ya mergeado.

---

## Task 1: Setup branch + add Glide dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Crear branch desde main**

```bash
git checkout main
git pull --ff-only origin main
git checkout -b feature/catalog-consigna-3
```

- [ ] **Step 2: Agregar versión y librerías en `gradle/libs.versions.toml`**

En `[versions]` agregar:

```toml
glide = "4.16.0"
```

En `[libraries]` agregar:

```toml
glide = { group = "com.github.bumptech.glide", name = "glide", version.ref = "glide" }
glide-compiler = { group = "com.github.bumptech.glide", name = "compiler", version.ref = "glide" }
```

- [ ] **Step 3: Agregar dependencias en `app/build.gradle.kts`**

Dentro del bloque `dependencies { ... }`, agregar:

```kotlin
implementation(libs.glide)
annotationProcessor(libs.glide.compiler)
```

- [ ] **Step 4: Sync + build**

```bash
./gradlew assembleDebug --warning-mode all
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "build: add Glide dependency"
```

---

## Task 2: Backend — replace fake image URLs with curated Unsplash URLs

**Files:** (en el repo `backend-app-node`)
- Modify: `src/data/data.js`

**IMPORTANTE:** Esta task se ejecuta en el **repo backend**, no en mobile. PR separado.

- [ ] **Step 1: Crear branch en el backend**

```bash
cd "C:/Users/a950839/OneDrive - ATOS/Dev/desapp/backend-app-node"
git checkout main
git pull --ff-only origin main
git checkout -b feature/unsplash-curated-images
```

- [ ] **Step 2: Mapear actividades a fotos curadas**

Por cada una de las 15 actividades en `src/data/data.js`, buscar 5 fotos en Unsplash (1 cover + 4 gallery) que coincidan con el contenido. Tabla guía:

| ID | Nombre | Keywords sugeridos |
|---|---|---|
| a1 | Walking Tour San Telmo | "san telmo buenos aires", "tango street", "cobblestone old town" |
| a2 | Free Tour La Boca | "caminito la boca", "buenos aires colorful houses" |
| a3 | Visita Colón | "teatro colon buenos aires", "opera house interior" |
| a4 | Cataratas Iguazú | "iguazu falls", "argentina waterfall jungle" |
| a5 | Vinos Mendoza | "mendoza vineyard", "wine tasting argentina" |
| a6 | Trekking Glaciar Perito Moreno | "perito moreno glacier", "patagonia ice" |
| a7 | Tour Fin del Mundo | "ushuaia patagonia", "tierra del fuego mountain" |
| a8 | Bodega Premium Mendoza | "premium winery argentina", "wine barrel mendoza" |
| a9 | City Tour Córdoba | "cordoba argentina cathedral", "cordoba historic plaza" |
| a10 | Salinas Grandes | "salinas grandes salta", "salt flats argentina" |
| a11 | Kayak Nahuel Huapi | "bariloche lake kayak", "nahuel huapi boat" |
| a12 | Free Tour Palermo | "palermo buenos aires park", "buenos aires gardens" |
| a13 | Tren de las Nubes | "tren de las nubes salta", "high altitude train argentina" |
| a14 | Paragliding Sierras | "paragliding mountains", "argentina sierras flying" |
| a15 | Asado Folclore Salta | "argentine asado bbq", "argentina folklore peña" |

Reemplazar cada bloque:

```javascript
// Antes
imageUrl: 'https://images.example.com/san-telmo-tour.jpg',
galleryUrls: [
  'https://images.example.com/san-telmo-1.jpg',
  // ...
],
```

por:

```javascript
// Después
imageUrl: 'https://images.unsplash.com/photo-XXXXXXXXXXXX?w=1200&q=80&auto=format&fit=crop',
galleryUrls: [
  'https://images.unsplash.com/photo-XXXXXXXXXXXX?w=1200&q=80&auto=format&fit=crop',
  'https://images.unsplash.com/photo-XXXXXXXXXXXX?w=1200&q=80&auto=format&fit=crop',
  'https://images.unsplash.com/photo-XXXXXXXXXXXX?w=1200&q=80&auto=format&fit=crop',
  'https://images.unsplash.com/photo-XXXXXXXXXXXX?w=1200&q=80&auto=format&fit=crop',
],
```

Mismo tratamiento para las 3 noticias (`news` array): cada una tiene `image`, reemplazar con foto temática (1 sola URL).

- [ ] **Step 3: Smoke test de las URLs**

Crear archivo temporal `scripts/check-images.sh`:

```bash
#!/usr/bin/env bash
set -e
grep -oE "https://images\.unsplash\.com/photo-[a-zA-Z0-9_-]+(\?[^'\"]*)?" src/data/data.js | sort -u | while read url; do
  status=$(curl -s -o /dev/null -w "%{http_code}" -L "$url")
  echo "$status  $url"
  [ "$status" = "200" ] || echo "FAIL: $url"
done
```

Correr:

```bash
chmod +x scripts/check-images.sh
./scripts/check-images.sh
```

Expected: todas las URLs respondan 200.

- [ ] **Step 4: Verificar localmente**

```bash
npm run dev
```

En otra terminal:

```bash
curl -s http://localhost:3000/api/activities/a1 | grep -E "imageUrl|galleryUrls" | head -10
```

Expected: ver URLs de Unsplash, NO `images.example.com`.

- [ ] **Step 5: Commit + push backend**

```bash
git add src/data/data.js scripts/check-images.sh
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "data: replace fake image URLs with curated Unsplash URLs"
git push -u origin feature/unsplash-curated-images
```

Crear PR en el repo backend, mergear, esperar redeploy de Railway.

- [ ] **Step 6: Volver al repo mobile**

```bash
cd "C:/Users/a950839/OneDrive - ATOS/Dev/desapp/mobile-app-android"
```

---

## Task 3: Add ic_placeholder_activity drawable

**Files:**
- Create: `app/src/main/res/drawable/ic_placeholder_activity.xml`

- [ ] **Step 1: Crear el drawable vector**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="120dp"
    android:height="120dp"
    android:viewportWidth="24"
    android:viewportHeight="24">

    <path
        android:fillColor="#E2E8F0"
        android:pathData="M0,0h24v24h-24z"/>

    <path
        android:fillColor="#94A3B8"
        android:pathData="M21,19V5c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2zM8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z"/>
</vector>
```

- [ ] **Step 2: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/drawable/ic_placeholder_activity.xml
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): add ic_placeholder_activity vector drawable for Glide"
```

---

## Task 4: Add chip_background and pill_background drawables

**Files:**
- Create: `app/src/main/res/drawable/chip_background.xml`
- Create: `app/src/main/res/drawable/chip_background_active.xml`
- Create: `app/src/main/res/drawable/pill_background.xml`
- Create: `app/src/main/res/drawable/pill_background_selected.xml`

- [ ] **Step 1: `chip_background.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#F1F5F9"/>
    <corners android:radius="14dp"/>
    <padding android:left="12dp" android:top="6dp" android:right="12dp" android:bottom="6dp"/>
</shape>
```

- [ ] **Step 2: `chip_background_active.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#2563EB"/>
    <corners android:radius="14dp"/>
    <padding android:left="12dp" android:top="6dp" android:right="12dp" android:bottom="6dp"/>
</shape>
```

- [ ] **Step 3: `pill_background.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#FFFFFF"/>
    <stroke android:width="1dp" android:color="#DDDDDD"/>
    <corners android:radius="8dp"/>
    <padding android:left="10dp" android:top="6dp" android:right="10dp" android:bottom="6dp"/>
</shape>
```

- [ ] **Step 4: `pill_background_selected.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#EEF4FF"/>
    <stroke android:width="1dp" android:color="#2563EB"/>
    <corners android:radius="8dp"/>
    <padding android:left="10dp" android:top="6dp" android:right="10dp" android:bottom="6dp"/>
</shape>
```

- [ ] **Step 5: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/drawable/chip_background.xml app/src/main/res/drawable/chip_background_active.xml app/src/main/res/drawable/pill_background.xml app/src/main/res/drawable/pill_background_selected.xml
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): add chip and pill background drawables"
```

---

## Task 5: ActivityAdapter — load images with Glide

**Files:**
- Modify: `app/src/main/java/com/example/androidapp/ui/home/ActivityAdapter.java`

- [ ] **Step 1: Leer el archivo actual**

```bash
cat app/src/main/java/com/example/androidapp/ui/home/ActivityAdapter.java
```

Localizar `holder.ivImage.setImageDrawable(null)` (o equivalente) y el `ImageView` correspondiente.

- [ ] **Step 2: Reemplazar el setter por Glide**

Imports a agregar:

```java
import com.bumptech.glide.Glide;
import com.example.androidapp.R;
```

Reemplazar `holder.ivImage.setImageDrawable(null);` por:

```java
Glide.with(holder.itemView)
        .load(activity.getImageUrl())
        .placeholder(R.drawable.ic_placeholder_activity)
        .error(R.drawable.ic_placeholder_activity)
        .centerCrop()
        .into(holder.ivImage);
```

Donde `activity` es el item de la posición actual. Usar el getter real del modelo.

- [ ] **Step 3: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Smoke test manual**

Pre-requisito: Plan A mergeado o backend con URLs Unsplash en producción.

```bash
./gradlew installDebug
```

Login → Home → ver fotos reales en cada card.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/androidapp/ui/home/ActivityAdapter.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): load activity card images with Glide"
```

---

## Task 6: Glide en HistorialAdapter y ReservationAdapter

**Files:**
- Modify: `app/src/main/java/com/example/androidapp/ui/historial/HistorialAdapter.java`
- Modify: `app/src/main/java/com/example/androidapp/ui/reservation/ReservationAdapter.java`

- [ ] **Step 1: Modificar `HistorialAdapter.java`**

Mismo patrón que Task 5. Localizar el `ImageView` del item, reemplazar `setImageDrawable(null)` por:

```java
Glide.with(holder.itemView)
        .load(item.getImageUrl())
        .placeholder(R.drawable.ic_placeholder_activity)
        .error(R.drawable.ic_placeholder_activity)
        .centerCrop()
        .into(holder.ivImage);
```

Ajustar `item.getImageUrl()` al getter real.

- [ ] **Step 2: Modificar `ReservationAdapter.java`**

Mismo patrón. Si la reserva tiene `getActivity().getImageUrl()`, ajustar la cadena de getters.

- [ ] **Step 3: Build + smoke**

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Manual: ver imágenes reales en Mis Reservas y en Historial.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/androidapp/ui/historial/HistorialAdapter.java app/src/main/java/com/example/androidapp/ui/reservation/ReservationAdapter.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): apply Glide to HistorialAdapter and ReservationAdapter"
```

---

## Task 7: ActivityDetailFragment — hero image with Glide

**Files:**
- Modify: `app/src/main/java/com/example/androidapp/ui/home/ActivityDetailFragment.java`

- [ ] **Step 1: Leer el archivo actual**

```bash
cat app/src/main/java/com/example/androidapp/ui/home/ActivityDetailFragment.java
cat app/src/main/res/layout/fragment_activity_detail.xml | head -40
```

Identificar el `ImageView` del hero (probablemente `ivCover` o `ivImage`).

- [ ] **Step 2: Reemplazar el setter del hero por Glide**

Imports:

```java
import com.bumptech.glide.Glide;
```

En el callback donde se obtiene la actividad, reemplazar el set previo del ImageView del hero por:

```java
Glide.with(this)
        .load(activity.getImageUrl())
        .placeholder(R.drawable.ic_placeholder_activity)
        .error(R.drawable.ic_placeholder_activity)
        .centerCrop()
        .into(binding.ivCover);
```

Ajustar `binding.ivCover` al ID real.

- [ ] **Step 3: Build + smoke**

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Manual: tap en una actividad del Home → ver hero con foto real.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/androidapp/ui/home/ActivityDetailFragment.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): load hero image with Glide in ActivityDetailFragment"
```

---

## Task 8: ActivityDetailFragment — vertical gallery from galleryUrls

**Files:**
- Modify: `app/src/main/java/com/example/androidapp/ui/home/ActivityDetailFragment.java`
- Modify: `app/src/main/res/layout/fragment_activity_detail.xml`

- [ ] **Step 1: Agregar contenedor de galería al layout**

En `fragment_activity_detail.xml`, debajo del `ImageView` hero y antes de las secciones de descripción/info, agregar:

```xml
<TextView
    android:id="@+id/tvGalleryHeader"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Galería"
    android:textSize="16sp"
    android:textStyle="bold"
    android:padding="12dp"
    android:visibility="gone"/>

<LinearLayout
    android:id="@+id/galleryContainer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingStart="12dp"
    android:paddingEnd="12dp"/>
```

- [ ] **Step 2: Inflar la galería en `ActivityDetailFragment.java`**

Imports:

```java
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.bumptech.glide.Glide;
```

En el método donde se popula la actividad (después del Glide del hero), agregar:

```java
binding.galleryContainer.removeAllViews();
java.util.List<String> gallery = activity.getGalleryUrls();
if (gallery != null && !gallery.isEmpty()) {
    binding.tvGalleryHeader.setVisibility(View.VISIBLE);
    int marginPx = (int) (8 * getResources().getDisplayMetrics().density);
    int heightPx = (int) (220 * getResources().getDisplayMetrics().density);

    for (String url : gallery) {
        ImageView iv = new ImageView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heightPx
        );
        params.topMargin = marginPx;
        iv.setLayoutParams(params);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackgroundResource(R.drawable.ic_placeholder_activity);

        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_placeholder_activity)
                .error(R.drawable.ic_placeholder_activity)
                .centerCrop()
                .into(iv);

        iv.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            }
        });

        binding.galleryContainer.addView(iv);
    }
} else {
    binding.tvGalleryHeader.setVisibility(View.GONE);
}
```

Ajustar `activity.getGalleryUrls()` al getter real.

- [ ] **Step 3: Build + smoke**

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Manual:
- Tap en actividad → detalle → ver galería vertical con 4 fotos extra debajo del hero
- Tap en una foto de la galería → abre visor del sistema

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/androidapp/ui/home/ActivityDetailFragment.java app/src/main/res/layout/fragment_activity_detail.xml
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): vertical gallery in ActivityDetailFragment with tap to open"
```

---

## Task 9: MapIntentLauncher + button "Ver en mapa" in ActivityDetailFragment

**Files:**
- Create: `app/src/main/java/com/example/androidapp/util/MapIntentLauncher.java`
- Modify: `app/src/main/java/com/example/androidapp/ui/home/ActivityDetailFragment.java`
- Modify: `app/src/main/res/layout/fragment_activity_detail.xml`

- [ ] **Step 1: Crear `MapIntentLauncher.java`**

```java
package com.example.androidapp.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class MapIntentLauncher {

    @Inject
    public MapIntentLauncher() {}

    public void openMap(Context context, double lat, double lng, String address) {
        String addressEncoded = address != null ? Uri.encode(address) : "";
        Uri uri = Uri.parse("geo:" + lat + "," + lng + "?q=" + addressEncoded);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "No tenés un app de mapas instalado", Toast.LENGTH_SHORT).show();
        }
    }
}
```

- [ ] **Step 2: Agregar botón al layout**

En `fragment_activity_detail.xml`, debajo del texto del meeting point, agregar:

```xml
<Button
    android:id="@+id/btnOpenMap"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:layout_marginStart="12dp"
    android:text="Ver en el mapa"
    android:textColor="#2563EB"
    android:background="?attr/selectableItemBackground"/>
```

- [ ] **Step 3: Cablear el botón en `ActivityDetailFragment.java`**

Imports:

```java
import com.example.androidapp.util.MapIntentLauncher;
import javax.inject.Inject;
```

Inyectar:

```java
@Inject MapIntentLauncher mapIntentLauncher;
```

En el callback donde se popula el meeting point:

```java
if (activity.getMeetingPoint() != null) {
    binding.btnOpenMap.setVisibility(View.VISIBLE);
    binding.btnOpenMap.setOnClickListener(v -> mapIntentLauncher.openMap(
            requireContext(),
            activity.getMeetingPoint().getLatitude(),
            activity.getMeetingPoint().getLongitude(),
            activity.getMeetingPoint().getAddress()
    ));
} else {
    binding.btnOpenMap.setVisibility(View.GONE);
}
```

Ajustar getters según el modelo real (verificar contra `FRONTEND_REFERENCE.md` — `meetingPoint` siempre objeto con `latitude`, `longitude`, `address`).

- [ ] **Step 4: Build + smoke**

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Manual: detalle → "Ver en el mapa" → abre Google Maps con pin en coords correctas.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/androidapp/util/MapIntentLauncher.java app/src/main/java/com/example/androidapp/ui/home/ActivityDetailFragment.java app/src/main/res/layout/fragment_activity_detail.xml
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): add MapIntentLauncher and 'Ver en mapa' button in detail"
```

---

## Task 10: PreferencesStore (cache local)

**Files:**
- Create: `app/src/main/java/com/example/androidapp/data/local/PreferencesStore.java`

**Nota arquitectónica**: el spec menciona `DataStore` (Clase 6), pero DataStore Preferences en Java requiere RxJava3 o Coroutines, ninguno habilitado por la cátedra. Solución: usar `SharedPreferences` debajo con la misma API pública. Cuando la cátedra introduzca DataStore con su flujo asincrónico, se migra sin tocar callsites.

- [ ] **Step 1: Crear el archivo**

```java
package com.example.androidapp.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Cache local de preferencias de viaje del usuario (categorías + destinos).
 * Usa SharedPreferences debajo. La API pública es estable: cuando la cátedra
 * introduzca DataStore como flujo asincrónico habilitado, se migra sin tocar callsites.
 */
@Singleton
public class PreferencesStore {

    private static final String PREF_NAME = "user_preferences_cache";
    private static final String KEY_CATEGORIES = "categories";
    private static final String KEY_DESTINATIONS = "destinations";
    private static final String KEY_LAST_SYNCED_AT = "last_synced_at";

    private final SharedPreferences prefs;

    @Inject
    public PreferencesStore(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public Set<String> getCategories() {
        return new HashSet<>(prefs.getStringSet(KEY_CATEGORIES, new HashSet<>()));
    }

    public Set<String> getDestinations() {
        return new HashSet<>(prefs.getStringSet(KEY_DESTINATIONS, new HashSet<>()));
    }

    public long getLastSyncedAt() {
        return prefs.getLong(KEY_LAST_SYNCED_AT, 0L);
    }

    public boolean hasAnyPreference() {
        return !getCategories().isEmpty() || !getDestinations().isEmpty();
    }

    public void save(List<String> categories, List<String> destinations) {
        Set<String> cats = new HashSet<>(categories != null ? categories : new HashSet<>());
        Set<String> dests = new HashSet<>(destinations != null ? destinations : new HashSet<>());
        prefs.edit()
                .putStringSet(KEY_CATEGORIES, cats)
                .putStringSet(KEY_DESTINATIONS, dests)
                .putLong(KEY_LAST_SYNCED_AT, System.currentTimeMillis())
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
```

- [ ] **Step 2: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/androidapp/data/local/PreferencesStore.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): add PreferencesStore for user travel preferences"
```

---

## Task 11: Add Filters model + FilterQueryBuilder

**Files:**
- Create: `app/src/main/java/com/example/androidapp/data/model/Filters.java`
- Create: `app/src/main/java/com/example/androidapp/util/FilterQueryBuilder.java`

- [ ] **Step 1: Crear `Filters.java`**

```java
package com.example.androidapp.data.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Conjunto de filtros aplicables a GET /api/activities. Todos los campos opcionales.
 * Parcelable para sobrevivir rotación y viajar via Bundle entre fragments.
 */
public class Filters implements Parcelable {

    public String destination;       // ej. "Buenos Aires"
    public String category;          // ej. "free_tour"
    public String date;              // ISO yyyy-MM-dd
    public Integer priceMin;
    public Integer priceMax;

    public Filters() {}

    public boolean isEmpty() {
        return destination == null && category == null && date == null
                && priceMin == null && priceMax == null;
    }

    public void reset() {
        destination = null;
        category = null;
        date = null;
        priceMin = null;
        priceMax = null;
    }

    protected Filters(Parcel in) {
        destination = in.readString();
        category = in.readString();
        date = in.readString();
        priceMin = (Integer) in.readValue(Integer.class.getClassLoader());
        priceMax = (Integer) in.readValue(Integer.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(destination);
        dest.writeString(category);
        dest.writeString(date);
        dest.writeValue(priceMin);
        dest.writeValue(priceMax);
    }

    @Override public int describeContents() { return 0; }

    public static final Creator<Filters> CREATOR = new Creator<Filters>() {
        @Override public Filters createFromParcel(Parcel in) { return new Filters(in); }
        @Override public Filters[] newArray(int size) { return new Filters[size]; }
    };
}
```

- [ ] **Step 2: Crear `FilterQueryBuilder.java`**

```java
package com.example.androidapp.util;

import com.example.androidapp.data.model.Filters;

import java.util.HashMap;
import java.util.Map;

public final class FilterQueryBuilder {

    private FilterQueryBuilder() {}

    public static Map<String, String> build(Filters filters) {
        Map<String, String> map = new HashMap<>();
        if (filters == null) return map;
        if (filters.destination != null) map.put("destination", filters.destination);
        if (filters.category != null) map.put("category", filters.category);
        if (filters.date != null) map.put("date", filters.date);
        if (filters.priceMin != null) map.put("priceMin", String.valueOf(filters.priceMin));
        if (filters.priceMax != null) map.put("priceMax", String.valueOf(filters.priceMax));
        return map;
    }
}
```

- [ ] **Step 3: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/androidapp/data/model/Filters.java app/src/main/java/com/example/androidapp/util/FilterQueryBuilder.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): add Filters parcelable model and FilterQueryBuilder"
```

---

## Task 12: ActivityApi — verify getActivities accepts QueryMap

**Files:**
- Modify (si es necesario): `app/src/main/java/com/example/androidapp/data/remote/ActivityApi.java`

- [ ] **Step 1: Leer el archivo actual**

```bash
cat app/src/main/java/com/example/androidapp/data/remote/ActivityApi.java
```

Verificar que existe un método `getActivities` que acepta `@Query` individual o `@QueryMap`.

- [ ] **Step 2: Si no acepta QueryMap, agregar overload**

Si solo hay queries individuales, agregar:

```java
@GET("activities")
Call<ApiResponse<List<Activity>>> getActivitiesFiltered(
        @Query("page") int page,
        @Query("page_size") int pageSize,
        @QueryMap Map<String, String> filters
);
```

(Si ya existe equivalente, dejarlo y este task es no-op.)

- [ ] **Step 3: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit (si hubo cambio)**

```bash
git add app/src/main/java/com/example/androidapp/data/remote/ActivityApi.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): ActivityApi accepts QueryMap for combined filters"
```

---

## Task 13: FiltersFragment full-screen

**Files:**
- Create: `app/src/main/res/layout/fragment_filters.xml`
- Create: `app/src/main/java/com/example/androidapp/ui/home/FiltersFragment.java`
- Modify: `app/src/main/res/navigation/nav_graph.xml`

- [ ] **Step 1: Verificar viewBinding habilitado**

En `app/build.gradle.kts`, dentro de `android { ... }`, asegurar:

```kotlin
buildFeatures {
    buildConfig = true
    viewBinding = true
}
```

Si `viewBinding = true` ya está, OK. Si no, agregarlo y rebuildear.

- [ ] **Step 2: Crear `fragment_filters.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#FFFFFF">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="12dp"
        android:gravity="center_vertical">
        <Button
            android:id="@+id/btnBack"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="←"
            android:background="?attr/selectableItemBackground"/>
        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Filtros"
            android:textSize="18sp"
            android:textStyle="bold"
            android:gravity="center"/>
        <Button
            android:id="@+id/btnClear"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Limpiar"
            android:textColor="#2563EB"
            android:background="?attr/selectableItemBackground"/>
    </LinearLayout>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="12dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Destino"
                android:textStyle="bold"
                android:layout_marginTop="12dp"/>
            <HorizontalScrollView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp">
                <LinearLayout
                    android:id="@+id/containerDestinations"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"/>
            </HorizontalScrollView>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Categoría"
                android:textStyle="bold"
                android:layout_marginTop="16dp"/>
            <HorizontalScrollView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp">
                <LinearLayout
                    android:id="@+id/containerCategories"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"/>
            </HorizontalScrollView>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Fecha"
                android:textStyle="bold"
                android:layout_marginTop="16dp"/>
            <Button
                android:id="@+id/btnDate"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="📅 Cualquier día"
                android:layout_marginTop="6dp"
                android:background="@drawable/pill_background"/>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Precio"
                android:textStyle="bold"
                android:layout_marginTop="16dp"/>
            <TextView
                android:id="@+id/tvPriceLabel"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:text="Sin filtro"/>
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:layout_marginTop="6dp">
                <EditText
                    android:id="@+id/etPriceMin"
                    android:layout_width="0dp"
                    android:layout_weight="1"
                    android:layout_height="wrap_content"
                    android:hint="Min"
                    android:inputType="number"/>
                <EditText
                    android:id="@+id/etPriceMax"
                    android:layout_width="0dp"
                    android:layout_weight="1"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:hint="Max"
                    android:inputType="number"/>
            </LinearLayout>
        </LinearLayout>
    </ScrollView>

    <Button
        android:id="@+id/btnApply"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_margin="12dp"
        android:text="Aplicar"
        android:backgroundTint="#2563EB"
        android:textColor="#FFFFFF"/>
</LinearLayout>
```

- [ ] **Step 3: Crear `FiltersFragment.java`**

```java
package com.example.androidapp.ui.home;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidapp.R;
import com.example.androidapp.data.model.Filters;
import com.example.androidapp.databinding.FragmentFiltersBinding;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FiltersFragment extends Fragment {

    public static final String RESULT_KEY = "filters_result";
    public static final String ARG_FILTERS = "filters_initial";

    private FragmentFiltersBinding binding;
    private Filters current = new Filters();

    private static final List<String> DESTINATIONS = Arrays.asList(
            "Buenos Aires", "Bariloche", "Mendoza", "Ushuaia", "Córdoba", "Salta"
    );
    private static final List<String> CATEGORIES = Arrays.asList(
            "free_tour", "guided_visit", "excursion", "gastronomic", "adventure"
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFiltersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Filters initial = getArguments() != null ? getArguments().getParcelable(ARG_FILTERS) : null;
        if (initial != null) current = initial;

        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        binding.btnClear.setOnClickListener(v -> {
            current.reset();
            populatePills();
            updateDateButton();
            updatePriceLabel();
            binding.etPriceMin.setText("");
            binding.etPriceMax.setText("");
        });

        populatePills();
        updateDateButton();
        updatePriceLabel();

        if (current.priceMin != null) binding.etPriceMin.setText(String.valueOf(current.priceMin));
        if (current.priceMax != null) binding.etPriceMax.setText(String.valueOf(current.priceMax));

        binding.btnDate.setOnClickListener(v -> showDatePicker());

        TextWatcher priceWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                current.priceMin = parseInt(binding.etPriceMin.getText().toString());
                current.priceMax = parseInt(binding.etPriceMax.getText().toString());
                updatePriceLabel();
            }
        };
        binding.etPriceMin.addTextChangedListener(priceWatcher);
        binding.etPriceMax.addTextChangedListener(priceWatcher);

        binding.btnApply.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putParcelable(RESULT_KEY, current);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            NavHostFragment.findNavController(this).popBackStack();
        });
    }

    private Integer parseInt(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private void populatePills() {
        binding.containerDestinations.removeAllViews();
        for (String d : DESTINATIONS) addPill(binding.containerDestinations, d, d.equals(current.destination), v -> {
            current.destination = d.equals(current.destination) ? null : d;
            populatePills();
        });

        binding.containerCategories.removeAllViews();
        for (String c : CATEGORIES) addPill(binding.containerCategories, prettyCategory(c), c.equals(current.category), v -> {
            current.category = c.equals(current.category) ? null : c;
            populatePills();
        });
    }

    private void addPill(LinearLayout container, String label, boolean selected, View.OnClickListener onClick) {
        Button btn = new Button(requireContext());
        btn.setText(label);
        btn.setBackgroundResource(selected ? R.drawable.pill_background_selected : R.drawable.pill_background);
        btn.setTextColor(selected ? 0xFF2563EB : 0xFF334155);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin);
        btn.setLayoutParams(params);
        btn.setOnClickListener(onClick);
        container.addView(btn);
    }

    private String prettyCategory(String key) {
        switch (key) {
            case "free_tour": return "Free Tour";
            case "guided_visit": return "Visita Guiada";
            case "excursion": return "Excursión";
            case "gastronomic": return "Gastronómica";
            case "adventure": return "Aventura";
            default: return key;
        }
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, day) -> {
                    current.date = String.format("%04d-%02d-%02d", year, month + 1, day);
                    updateDateButton();
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void updateDateButton() {
        binding.btnDate.setText(current.date != null ? "📅 " + current.date : "📅 Cualquier día");
    }

    private void updatePriceLabel() {
        String min = current.priceMin != null ? "$" + current.priceMin : "Sin mínimo";
        String max = current.priceMax != null ? "$" + current.priceMax : "Sin máximo";
        binding.tvPriceLabel.setText(min + " — " + max);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
```

- [ ] **Step 4: Agregar fragment al nav_graph**

En `app/src/main/res/navigation/nav_graph.xml`, dentro de `<navigation>`, agregar:

```xml
<fragment
    android:id="@+id/filtersFragment"
    android:name="com.example.androidapp.ui.home.FiltersFragment"
    android:label="Filtros"/>
```

Y en el fragment del `homeFragment`, agregar acción:

```xml
<action
    android:id="@+id/action_home_to_filters"
    app:destination="@id/filtersFragment"/>
```

- [ ] **Step 5: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/androidapp/ui/home/FiltersFragment.java app/src/main/res/layout/fragment_filters.xml app/src/main/res/navigation/nav_graph.xml
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): add FiltersFragment full-screen with destination/category/date/price"
```

---

## Task 14: HomeFragment — chips bar + filters integration + pagination

**Files:**
- Modify: `app/src/main/java/com/example/androidapp/ui/home/HomeFragment.java`
- Modify: `app/src/main/res/layout/fragment_home.xml`

- [ ] **Step 1: Leer estado actual**

```bash
cat app/src/main/java/com/example/androidapp/ui/home/HomeFragment.java
cat app/src/main/res/layout/fragment_home.xml | head -40
```

- [ ] **Step 2: Agregar barra de chips al layout**

En `fragment_home.xml`, **encima** del `ListView`, agregar:

```xml
<HorizontalScrollView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:scrollbars="none">
    <LinearLayout
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp">
        <Button
            android:id="@+id/chipFeatured"
            style="@style/Widget.AppCompat.Button.Borderless"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="⭐ Destacadas"
            android:textSize="12sp"
            android:layout_marginEnd="6dp"/>
        <Button
            android:id="@+id/chipForYou"
            style="@style/Widget.AppCompat.Button.Borderless"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="💙 Para vos"
            android:textSize="12sp"
            android:layout_marginEnd="6dp"/>
        <Button
            android:id="@+id/chipAll"
            style="@style/Widget.AppCompat.Button.Borderless"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="📍 Todas"
            android:textSize="12sp"
            android:layout_marginEnd="6dp"/>
        <Button
            android:id="@+id/chipFilters"
            style="@style/Widget.AppCompat.Button.Borderless"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="⚙ Filtros"
            android:textSize="12sp"/>
    </LinearLayout>
</HorizontalScrollView>

<TextView
    android:id="@+id/tvEmptyState"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:visibility="gone"
    android:gravity="center"
    android:padding="24dp"
    android:textColor="#666666"/>
```

- [ ] **Step 3: Modificar `HomeFragment.java`**

Imports nuevos:

```java
import com.example.androidapp.data.local.PreferencesStore;
import com.example.androidapp.data.model.Filters;
import com.example.androidapp.util.FilterQueryBuilder;
import com.example.androidapp.R;
import android.widget.AbsListView;
import android.widget.Toast;
import android.view.View;
import androidx.navigation.fragment.NavHostFragment;
import javax.inject.Inject;
```

Inyectar:

```java
@Inject PreferencesStore preferencesStore;
```

(asume que `activityApi` y el `adapter` ya están inyectados/inicializados según el código existente.)

Agregar campos en la clase:

```java
private enum ChipMode { ALL, FEATURED, FOR_YOU }

private ChipMode currentChip = ChipMode.ALL;
private Filters currentFilters = new Filters();
private int currentPage = 1;
private int totalAvailable = 0;
private boolean loading = false;
```

En `onViewCreated`, después del wiring existente del adapter:

```java
setupChips();
setupFiltersResultListener();
setupScrollListener();
loadCurrent(true);
```

Métodos privados:

```java
private void setupChips() {
    binding.chipAll.setOnClickListener(v -> { currentChip = ChipMode.ALL; loadCurrent(true); refreshChipStyles(); });
    binding.chipFeatured.setOnClickListener(v -> { currentChip = ChipMode.FEATURED; loadCurrent(true); refreshChipStyles(); });
    binding.chipForYou.setOnClickListener(v -> { currentChip = ChipMode.FOR_YOU; loadCurrent(true); refreshChipStyles(); });
    binding.chipFilters.setOnClickListener(v -> {
        Bundle args = new Bundle();
        args.putParcelable(FiltersFragment.ARG_FILTERS, currentFilters);
        NavHostFragment.findNavController(this).navigate(R.id.action_home_to_filters, args);
    });
    refreshChipStyles();
}

private void refreshChipStyles() {
    binding.chipAll.setBackgroundResource(currentChip == ChipMode.ALL ? R.drawable.chip_background_active : R.drawable.chip_background);
    binding.chipFeatured.setBackgroundResource(currentChip == ChipMode.FEATURED ? R.drawable.chip_background_active : R.drawable.chip_background);
    binding.chipForYou.setBackgroundResource(currentChip == ChipMode.FOR_YOU ? R.drawable.chip_background_active : R.drawable.chip_background);
    binding.chipAll.setTextColor(currentChip == ChipMode.ALL ? 0xFFFFFFFF : 0xFF334155);
    binding.chipFeatured.setTextColor(currentChip == ChipMode.FEATURED ? 0xFFFFFFFF : 0xFF334155);
    binding.chipForYou.setTextColor(currentChip == ChipMode.FOR_YOU ? 0xFFFFFFFF : 0xFF334155);
}

private void setupFiltersResultListener() {
    getParentFragmentManager().setFragmentResultListener(
            FiltersFragment.RESULT_KEY,
            getViewLifecycleOwner(),
            (key, bundle) -> {
                Filters f = bundle.getParcelable(FiltersFragment.RESULT_KEY);
                if (f != null) {
                    currentFilters = f;
                    currentChip = ChipMode.ALL;
                    loadCurrent(true);
                    refreshChipStyles();
                }
            }
    );
}

private void setupScrollListener() {
    binding.listView.setOnScrollListener(new AbsListView.OnScrollListener() {
        @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
        @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (currentChip != ChipMode.ALL) return;
            if (loading) return;
            if (totalItemCount == 0) return;
            if (firstVisibleItem + visibleItemCount >= totalItemCount - 3 && adapter.getCount() < totalAvailable) {
                loadCurrent(false);
            }
        }
    });
}

private void loadCurrent(boolean replace) {
    if (replace) currentPage = 1;
    loading = true;
    binding.tvEmptyState.setVisibility(View.GONE);

    switch (currentChip) {
        case FEATURED:
            activityApi.getFeatured().enqueue(simpleListCallback(replace));
            break;
        case FOR_YOU:
            if (!preferencesStore.hasAnyPreference()) {
                loading = false;
                if (replace) adapter.replace(new java.util.ArrayList<>());
                binding.tvEmptyState.setText("Configurá tus preferencias en tu perfil para ver actividades recomendadas.");
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                return;
            }
            activityApi.getRecommended().enqueue(simpleListCallback(replace));
            break;
        case ALL:
        default:
            activityApi.getActivitiesFiltered(
                    replace ? 1 : currentPage,
                    10,
                    FilterQueryBuilder.build(currentFilters)
            ).enqueue(allCallback(replace));
            break;
    }
}

private retrofit2.Callback<ApiResponse<java.util.List<Activity>>> simpleListCallback(boolean replace) {
    return new retrofit2.Callback<ApiResponse<java.util.List<Activity>>>() {
        @Override
        public void onResponse(retrofit2.Call<ApiResponse<java.util.List<Activity>>> call,
                               retrofit2.Response<ApiResponse<java.util.List<Activity>>> response) {
            if (!isAdded()) return;
            loading = false;
            if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                java.util.List<Activity> data = response.body().getData();
                if (replace) adapter.replace(data); else adapter.append(data);
                if (data.isEmpty()) {
                    binding.tvEmptyState.setText("No hay actividades para mostrar.");
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                }
            } else {
                Toast.makeText(getContext(), "No pudimos cargar las actividades", Toast.LENGTH_SHORT).show();
            }
        }
        @Override
        public void onFailure(retrofit2.Call<ApiResponse<java.util.List<Activity>>> call, Throwable t) {
            if (!isAdded()) return;
            loading = false;
            Toast.makeText(getContext(), "Sin conexión, reintentá", Toast.LENGTH_SHORT).show();
        }
    };
}

private retrofit2.Callback<ApiResponse<java.util.List<Activity>>> allCallback(boolean replace) {
    return new retrofit2.Callback<ApiResponse<java.util.List<Activity>>>() {
        @Override
        public void onResponse(retrofit2.Call<ApiResponse<java.util.List<Activity>>> call,
                               retrofit2.Response<ApiResponse<java.util.List<Activity>>> response) {
            if (!isAdded()) return;
            loading = false;
            if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                java.util.List<Activity> items = response.body().getData();
                if (response.body().getMeta() != null) {
                    totalAvailable = response.body().getMeta().getTotal();
                }
                if (replace) {
                    adapter.replace(items);
                    currentPage = 2;
                } else {
                    adapter.append(items);
                    currentPage++;
                }
                if (items.isEmpty() && replace) {
                    binding.tvEmptyState.setText("No encontramos actividades con esos filtros. Probá ajustarlos.");
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                }
            } else {
                Toast.makeText(getContext(), "No pudimos cargar las actividades", Toast.LENGTH_SHORT).show();
            }
        }
        @Override
        public void onFailure(retrofit2.Call<ApiResponse<java.util.List<Activity>>> call, Throwable t) {
            if (!isAdded()) return;
            loading = false;
            Toast.makeText(getContext(), "Sin conexión, reintentá", Toast.LENGTH_SHORT).show();
        }
    };
}
```

**Nota sobre `adapter`**: el `ActivityAdapter` debe exponer `replace(List)` y `append(List)`. Si solo expone `setItems(List)`, agregar:

```java
public void replace(java.util.List<Activity> items) {
    this.dataset.clear();
    this.dataset.addAll(items);
    notifyDataSetChanged();
}

public void append(java.util.List<Activity> items) {
    this.dataset.addAll(items);
    notifyDataSetChanged();
}
```

(`dataset` es la lista interna del adapter — ajustar al nombre real.)

- [ ] **Step 4: Build + smoke**

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Manual:
- Home → ver chips arriba
- Tap "⭐ Destacadas" → solo ~7 actividades con featured=true
- Tap "💙 Para vos" sin preferencias → ver mensaje "Configurá tus preferencias"
- Tap "⚙ Filtros" → abre FiltersFragment → setear destino BA → Aplicar → lista filtrada
- Scroll en chip "📍 Todas" hasta el final → carga page 2

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/androidapp/ui/home/HomeFragment.java app/src/main/java/com/example/androidapp/ui/home/ActivityAdapter.java app/src/main/res/layout/fragment_home.xml
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): HomeFragment chips bar, filters integration and pagination"
```

---

## Task 15: PreferencesFragment — multi-choice categories + destinations

**Files:**
- Create: `app/src/main/res/layout/fragment_preferences.xml`
- Create: `app/src/main/java/com/example/androidapp/ui/profile/PreferencesFragment.java`
- Modify: `app/src/main/res/navigation/nav_graph.xml`
- Possibly modify: `app/src/main/java/com/example/androidapp/data/remote/ProfileApi.java`
- Possibly create: `app/src/main/java/com/example/androidapp/data/model/PreferencesRequest.java` y `PreferencesResponse.java`

- [ ] **Step 1: Verificar `ProfileApi`**

```bash
cat app/src/main/java/com/example/androidapp/data/remote/ProfileApi.java
```

Asegurar que tiene:

```java
@GET("profile/preferences")
Call<ApiResponse<PreferencesResponse>> getPreferences();

@PUT("profile/preferences")
Call<ApiResponse<Object>> putPreferences(@Body PreferencesRequest body);
```

Si faltan, agregarlos. Crear `PreferencesRequest`:

```java
package com.example.androidapp.data.model;

import java.util.List;

public class PreferencesRequest {
    public List<String> categories;
    public List<String> destinations;
}
```

Y `PreferencesResponse`:

```java
package com.example.androidapp.data.model;

import java.util.List;

public class PreferencesResponse {
    private Preferences preferences;
    public Preferences getPreferences() { return preferences; }

    public static class Preferences {
        private List<String> categories;
        private List<String> destinations;
        public List<String> getCategories() { return categories; }
        public List<String> getDestinations() { return destinations; }
    }
}
```

- [ ] **Step 2: Crear `fragment_preferences.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#FFFFFF">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="12dp"
        android:gravity="center_vertical">
        <Button android:id="@+id/btnBack" style="@style/Widget.AppCompat.Button.Borderless"
            android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="←"/>
        <TextView android:layout_width="0dp" android:layout_weight="1"
            android:layout_height="wrap_content" android:text="Mis preferencias"
            android:textSize="18sp" android:textStyle="bold" android:gravity="center"/>
        <View android:layout_width="48dp" android:layout_height="match_parent"/>
    </LinearLayout>

    <ScrollView android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
        <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical" android:padding="12dp">

            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="Categorías que te interesan" android:textStyle="bold" android:layout_marginTop="12dp"/>
            <LinearLayout
                android:id="@+id/containerCategories"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:layout_marginTop="6dp"/>

            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="Destinos favoritos" android:textStyle="bold" android:layout_marginTop="20dp"/>
            <LinearLayout
                android:id="@+id/containerDestinations"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:layout_marginTop="6dp"/>
        </LinearLayout>
    </ScrollView>

    <Button
        android:id="@+id/btnSave"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_margin="12dp"
        android:text="Guardar"
        android:backgroundTint="#2563EB"
        android:textColor="#FFFFFF"/>
</LinearLayout>
```

- [ ] **Step 3: Crear `PreferencesFragment.java`**

```java
package com.example.androidapp.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidapp.data.local.PreferencesStore;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.PreferencesRequest;
import com.example.androidapp.data.model.PreferencesResponse;
import com.example.androidapp.data.remote.ProfileApi;
import com.example.androidapp.databinding.FragmentPreferencesBinding;
import com.example.androidapp.util.ApiErrorParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferencesFragment extends Fragment {

    private static final List<String> CATEGORIES = Arrays.asList(
            "free_tour", "guided_visit", "excursion", "gastronomic", "adventure"
    );
    private static final List<String> DESTINATIONS = Arrays.asList(
            "Buenos Aires", "Bariloche", "Mendoza", "Ushuaia", "Córdoba", "Salta"
    );

    @Inject ProfileApi profileApi;
    @Inject PreferencesStore preferencesStore;

    private FragmentPreferencesBinding binding;
    private final Set<String> selectedCategories = new HashSet<>();
    private final Set<String> selectedDestinations = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPreferencesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        selectedCategories.addAll(preferencesStore.getCategories());
        selectedDestinations.addAll(preferencesStore.getDestinations());
        renderCheckboxes();

        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.btnSave.setOnClickListener(v -> save());

        profileApi.getPreferences().enqueue(new retrofit2.Callback<ApiResponse<PreferencesResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<PreferencesResponse>> call,
                                   retrofit2.Response<ApiResponse<PreferencesResponse>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    PreferencesResponse.Preferences prefs = response.body().getData().getPreferences();
                    if (prefs != null) {
                        selectedCategories.clear();
                        if (prefs.getCategories() != null) selectedCategories.addAll(prefs.getCategories());
                        selectedDestinations.clear();
                        if (prefs.getDestinations() != null) selectedDestinations.addAll(prefs.getDestinations());
                        renderCheckboxes();
                    }
                }
            }
            @Override public void onFailure(retrofit2.Call<ApiResponse<PreferencesResponse>> call, Throwable t) {
                /* mantener cache local */
            }
        });
    }

    private void renderCheckboxes() {
        binding.containerCategories.removeAllViews();
        for (String c : CATEGORIES) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(prettyCategory(c));
            cb.setChecked(selectedCategories.contains(c));
            cb.setOnCheckedChangeListener((view, isChecked) -> {
                if (isChecked) selectedCategories.add(c); else selectedCategories.remove(c);
            });
            binding.containerCategories.addView(cb);
        }

        binding.containerDestinations.removeAllViews();
        for (String d : DESTINATIONS) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(d);
            cb.setChecked(selectedDestinations.contains(d));
            cb.setOnCheckedChangeListener((view, isChecked) -> {
                if (isChecked) selectedDestinations.add(d); else selectedDestinations.remove(d);
            });
            binding.containerDestinations.addView(cb);
        }
    }

    private void save() {
        PreferencesRequest body = new PreferencesRequest();
        body.categories = new ArrayList<>(selectedCategories);
        body.destinations = new ArrayList<>(selectedDestinations);

        binding.btnSave.setEnabled(false);
        profileApi.putPreferences(body).enqueue(new retrofit2.Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<Object>> call,
                                   retrofit2.Response<ApiResponse<Object>> response) {
                if (!isAdded()) return;
                binding.btnSave.setEnabled(true);
                if (response.isSuccessful()) {
                    preferencesStore.save(new ArrayList<>(selectedCategories), new ArrayList<>(selectedDestinations));
                    Toast.makeText(getContext(), "Preferencias guardadas", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(PreferencesFragment.this).popBackStack();
                } else {
                    Toast.makeText(getContext(), ApiErrorParser.extractMessage(response), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
                if (!isAdded()) return;
                binding.btnSave.setEnabled(true);
                Toast.makeText(getContext(), "Sin conexión, reintentá", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String prettyCategory(String key) {
        switch (key) {
            case "free_tour": return "Free Tour";
            case "guided_visit": return "Visita Guiada";
            case "excursion": return "Excursión";
            case "gastronomic": return "Gastronómica";
            case "adventure": return "Aventura";
            default: return key;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
```

- [ ] **Step 4: Agregar al `nav_graph.xml`**

Dentro de `<navigation>`:

```xml
<fragment
    android:id="@+id/preferencesFragment"
    android:name="com.example.androidapp.ui.profile.PreferencesFragment"
    android:label="Mis preferencias"/>
```

En el fragment del `profileFragment`, agregar acción:

```xml
<action
    android:id="@+id/action_profile_to_preferences"
    app:destination="@id/preferencesFragment"/>
```

- [ ] **Step 5: Modificar `ProfileFragment` para navegar a Preferences**

En `ProfileFragment.java`, en `onViewCreated`:

```java
binding.btnPreferences.setOnClickListener(v ->
    NavHostFragment.findNavController(this).navigate(R.id.action_profile_to_preferences)
);
```

Si no existe el botón en `fragment_profile.xml`, agregarlo:

```xml
<Button
    android:id="@+id/btnPreferences"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Mis preferencias"
    android:layout_margin="12dp"/>
```

- [ ] **Step 6: Build + smoke**

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Manual:
- Login → Perfil → "Mis preferencias" → seleccionar categorías + destinos
- Guardar → Toast → vuelve a Perfil
- Home → "💙 Para vos" → ahora trae lista no vacía

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/androidapp/ui/profile/PreferencesFragment.java app/src/main/res/layout/fragment_preferences.xml app/src/main/res/navigation/nav_graph.xml app/src/main/java/com/example/androidapp/ui/profile/ProfileFragment.java app/src/main/res/layout/fragment_profile.xml app/src/main/java/com/example/androidapp/data/remote/ProfileApi.java app/src/main/java/com/example/androidapp/data/model/PreferencesRequest.java app/src/main/java/com/example/androidapp/data/model/PreferencesResponse.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(catalog): add PreferencesFragment for user travel preferences"
```

---

## Task 16: End-to-end QA + push branch

**Files:** ninguno (validación)

- [ ] **Step 1: Ejecutar checklist QA del spec sección 6 (Catálogo)**

Correr en device físico, paso a paso:

- [ ] Home → ver imágenes reales cargando (no placeholders) en cada card
- [ ] Detalle → galería vertical con 4-5 fotos visibles scrolleando
- [ ] Tap en foto de galería → abre visor del sistema
- [ ] Detalle → "Ver en mapa" → abre Google Maps con pin en coords correctas
- [ ] Detalle en device sin app de mapas → toast "No tenés un app de mapas instalado"
- [ ] Filtros → seteo destino + categoría + precio max → lista cambia
- [ ] Filtros → fecha específica → solo aparecen actividades con esa fecha en `dates[]`
- [ ] Filtros sin resultados → mensaje + botón "Limpiar filtros" funciona
- [ ] Scroll en Home hasta el final con chip "Todas" → carga page 2
- [ ] Chip "Para vos" sin preferencias → CTA "Configurá tus preferencias"
- [ ] Configurar preferencias en Perfil → guardar → chip "Para vos" trae lista no vacía
- [ ] Chip "Destacadas" → solo actividades con `featured=true` (verificar contra `FRONTEND_REFERENCE.md` — debería ser 7)
- [ ] Modo avión → toasts "Sin conexión" en lugar de crashes

- [ ] **Step 2: Verificar cambios al backend deployados**

```bash
curl -s https://uadetpodda1backend-production.up.railway.app/api/activities/a1 | grep -E "imageUrl"
```

Expected: ver URL de Unsplash, no `images.example.com`. Si sale fake, el PR backend no está mergeado o no se redeployó.

- [ ] **Step 3: Push branch para PR**

```bash
git push -u origin feature/catalog-consigna-3
```

- [ ] **Step 4: Crear PR (manual)**

Abrir GitHub web (o usar `gh pr create`) y crear PR de `feature/catalog-consigna-3` → `main`:

- Título: `feat: cierre consigna 3 (Catálogo completo con imágenes reales, filtros, paginación, preferencias)`
- Body: copiar el resumen del spec y la lista de tasks completadas. Mencionar dependencia del PR backend `feature/unsplash-curated-images`.

---

## Self-review

| Check | Resultado |
|---|---|
| Cobertura de spec | Cada sub-consigna del spec sección 3 (Catálogo) tiene una task: imágenes reales con Glide (T2 backend + T5 cards + T7 hero), galería vertical (T8), filtros combinados (T11+T13), sección Destacadas/Recomendadas como chips (T14), paginación (T14 scroll listener), detalle completo (T7+T8+T9), punto de encuentro con mapa (T9), preferencias del usuario (T10+T15). |
| Placeholders | Las URLs específicas de Unsplash en T2 son `XXXXXXXXXXXX` deliberados — el implementador busca y reemplaza con IDs reales. Sustituibles 1-a-1, no son TBDs ambiguos. |
| Type consistency | `Filters` (T11) usado en `FiltersFragment` (T13), `HomeFragment` (T14) y `FilterQueryBuilder` (T11) con la misma signatura. `PreferencesStore.hasAnyPreference()` y `save()` usados igual en T14 y T15. |
| Dependencias entre tasks | T1 bloquea T3-T15. T2 (backend) bloquea las QA visuales pero no el código mobile. T11 bloquea T13 y T14. T10 bloquea T14 y T15. T13 bloquea T14 (recibe resultado vía setFragmentResult). |
| TDD | No aplicado por restricción del spec. Cada task termina con build verification + smoke manual. |

---

## Conjunto completo (Plan A + Plan B)

Una vez ambos PRs mergeados a `main` del repo mobile + el PR del backend mergeado a `main` + Railway redeployado:

- **Consigna 1 (Auth)** queda 100% cerrada
- **Consigna 3 (Catálogo)** queda 100% cerrada
- Total: ~33 tasks, ~30 commits, 2 PRs mobile + 1 PR backend
