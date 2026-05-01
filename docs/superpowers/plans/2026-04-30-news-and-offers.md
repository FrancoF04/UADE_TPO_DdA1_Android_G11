# News, Offers and Featured Destinations — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the "Noticias" tab (Feature 9 — Consigna 9) showing news, offers and featured destinations from `/api/news`, with offline cache, conditional navigation to activity detail, and a binary chip ("Oferta" / "Destacado").

**Architecture:** New nested nav graph + new tab in `BottomNavigationView`. Two fragments (`NewsFragment` list, `NewsDetailFragment`). Single Hilt-injected `NewsApi` (already provided), single Hilt-injected `NewsCache` backed by an internal-storage JSON file. Click on item routes to `ActivityDetailFragment` (existing) when `activityId != null`, otherwise to `NewsDetailFragment`.

**Tech Stack:** Java 11, Android Views, Navigation Component, Hilt, Retrofit + Gson, Glide (new dependency), JUnit 4 (unit), AndroidX Test (instrumented).

**Spec reference:** `docs/superpowers/specs/2026-04-30-news-offers-featured-destinations-design.md`

**Pre-task setup:** every task assumes the working tree is clean and the current branch is the one chosen by the user. Commit messages are in English (global CLAUDE.md rule); never add `Co-Authored-By: Claude` (global CLAUDE.md rule).

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `gradle/libs.versions.toml` | modify | Add `glide` version + library entry |
| `app/build.gradle.kts` | modify | `implementation(libs.glide)` + Glide annotation processor |
| `app/src/main/java/com/example/androidapp/di/NetworkModule.java` | modify | `provideGson()` (new), `provideRetrofit` refactor to share `Gson`, `provideNewsCache` (new). `provideNewsApiService` already exists at line 103. |
| `app/src/main/java/com/example/androidapp/data/model/News.java` | create | POJO for `/api/news` list items + `hasRelatedActivity()` helper |
| `app/src/main/java/com/example/androidapp/data/model/NewsDetail.java` | create | Extends `News` with `content` for `/api/news/:id` |
| `app/src/main/java/com/example/androidapp/data/remote/NewsApi.java` | modify | Replace `Object` payloads with typed `News` / `NewsDetail` |
| `app/src/main/java/com/example/androidapp/data/local/NewsCache.java` | create | Read/write `news_cache.json` in `getFilesDir()` via Gson |
| `app/src/main/res/drawable/chip_offer.xml` | create | Rounded green pill background |
| `app/src/main/res/drawable/chip_destacado.xml` | create | Rounded blue pill background |
| `app/src/main/res/drawable/ic_news_24.xml` | create | 24dp vector icon for bottom nav |
| `app/src/main/res/layout/item_news.xml` | create | Horizontal list row (thumb + title + desc + chip) |
| `app/src/main/res/layout/fragment_news.xml` | create | List + offline banner + loading + empty + error states |
| `app/src/main/res/layout/fragment_news_detail.xml` | create | ScrollView with image + title + date + content |
| `app/src/main/java/com/example/androidapp/ui/news/NewsAdapter.java` | create | `BaseAdapter` for `item_news.xml` with chip logic |
| `app/src/main/java/com/example/androidapp/ui/news/NewsDetailFragment.java` | create | Fetch `/api/news/:id` and render full content |
| `app/src/main/java/com/example/androidapp/ui/news/NewsFragment.java` | create | List screen, cache-first + network refresh + conditional click navigation |
| `app/src/main/res/navigation/news_nav_graph.xml` | create | `newsFragment` ↔ `newsDetailFragment` ↔ cross-graph action to `activityDetailFragment` |
| `app/src/main/res/navigation/nav_graph.xml` | modify | Add `<include app:graph="@navigation/news_nav_graph" />` |
| `app/src/main/res/menu/bottom_nav_menu.xml` | modify | Add `<item android:id="@+id/news_nav_graph" />` |
| `app/src/main/java/com/example/androidapp/MainActivity.java` | modify | Add `R.id.newsFragment` to the visibility `if` (line 41). No manual listener — `NavigationUI` handles selection automatically. |
| `app/src/test/java/com/example/androidapp/data/model/NewsTest.java` | create | Unit tests for `News.hasRelatedActivity()` |
| `app/src/androidTest/java/com/example/androidapp/data/local/NewsCacheTest.java` | create | Instrumented round-trip test (real `Context.getFilesDir()`) |

---

## Task 1: Add Glide dependency and a single Gson provider in Hilt

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/example/androidapp/di/NetworkModule.java`

- [ ] **Step 1.1: Add Glide to the version catalog**

Open `gradle/libs.versions.toml` and add the version + library lines (keep the rest untouched).

In the `[versions]` block, append:
```toml
glide = "4.16.0"
```

In the `[libraries]` block, append:
```toml
glide = { group = "com.github.bumptech.glide", name = "glide", version.ref = "glide" }
```

- [ ] **Step 1.2: Wire Glide into the app module**

Open `app/build.gradle.kts`. Inside the `dependencies { ... }` block (currently lines 48–65), append two lines after `implementation(libs.okhttp)`:
```kotlin
    implementation(libs.glide)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
```

- [ ] **Step 1.3: Add a single `Gson` provider and refactor `provideRetrofit` to use it**

Open `app/src/main/java/com/example/androidapp/di/NetworkModule.java`.

Add this import near the existing imports:
```java
import com.google.gson.Gson;
```

Inside the class, **add** this provider (anywhere after `provideTokenManager`, before `provideRetrofit`):
```java
    @Provides
    @Singleton
    public Gson provideGson() {
        return new Gson();
    }
```

**Replace** the existing `provideRetrofit` (currently lines 60–68) with:
```java
    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
```

- [ ] **Step 1.4: Verify Gradle sync builds**

Run:
```
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`. If it fails because of Glide annotation-processor version mismatch, double-check the `4.16.0` version is the same in both lines.

- [ ] **Step 1.5: Commit**

```
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/example/androidapp/di/NetworkModule.java
git commit -m "chore: add Glide dependency and centralize Gson provider"
```

---

## Task 2: `News` model with TDD for `hasRelatedActivity()`

**Files:**
- Test: `app/src/test/java/com/example/androidapp/data/model/NewsTest.java`
- Create: `app/src/main/java/com/example/androidapp/data/model/News.java`

- [ ] **Step 2.1: Write the failing unit test**

Create `app/src/test/java/com/example/androidapp/data/model/NewsTest.java`:
```java
package com.example.androidapp.data.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import java.lang.reflect.Field;

public class NewsTest {

    @Test
    public void hasRelatedActivity_returnsTrue_whenActivityIdIsNonEmpty() throws Exception {
        News n = new News();
        setField(n, "activityId", "a1");
        assertTrue(n.hasRelatedActivity());
    }

    @Test
    public void hasRelatedActivity_returnsFalse_whenActivityIdIsNull() {
        News n = new News();
        assertFalse(n.hasRelatedActivity());
    }

    @Test
    public void hasRelatedActivity_returnsFalse_whenActivityIdIsEmptyString() throws Exception {
        News n = new News();
        setField(n, "activityId", "");
        assertFalse(n.hasRelatedActivity());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
```

> Reflection is used to set private fields because `News` has no setters (the model is populated by Gson). This is acceptable for unit tests of POJOs.

- [ ] **Step 2.2: Run the test and confirm it fails to compile**

Run:
```
./gradlew :app:testDebugUnitTest --tests "com.example.androidapp.data.model.NewsTest"
```
Expected: compilation FAIL with `error: cannot find symbol class News`.

- [ ] **Step 2.3: Create the `News` POJO**

Create `app/src/main/java/com/example/androidapp/data/model/News.java`:
```java
package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class News {

    @SerializedName("id")
    private String id;

    @SerializedName("image")
    private String image;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("activityId")
    private String activityId;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId() { return id; }
    public String getImage() { return image; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getActivityId() { return activityId; }
    public String getCreatedAt() { return createdAt; }

    public boolean hasRelatedActivity() {
        return activityId != null && !activityId.isEmpty();
    }
}
```

- [ ] **Step 2.4: Run the test and confirm it passes**

Run:
```
./gradlew :app:testDebugUnitTest --tests "com.example.androidapp.data.model.NewsTest"
```
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 2.5: Commit**

```
git add app/src/main/java/com/example/androidapp/data/model/News.java app/src/test/java/com/example/androidapp/data/model/NewsTest.java
git commit -m "feat: add News model with hasRelatedActivity helper and unit tests"
```

---

## Task 3: `NewsDetail` model

**Files:**
- Create: `app/src/main/java/com/example/androidapp/data/model/NewsDetail.java`

- [ ] **Step 3.1: Create `NewsDetail` extending `News`**

Create `app/src/main/java/com/example/androidapp/data/model/NewsDetail.java`:
```java
package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class NewsDetail extends News {

    @SerializedName("content")
    private String content;

    public String getContent() { return content; }
}
```

- [ ] **Step 3.2: Build to confirm it compiles**

Run:
```
./gradlew :app:compileDebugJavaWithJavac
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3.3: Commit**

```
git add app/src/main/java/com/example/androidapp/data/model/NewsDetail.java
git commit -m "feat: add NewsDetail model for /api/news/:id payload"
```

---

## Task 4: Type `NewsApi` with concrete payloads

**Files:**
- Modify: `app/src/main/java/com/example/androidapp/data/remote/NewsApi.java`

- [ ] **Step 4.1: Replace `Object` placeholders with typed payloads**

Replace the entire body of `app/src/main/java/com/example/androidapp/data/remote/NewsApi.java` with:
```java
package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.News;
import com.example.androidapp.data.model.NewsDetail;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NewsApi {

    @GET("/news")
    Call<ApiResponse<List<News>>> getNews(@Query("page") Integer page,
                                          @Query("page_size") Integer pageSize);

    @GET("/news/{id}")
    Call<ApiResponse<NewsDetail>> getNewsById(@Path("id") String id);
}
```

- [ ] **Step 4.2: Build**

Run:
```
./gradlew :app:compileDebugJavaWithJavac
```
Expected: `BUILD SUCCESSFUL`. If a compile error mentions `ApiResponse` not found, the import path is correct (`com.example.androidapp.data.model.ApiResponse`); the file already exists in the project per `CLAUDE.md`.

- [ ] **Step 4.3: Commit**

```
git add app/src/main/java/com/example/androidapp/data/remote/NewsApi.java
git commit -m "refactor: type NewsApi payloads as News and NewsDetail"
```

---

## Task 5: `NewsCache` with instrumented round-trip test

**Files:**
- Test: `app/src/androidTest/java/com/example/androidapp/data/local/NewsCacheTest.java`
- Create: `app/src/main/java/com/example/androidapp/data/local/NewsCache.java`
- Modify: `app/src/main/java/com/example/androidapp/di/NetworkModule.java`

- [ ] **Step 5.1: Write the failing instrumented test**

Create `app/src/androidTest/java/com/example/androidapp/data/local/NewsCacheTest.java`:
```java
package com.example.androidapp.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.example.androidapp.data.model.News;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NewsCacheTest {

    private Context ctx;
    private NewsCache cache;

    @Before
    public void setUp() {
        ctx = ApplicationProvider.getApplicationContext();
        cache = new NewsCache(ctx, new Gson());
        new File(ctx.getFilesDir(), "news_cache.json").delete();
    }

    @After
    public void tearDown() {
        new File(ctx.getFilesDir(), "news_cache.json").delete();
    }

    @Test
    public void exists_isFalse_whenFileMissing() {
        assertFalse(cache.exists());
    }

    @Test
    public void read_returnsNull_whenFileMissing() {
        assertNull(cache.read());
    }

    @Test
    public void save_then_read_roundTripsItems() throws Exception {
        News a = makeNews("n1", "a1", "Promo");
        News b = makeNews("n3", null, "Destino");
        cache.save(Arrays.asList(a, b));

        assertTrue(cache.exists());
        List<News> out = cache.read();
        assertNotNull(out);
        assertEquals(2, out.size());
        assertEquals("n1", out.get(0).getId());
        assertEquals("a1", out.get(0).getActivityId());
        assertEquals("n3", out.get(1).getId());
        assertNull(out.get(1).getActivityId());
    }

    @Test
    public void save_emptyList_readsBackEmpty() {
        cache.save(Collections.<News>emptyList());
        List<News> out = cache.read();
        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    private static News makeNews(String id, String activityId, String title) throws Exception {
        News n = new News();
        setField(n, "id", id);
        setField(n, "activityId", activityId);
        setField(n, "title", title);
        return n;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
```

> The instrumented test needs `androidx.test:core` (provides `ApplicationProvider`). It is pulled transitively by the existing `androidTestImplementation(libs.ext.junit)` dependency, so no new entry is required.

- [ ] **Step 5.2: Run the test and confirm compile failure**

Run:
```
./gradlew :app:assembleDebugAndroidTest
```
Expected: compilation FAIL with `error: cannot find symbol class NewsCache`.

- [ ] **Step 5.3: Create `NewsCache`**

Create `app/src/main/java/com/example/androidapp/data/local/NewsCache.java`:
```java
package com.example.androidapp.data.local;

import android.content.Context;
import android.util.Log;

import com.example.androidapp.data.model.News;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class NewsCache {

    private static final String TAG = "NewsCache";
    private static final String FILE_NAME = "news_cache.json";

    private final Context context;
    private final Gson gson;

    @Inject
    public NewsCache(@ApplicationContext Context context, Gson gson) {
        this.context = context;
        this.gson = gson;
    }

    public void save(List<News> items) {
        String json = gson.toJson(items);
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.w(TAG, "save failed", e);
        }
    }

    public List<News> read() {
        File f = new File(context.getFilesDir(), FILE_NAME);
        if (!f.exists()) return null;
        try (FileInputStream fis = context.openFileInput(FILE_NAME)) {
            int size = (int) f.length();
            byte[] bytes = new byte[size];
            int read = 0;
            while (read < size) {
                int r = fis.read(bytes, read, size - read);
                if (r < 0) break;
                read += r;
            }
            String json = new String(bytes, 0, read, StandardCharsets.UTF_8);
            Type t = new TypeToken<List<News>>(){}.getType();
            return gson.fromJson(json, t);
        } catch (IOException | JsonSyntaxException e) {
            Log.w(TAG, "read failed", e);
            return null;
        }
    }

    public boolean exists() {
        return new File(context.getFilesDir(), FILE_NAME).exists();
    }
}
```

- [ ] **Step 5.4: Provide `NewsCache` from `NetworkModule`**

Open `app/src/main/java/com/example/androidapp/di/NetworkModule.java`. Make sure these imports exist (some may already be present from Task 1 — keep each only once):
```java
import android.content.Context;
import com.example.androidapp.data.local.NewsCache;
import com.google.gson.Gson;
```

Append at the end of the class, after `provideNewsApiService`:
```java
    @Provides
    @Singleton
    public NewsCache provideNewsCache(@ApplicationContext Context context, Gson gson) {
        return new NewsCache(context, gson);
    }
```

- [ ] **Step 5.5: Run the instrumented tests**

Make sure an emulator or connected device is available, then run:
```
./gradlew :app:connectedDebugAndroidTest --tests "com.example.androidapp.data.local.NewsCacheTest"
```
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

> If no device is available, the engineer must spin up an emulator first. Document this expectation if running in CI.

- [ ] **Step 5.6: Commit**

```
git add app/src/main/java/com/example/androidapp/data/local/NewsCache.java app/src/androidTest/java/com/example/androidapp/data/local/NewsCacheTest.java app/src/main/java/com/example/androidapp/di/NetworkModule.java
git commit -m "feat: add NewsCache backed by internal storage and instrumented round-trip test"
```

---

## Task 6: Drawables (chips + bottom-nav icon)

**Files:**
- Create: `app/src/main/res/drawable/chip_offer.xml`
- Create: `app/src/main/res/drawable/chip_destacado.xml`
- Create: `app/src/main/res/drawable/ic_news_24.xml`

- [ ] **Step 6.1: `chip_offer.xml`**

Create `app/src/main/res/drawable/chip_offer.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#1E8E3E" />
    <corners android:radius="10dp" />
    <padding
        android:left="8dp"
        android:right="8dp"
        android:top="2dp"
        android:bottom="2dp" />
</shape>
```

- [ ] **Step 6.2: `chip_destacado.xml`**

Create `app/src/main/res/drawable/chip_destacado.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#1A73E8" />
    <corners android:radius="10dp" />
    <padding
        android:left="8dp"
        android:right="8dp"
        android:top="2dp"
        android:bottom="2dp" />
</shape>
```

- [ ] **Step 6.3: `ic_news_24.xml`**

Create `app/src/main/res/drawable/ic_news_24.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorOnSurface">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M20,3H4C2.9,3 2,3.9 2,5v14c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V5C22,3.9 21.1,3 20,3zM20,19H4V5h16V19zM6,7h8v2H6V7zM6,11h12v2H6V11zM6,15h12v2H6V15z"/>
</vector>
```

- [ ] **Step 6.4: Build to ensure resources compile**

Run:
```
./gradlew :app:processDebugResources
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6.5: Commit**

```
git add app/src/main/res/drawable/chip_offer.xml app/src/main/res/drawable/chip_destacado.xml app/src/main/res/drawable/ic_news_24.xml
git commit -m "feat: add drawables for news chips and bottom-nav icon"
```

---

## Task 7: Layouts (item, list, detail)

**Files:**
- Create: `app/src/main/res/layout/item_news.xml`
- Create: `app/src/main/res/layout/fragment_news.xml`
- Create: `app/src/main/res/layout/fragment_news_detail.xml`

- [ ] **Step 7.1: `item_news.xml`**

Create `app/src/main/res/layout/item_news.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="12dp">

    <ImageView
        android:id="@+id/ivThumb"
        android:layout_width="80dp"
        android:layout_height="80dp"
        android:scaleType="centerCrop"
        android:contentDescription="@null"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/tvChip"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="#FFFFFF"
        android:textSize="11sp"
        android:textStyle="bold"
        android:background="@drawable/chip_destacado"
        android:layout_marginStart="12dp"
        app:layout_constraintStart_toEndOf="@id/ivThumb"
        app:layout_constraintTop_toTopOf="@id/ivThumb" />

    <TextView
        android:id="@+id/tvTitle"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:textStyle="bold"
        android:textSize="15sp"
        android:maxLines="1"
        android:ellipsize="end"
        android:layout_marginStart="12dp"
        android:layout_marginTop="6dp"
        app:layout_constraintStart_toEndOf="@id/ivThumb"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toBottomOf="@id/tvChip" />

    <TextView
        android:id="@+id/tvDescription"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:textSize="13sp"
        android:textColor="#666666"
        android:maxLines="2"
        android:ellipsize="end"
        android:layout_marginStart="12dp"
        android:layout_marginTop="2dp"
        app:layout_constraintStart_toEndOf="@id/ivThumb"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toBottomOf="@id/tvTitle" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 7.2: `fragment_news.xml`**

Create `app/src/main/res/layout/fragment_news.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/offlineBanner"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="8dp"
        android:gravity="center"
        android:textColor="#7A5A00"
        android:background="#FFF4C2"
        android:text="Modo sin conexión — mostrando última lista guardada"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent" />

    <ListView
        android:id="@+id/lvNews"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:divider="@android:color/transparent"
        android:dividerHeight="4dp"
        app:layout_constraintTop_toBottomOf="@id/offlineBanner"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <ProgressBar
        android:id="@+id/loading"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent" />

    <LinearLayout
        android:id="@+id/emptyState"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center"
        android:visibility="gone"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="No hay noticias disponibles"
            android:textSize="15sp" />
    </LinearLayout>

    <LinearLayout
        android:id="@+id/errorState"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center"
        android:visibility="gone"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="No se pudieron cargar las noticias"
            android:textSize="15sp" />

        <Button
            android:id="@+id/btnRetry"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="Reintentar" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 7.3: `fragment_news_detail.xml`**

Create `app/src/main/res/layout/fragment_news_detail.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <ImageView
            android:id="@+id/ivImage"
            android:layout_width="match_parent"
            android:layout_height="220dp"
            android:scaleType="centerCrop"
            android:contentDescription="@null" />

        <TextView
            android:id="@+id/tvTitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="20sp"
            android:textStyle="bold"
            android:padding="16dp" />

        <TextView
            android:id="@+id/tvDate"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="12sp"
            android:textColor="#888888"
            android:paddingStart="16dp"
            android:paddingEnd="16dp" />

        <TextView
            android:id="@+id/tvContent"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="14sp"
            android:padding="16dp" />

        <ProgressBar
            android:id="@+id/loading"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:visibility="gone" />

        <TextView
            android:id="@+id/errorState"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="16dp"
            android:text="No se pudo cargar la noticia"
            android:textColor="#B00020"
            android:visibility="gone" />
    </LinearLayout>
</ScrollView>
```

- [ ] **Step 7.4: Build resources**

Run:
```
./gradlew :app:processDebugResources
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7.5: Commit**

```
git add app/src/main/res/layout/item_news.xml app/src/main/res/layout/fragment_news.xml app/src/main/res/layout/fragment_news_detail.xml
git commit -m "feat: add layouts for news list, item and detail"
```

---

## Task 8: `NewsAdapter`

**Files:**
- Create: `app/src/main/java/com/example/androidapp/ui/news/NewsAdapter.java`

- [ ] **Step 8.1: Create the adapter**

Create directory `app/src/main/java/com/example/androidapp/ui/news/` if needed, then create `NewsAdapter.java`:
```java
package com.example.androidapp.ui.news;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.androidapp.R;
import com.example.androidapp.data.model.News;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends BaseAdapter {

    private final Context context;
    private final List<News> items = new ArrayList<>();

    public NewsAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<News> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public News getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_news, parent, false);
            holder = new ViewHolder();
            holder.ivThumb = convertView.findViewById(R.id.ivThumb);
            holder.tvTitle = convertView.findViewById(R.id.tvTitle);
            holder.tvDescription = convertView.findViewById(R.id.tvDescription);
            holder.tvChip = convertView.findViewById(R.id.tvChip);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        News n = items.get(position);
        holder.tvTitle.setText(n.getTitle());
        holder.tvDescription.setText(n.getDescription());

        if (n.hasRelatedActivity()) {
            holder.tvChip.setText("Oferta");
            holder.tvChip.setBackgroundResource(R.drawable.chip_offer);
        } else {
            holder.tvChip.setText("Destacado");
            holder.tvChip.setBackgroundResource(R.drawable.chip_destacado);
        }

        if (n.getImage() != null && !n.getImage().isEmpty()) {
            Glide.with(context).load(n.getImage()).into(holder.ivThumb);
        } else {
            holder.ivThumb.setImageDrawable(null);
        }

        return convertView;
    }

    private static class ViewHolder {
        ImageView ivThumb;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvChip;
    }
}
```

- [ ] **Step 8.2: Build**

Run:
```
./gradlew :app:compileDebugJavaWithJavac
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8.3: Commit**

```
git add app/src/main/java/com/example/androidapp/ui/news/NewsAdapter.java
git commit -m "feat: add NewsAdapter with chip and Glide-loaded thumbnail"
```

---

## Task 9: Nav graph for the news feature

**Files:**
- Create: `app/src/main/res/navigation/news_nav_graph.xml`
- Modify: `app/src/main/res/navigation/nav_graph.xml`

- [ ] **Step 9.1: Create `news_nav_graph.xml`**

Create `app/src/main/res/navigation/news_nav_graph.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/news_nav_graph"
    app:startDestination="@id/newsFragment">

    <fragment
        android:id="@+id/newsFragment"
        android:name="com.example.androidapp.ui.news.NewsFragment"
        android:label="Noticias"
        tools:layout="@layout/fragment_news">

        <action
            android:id="@+id/action_news_to_news_detail"
            app:destination="@id/newsDetailFragment" />

        <action
            android:id="@+id/action_news_to_activity_detail"
            app:destination="@id/activityDetailFragment" />
    </fragment>

    <fragment
        android:id="@+id/newsDetailFragment"
        android:name="com.example.androidapp.ui.news.NewsDetailFragment"
        android:label="Detalle de noticia"
        tools:layout="@layout/fragment_news_detail">
        <argument
            android:name="newsId"
            app:argType="string" />
    </fragment>
</navigation>
```

> The action `action_news_to_activity_detail` resolves cross-graph because `nav_graph.xml` includes `activity_nav_graph` (which contains `activityDetailFragment`).

- [ ] **Step 9.2: Add include to root `nav_graph.xml`**

Open `app/src/main/res/navigation/nav_graph.xml`. Add this line right before the closing `</navigation>`, after the existing `historial_nav_graph` include:
```xml
    <include app:graph="@navigation/news_nav_graph" />
```

- [ ] **Step 9.3: Build**

Run:
```
./gradlew :app:processDebugResources
```
Expected: it will complain about `NewsFragment` and `NewsDetailFragment` classes not existing (we'll create them in Tasks 10 and 11). That's fine for now — the resource compilation step itself should pass at the XML schema level. **If the navigation tooling fails the build, skip the verification here and run it again at the end of Task 11.**

- [ ] **Step 9.4: Commit**

```
git add app/src/main/res/navigation/news_nav_graph.xml app/src/main/res/navigation/nav_graph.xml
git commit -m "feat: add nested news_nav_graph and include in root nav_graph"
```

---

## Task 10: `NewsDetailFragment`

**Files:**
- Create: `app/src/main/java/com/example/androidapp/ui/news/NewsDetailFragment.java`

- [ ] **Step 10.1: Create the fragment**

Create `app/src/main/java/com/example/androidapp/ui/news/NewsDetailFragment.java`:
```java
package com.example.androidapp.ui.news;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.androidapp.R;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.NewsDetail;
import com.example.androidapp.data.remote.NewsApi;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class NewsDetailFragment extends Fragment {

    private static final String TAG = "NewsDetailFragment";

    @Inject NewsApi newsApi;

    private ImageView ivImage;
    private TextView tvTitle;
    private TextView tvDate;
    private TextView tvContent;
    private ProgressBar loading;
    private TextView errorState;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ivImage = view.findViewById(R.id.ivImage);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvDate = view.findViewById(R.id.tvDate);
        tvContent = view.findViewById(R.id.tvContent);
        loading = view.findViewById(R.id.loading);
        errorState = view.findViewById(R.id.errorState);

        String newsId = getArguments() != null ? getArguments().getString("newsId") : null;
        if (newsId == null) {
            errorState.setVisibility(View.VISIBLE);
            return;
        }

        loading.setVisibility(View.VISIBLE);
        newsApi.getNewsById(newsId).enqueue(new Callback<ApiResponse<NewsDetail>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<NewsDetail>> call,
                                   @NonNull Response<ApiResponse<NewsDetail>> response) {
                if (!isAdded()) return;
                loading.setVisibility(View.GONE);
                ApiResponse<NewsDetail> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess() && body.getData() != null) {
                    render(body.getData());
                } else {
                    errorState.setVisibility(View.VISIBLE);
                    Log.e(TAG, "load failed status=" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<NewsDetail>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loading.setVisibility(View.GONE);
                errorState.setVisibility(View.VISIBLE);
                Log.e(TAG, "load failed", t);
            }
        });
    }

    private void render(NewsDetail d) {
        tvTitle.setText(d.getTitle());
        tvDate.setText(d.getCreatedAt() != null ? d.getCreatedAt() : "");
        tvContent.setText(d.getContent());
        if (d.getImage() != null && !d.getImage().isEmpty()) {
            Glide.with(this).load(d.getImage()).into(ivImage);
        }
    }
}
```

> `tvDate` uses the raw ISO string for now. If a follow-up wants to format it via `DateTimeUtils` (already in the project), that's a 1-line change — out of scope for this plan to avoid touching the helper.

- [ ] **Step 10.2: Verify the response wrapper exposes `isSuccess()`**

The code calls `body.isSuccess()` on `ApiResponse`. Confirm by running:
```
./gradlew :app:compileDebugJavaWithJavac
```
Expected: `BUILD SUCCESSFUL`. If a method-not-found error appears for `isSuccess()` or `getData()`, open `data/model/ApiResponse.java` to check the actual method names and adjust the calls (the project already uses these methods in other fragments per CLAUDE.md, so this is a sanity check).

- [ ] **Step 10.3: Commit**

```
git add app/src/main/java/com/example/androidapp/ui/news/NewsDetailFragment.java
git commit -m "feat: add NewsDetailFragment with Glide image and content rendering"
```

---

## Task 11: `NewsFragment` (cache-first list, conditional click navigation)

**Files:**
- Create: `app/src/main/java/com/example/androidapp/ui/news/NewsFragment.java`

- [ ] **Step 11.1: Create the fragment**

Create `app/src/main/java/com/example/androidapp/ui/news/NewsFragment.java`:
```java
package com.example.androidapp.ui.news;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidapp.R;
import com.example.androidapp.data.local.NewsCache;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.News;
import com.example.androidapp.data.remote.NewsApi;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class NewsFragment extends Fragment {

    private static final String TAG = "NewsFragment";

    @Inject NewsApi newsApi;
    @Inject NewsCache newsCache;

    private NewsAdapter adapter;
    private ListView lvNews;
    private TextView offlineBanner;
    private ProgressBar loading;
    private LinearLayout emptyState;
    private LinearLayout errorState;
    private Button btnRetry;

    private boolean cachedShown = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lvNews = view.findViewById(R.id.lvNews);
        offlineBanner = view.findViewById(R.id.offlineBanner);
        loading = view.findViewById(R.id.loading);
        emptyState = view.findViewById(R.id.emptyState);
        errorState = view.findViewById(R.id.errorState);
        btnRetry = view.findViewById(R.id.btnRetry);

        adapter = new NewsAdapter(requireContext());
        lvNews.setAdapter(adapter);

        lvNews.setOnItemClickListener((parent, v, position, id) -> onItemClick(adapter.getItem(position)));
        btnRetry.setOnClickListener(v -> fetch());

        List<News> cached = newsCache.read();
        if (cached != null) {
            adapter.setItems(cached);
            cachedShown = true;
        } else {
            loading.setVisibility(View.VISIBLE);
        }

        fetch();
    }

    private void fetch() {
        errorState.setVisibility(View.GONE);
        newsApi.getNews(null, null).enqueue(new Callback<ApiResponse<List<News>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<News>>> call,
                                   @NonNull Response<ApiResponse<List<News>>> response) {
                if (!isAdded()) return;
                loading.setVisibility(View.GONE);
                ApiResponse<List<News>> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess() && body.getData() != null) {
                    List<News> items = body.getData();
                    adapter.setItems(items);
                    newsCache.save(items);
                    offlineBanner.setVisibility(View.GONE);
                    emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    handleFailure(null, "status=" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<News>>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                loading.setVisibility(View.GONE);
                handleFailure(t, null);
            }
        });
    }

    private void handleFailure(Throwable t, String detail) {
        Log.e(TAG, "fetch failed " + (detail != null ? detail : ""), t);
        if (cachedShown) {
            offlineBanner.setVisibility(View.VISIBLE);
        } else {
            errorState.setVisibility(View.VISIBLE);
        }
    }

    private void onItemClick(News item) {
        if (item == null) return;
        Bundle args = new Bundle();
        if (item.hasRelatedActivity()) {
            args.putString("activityId", item.getActivityId());
            args.putBoolean("showReserveButton", true);
            args.putBoolean("showSpotsField", true);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_news_to_activity_detail, args);
        } else {
            args.putString("newsId", item.getId());
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_news_to_news_detail, args);
        }
    }
}
```

- [ ] **Step 11.2: Verify `ActivityDetailFragment` accepts the `activityId` arg name**

In an unrelated nav graph (likely `home_nav_graph.xml` or `activity_nav_graph.xml`), confirm the fragment declares `<argument android:name="activityId" .../>`. If the existing argument key is different (for example `activity_id`), update the `args.putString(...)` line in `onItemClick` to match. Search:

```
grep -rn "activityId\|activity_id" app/src/main/res/navigation/
```
Pick the spelling that the existing code uses for `ActivityDetailFragment`. Same logic for `showReserveButton` and `showSpotsField` — verify they match the names in CLAUDE.md (the file lists these exactly, so they should be correct).

- [ ] **Step 11.3: Build**

Run:
```
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 11.4: Commit**

```
git add app/src/main/java/com/example/androidapp/ui/news/NewsFragment.java
git commit -m "feat: add NewsFragment with cache-first load and conditional navigation"
```

---

## Task 12: Bottom-nav menu and `MainActivity` visibility

**Files:**
- Modify: `app/src/main/res/menu/bottom_nav_menu.xml`
- Modify: `app/src/main/java/com/example/androidapp/MainActivity.java`

- [ ] **Step 12.1: Add the menu item**

Open `app/src/main/res/menu/bottom_nav_menu.xml`. Append a new `<item>` before the closing `</menu>`:
```xml
    <item
        android:id="@+id/news_nav_graph"
        android:icon="@drawable/ic_news_24"
        android:title="Noticias" />
```

> The id `news_nav_graph` matches the `<navigation android:id="@+id/news_nav_graph">` in `news_nav_graph.xml`. `NavigationUI.setupWithNavController` resolves the destination automatically — no manual listener changes needed.

- [ ] **Step 12.2: Show the bottom bar on `newsFragment`**

Open `app/src/main/java/com/example/androidapp/MainActivity.java`. Replace the `if` condition on line 41 (currently `destination.getId() == R.id.homeFragment || destination.getId() == R.id.reservasFragment`) with:
```java
            if (destination.getId() == R.id.homeFragment
                    || destination.getId() == R.id.reservasFragment
                    || destination.getId() == R.id.newsFragment) {
```

- [ ] **Step 12.3: Build**

Run:
```
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 12.4: Commit**

```
git add app/src/main/res/menu/bottom_nav_menu.xml app/src/main/java/com/example/androidapp/MainActivity.java
git commit -m "feat: add Noticias tab to bottom navigation"
```

---

## Task 13: Smoke test on a real device / emulator

**No code changes — verification only. Do not commit anything.**

- [ ] **Step 13.1: Install on a connected device**

Run:
```
./gradlew :app:installDebug
```

- [ ] **Step 13.2: Launch the app and verify the checklist**

Open the app via scrcpy or device. Verify each item:

1. The bottom nav shows 5 tabs: Home, Mis Reservas, Favoritos, Perfil, Noticias.
2. Tapping "Noticias" shows the list with 3 items (n1, n2, n3 per `FRONTEND_REFERENCE.md`).
3. The chip on n1 and n2 reads "Oferta" (green); the chip on n3 reads "Destacado" (blue).
4. Tapping n1 navigates to the activity detail screen for `a1` and the "Reservar" button is visible.
5. Tapping n3 opens `NewsDetailFragment` and shows the long `content` text.
6. Enable airplane mode, kill the app, relaunch. Tapping "Noticias" shows the same 3 items + a yellow banner ("Modo sin conexión — mostrando última lista guardada").
7. Clear app storage, enable airplane mode, launch. Tapping "Noticias" shows the error state with the "Reintentar" button.

- [ ] **Step 13.3: If any check fails**

Diagnose and fix in a follow-up commit. The most common failure modes:
- **Bottom nav stays hidden on `newsFragment`** → Step 12.2 condition wasn't applied; re-check the `if`.
- **Click on n1 doesn't navigate** → the `activityId` arg name mismatch (see Step 11.2). The cross-graph action requires `activityDetailFragment` to be declared inside another nav graph included from the root `nav_graph.xml` — confirmed already (see file structure table).
- **Glide doesn't load images** → confirm the device has internet and that `image` URLs in the response are reachable (check Logcat for `Glide` warnings).

---

## Task 14 (Optional / Stretch): Espresso test for click navigation

> **Skip on first delivery.** This task adds `hilt-android-testing` (not currently in the catalog) and pulls in extra plumbing. The smoke checklist in Task 13 is the primary acceptance gate. Tackle this only after the feature is in `main` and the team agrees to invest in Espresso infrastructure.

The high-level steps would be:

1. Add to `gradle/libs.versions.toml`:
   ```toml
   hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }
   ```
2. In `app/build.gradle.kts` `dependencies { ... }`:
   ```kotlin
   androidTestImplementation(libs.hilt.android.testing)
   androidTestAnnotationProcessor(libs.hilt.compiler)
   ```
3. Provide a fake `NewsApi` via `@TestInstallIn(replaces = NetworkModule.class)` and assert with `Navigation.findNavController()` that clicking an item with `activityId == "a1"` ends up at `R.id.activityDetailFragment`, while clicking an item with `activityId == null` ends up at `R.id.newsDetailFragment`.

The detailed implementation is left to a follow-up plan once the team commits to Hilt-aware Espresso testing.

---

## Out of Scope (do not implement)

- Pull-to-refresh, infinite scroll, paginación.
- Caching of `/api/news/:id` detail responses.
- Push notifications for new news items.
- Migrating `ActivityAdapter` and `HistorialAdapter` to Glide.
- "Mark as read" / read-tracking.
- Search or filters inside the News tab.
- Updating `DateTimeUtils` to format `createdAt` for display.

## Acceptance gate

The plan is complete when:
- All 12 core tasks are committed.
- `./gradlew :app:assembleDebug` builds clean.
- `./gradlew :app:testDebugUnitTest` passes (Task 2 tests).
- `./gradlew :app:connectedDebugAndroidTest` passes (Task 5 tests) — requires emulator/device.
- The Task 13 smoke checklist passes on a real device.
