# Feature 9 — Noticias, Ofertas y Destinos Destacados

**Fecha:** 2026-04-30
**Repo:** `mobile-app-android` (rama `main`)
**Stack:** Java + Android Views + Navigation Component + Hilt + Retrofit + Gson

---

## 1. Objetivo

Agregar un tab nuevo en la app llamado **Noticias** que liste novedades, descuentos, nuevos destinos y promociones de XploreNow. Las noticias vienen del backend (`/api/news`) y se muestran con imagen, título y descripción breve. Al tocar una noticia:

- Si tiene `activityId` → navega al detalle de la actividad relacionada (reutiliza `ActivityDetailFragment`).
- Si no tiene `activityId` → abre el detalle completo de la noticia (`NewsDetailFragment` con `content`).

## 2. Restricciones de la cátedra (PROJECT_CONTEXT.md)

Todo el diseño se ajusta a lo habilitado por la cátedra. Tecnologías permitidas y usadas en este feature:

- Java (no Kotlin)
- Activities + Fragments + Navigation Component (Clase 1 y 3)
- Retrofit + Gson + OkHttp (Clase 3)
- Hilt para DI (Clase 5)
- Files internos privados con `openFileOutput()` y `getFilesDir()` (Clase 6)
- Glide para imágenes (Clase 6)
- JUnit 4 + AndroidX Test + Espresso (testing)

No se usa: ViewModel, LiveData, RecyclerView, RxJava, Coroutines, DataStore (la cátedra lo habilita pero su API en Java es incómoda y no aporta sobre archivos internos).

## 3. Decisiones de producto

| Decisión | Elección | Razón |
|---|---|---|
| Ubicación del feature | Tab dedicado en `BottomNavigationView` | Comentario de CLAUDE.md ("agregar más destinos al listener"); aísla del estado de Home. |
| Comportamiento al tocar ítem | Mutuamente exclusivo: con `activityId` → detalle de actividad; sin `activityId` → detalle de noticia | Lectura literal del requerimiento; reusa `ActivityDetailFragment` sin modificarlo. |
| Layout del ítem | Horizontal (thumb 80×80 + título + descripción) | Consistencia con `HistorialAdapter` y `ActivityAdapter`. |
| Diferenciación visual | Chip binario por `activityId`: "Oferta" si tiene actividad relacionada, "Destacado" si no | Cumple con que el feature se llama "Noticias, Ofertas y Destinos" sin pedir campos al backend. |
| Caché offline | Sí, lista completa en archivo interno JSON | Clase 6 habilita files internos; primera milla de modo sin conexión sin sumar dependencias. |
| Paginación | No | Backend tiene 3 ítems pre-cargados; el endpoint admite `page`/`limit` opcionales y los pasamos `null`. |

## 4. Estructura de paquetes y archivos

### Nuevos

```
app/src/main/java/com/example/androidapp/
├── data/
│   ├── model/
│   │   ├── News.java
│   │   └── NewsDetail.java
│   └── local/
│       └── NewsCache.java
└── ui/
    └── news/
        ├── NewsFragment.java
        ├── NewsAdapter.java
        └── NewsDetailFragment.java

app/src/main/res/
├── layout/
│   ├── fragment_news.xml
│   ├── item_news.xml
│   └── fragment_news_detail.xml
├── navigation/
│   └── news_nav_graph.xml
└── drawable/
    ├── chip_offer.xml
    ├── chip_destacado.xml
    └── ic_news_24.xml

app/src/test/java/com/example/androidapp/ui/news/
└── NewsTest.java

app/src/androidTest/java/com/example/androidapp/ui/news/
├── NewsFragmentTest.java
└── NewsCacheInstrumentedTest.java
```

### Modificados

- `app/src/main/java/com/example/androidapp/data/remote/NewsApi.java` — tipar `Object` → `News` / `NewsDetail`.
- `app/src/main/java/com/example/androidapp/di/NetworkModule.java` — `provideNewsApiService` ya existe (línea ~103). Sumar `@Provides @Singleton Gson provideGson()` y `@Provides @Singleton NewsCache` con `@ApplicationContext`. Refactorizar `provideRetrofit` para que reciba `Gson` y use `GsonConverterFactory.create(gson)` (compartir la misma instancia).
- `app/src/main/java/com/example/androidapp/MainActivity.java` — sumar caso `R.id.news_nav_graph` al listener del bottom nav y al control de visibilidad de la barra (visible en `newsFragment`, oculta en `newsDetailFragment`).
- `app/src/main/res/menu/bottom_nav_menu.xml` — sumar `<item android:id="@+id/news_nav_graph" />` (mismo patrón que `home_nav_graph` y `mis_reservas_nav_graph`).
- `app/src/main/res/navigation/nav_graph.xml` — `<include app:graph="@navigation/news_nav_graph" />`.
- `gradle/libs.versions.toml` — sumar `glide` (versión `4.16.0`).
- `app/build.gradle.kts` — `implementation(libs.glide)` + `annotationProcessor` del compiler de Glide.

## 5. Capa de datos

### `data/model/News.java`

POJO de listado.

```java
public class News {
    @SerializedName("id")          private String id;
    @SerializedName("image")       private String image;
    @SerializedName("title")       private String title;
    @SerializedName("description") private String description;
    @SerializedName("activityId")  private String activityId;  // puede ser null
    @SerializedName("createdAt")   private String createdAt;

    // getters
    public boolean hasRelatedActivity() {
        return activityId != null && !activityId.isEmpty();
    }
}
```

### `data/model/NewsDetail.java`

Extiende `News` con el campo `content` (texto largo del detalle).

```java
public class NewsDetail extends News {
    @SerializedName("content") private String content;
    public String getContent() { return content; }
}
```

### `data/remote/NewsApi.java` (refactor)

```java
public interface NewsApi {
    @GET("/news")
    Call<ApiResponse<List<News>>> getNews(@Query("page") Integer page,
                                          @Query("page_size") Integer pageSize);

    @GET("/news/{id}")
    Call<ApiResponse<NewsDetail>> getNewsById(@Path("id") String id);
}
```

> El endpoint **no requiere auth**. El interceptor de `NetworkModule` que agrega `Authorization: Bearer <token>` se sigue aplicando, pero el backend lo ignora en `/api/news`. No hace falta tocar el interceptor.

### `data/local/NewsCache.java`

Caché simple en archivo interno privado. Solo cachea la **lista**, no el detalle.

```java
@Singleton
public class NewsCache {
    private static final String FILE = "news_cache.json";
    private final Context context;
    private final Gson gson;

    @Inject
    public NewsCache(@ApplicationContext Context context, Gson gson) {
        this.context = context;
        this.gson = gson;
    }

    public void save(List<News> items) {
        try (FileOutputStream fos = context.openFileOutput(FILE, Context.MODE_PRIVATE)) {
            fos.write(gson.toJson(items).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.w("NewsCache", "save failed", e);
        }
    }

    public List<News> read() {
        File f = new File(context.getFilesDir(), FILE);
        if (!f.exists()) return null;
        try (FileInputStream fis = context.openFileInput(FILE)) {
            byte[] bytes = new byte[(int) f.length()];
            fis.read(bytes);
            String json = new String(bytes, StandardCharsets.UTF_8);
            Type t = new TypeToken<List<News>>(){}.getType();
            return gson.fromJson(json, t);
        } catch (IOException | JsonSyntaxException e) {
            Log.w("NewsCache", "read failed", e);
            return null;
        }
    }

    public boolean exists() {
        return new File(context.getFilesDir(), FILE).exists();
    }
}
```

**Estructura del JSON cacheado** (`news_cache.json` en `getFilesDir()`):

```json
[
  {
    "id": "n1",
    "image": "https://example.com/promo.jpg",
    "title": "Promo especial en Buenos Aires",
    "description": "Descuentos en actividades...",
    "activityId": "a1",
    "createdAt": "2026-04-01T10:00:00Z"
  }
]
```

Campos: `id` (string), `image` (URL string), `title` (string), `description` (string), `activityId` (string nullable), `createdAt` (ISO-8601 UTC, `YYYY-MM-DDTHH:mm:ssZ`). Mismo shape que el response de `/api/news`.

### `di/NetworkModule.java` — providers nuevos / cambios

`NewsApi` ya está provisto (línea 103 del módulo actual). Cambios:

```java
// NUEVO — único productor de Gson en la app
@Provides @Singleton
public Gson provideGson() {
    return new Gson();
}

// REFACTOR — recibir Gson y compartir la misma instancia con la fábrica
@Provides @Singleton
public Retrofit provideRetrofit(OkHttpClient okHttpClient, Gson gson) {
    return new Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();
}

// NUEVO
@Provides @Singleton
public NewsCache provideNewsCache(@ApplicationContext Context context, Gson gson) {
    return new NewsCache(context, gson);
}
```

## 6. UI

### Bottom navigation

`res/menu/bottom_nav_menu.xml` agrega:

```xml
<item android:id="@+id/news_nav_graph"
      android:title="Noticias"
      android:icon="@drawable/ic_news_24" />
```

> El ID matchea el `@+id/news_nav_graph` declarado en el nav graph anidado, así `BottomNavigationView` resuelve el destino y mantiene un back stack independiente por tab — mismo patrón que `home_nav_graph` y `mis_reservas_nav_graph` ya en uso. **Nota sobre cantidad de ítems:** el menú actual tiene 4 (Home, Mis Reservas, Favoritos, Perfil); con Noticias pasa a 5, el máximo de Material para `BottomNavigationView` en modo fixed.

`MainActivity` suma `R.id.news_nav_graph` al `OnItemSelectedListener` del bottom nav (mismo patrón que `R.id.home_nav_graph` y `R.id.mis_reservas_nav_graph`) y a la lógica que controla la visibilidad de la barra (visible en `newsFragment`, oculta en `newsDetailFragment` igual que en `activityDetailFragment`).

### `res/navigation/news_nav_graph.xml`

```xml
<navigation
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
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
        <argument android:name="newsId" app:argType="string" />
    </fragment>
</navigation>
```

`res/navigation/nav_graph.xml` raíz agrega:

```xml
<include app:graph="@navigation/news_nav_graph" />
```

> El destino `@id/activityDetailFragment` ya está declarado en otro nav graph anidado; la acción cross-graph funciona como ya lo hace `Historial → ActivityDetailFragment` y `MisReservas → ActivityDetailFragment`.

### `res/layout/fragment_news.xml`

`ConstraintLayout` raíz con:

- `TextView` `offlineBanner` arriba (fondo amarillo, `visibility="gone"`).
- `ProgressBar` `loading` centrado (`visibility="gone"`).
- `ListView` `lvNews` ocupando el espacio restante.
- Bloque vertical `emptyState` (icono + "No hay noticias disponibles", `visibility="gone"`).
- Bloque vertical `errorState` (icono + "No se pudieron cargar las noticias" + `Button` "Reintentar", `visibility="gone"`).

### `res/layout/item_news.xml`

`ConstraintLayout` (alto wrap, ~96dp con padding):

- `ImageView` `ivThumb` 80×80 a la izquierda. Cargada con Glide; placeholder y error con un drawable simple.
- `TextView` `tvTitle` (1 línea, ellipsize end, `textStyle=bold`).
- `TextView` `tvDescription` (2 líneas, ellipsize end, color secundario).
- `TextView` `tvChip` esquina superior derecha. Texto "Oferta" o "Destacado", `background` = `chip_offer.xml` o `chip_destacado.xml`, padding chico, font size pequeño.

> Si en el futuro se suma un `Button` dentro del ítem (no es el caso ahora), agregar `android:focusable="false"` para no romper el `OnItemClickListener` (CLAUDE.md, convención del proyecto).

### `res/layout/fragment_news_detail.xml`

`ScrollView` raíz con `LinearLayout` vertical:

- `ImageView` `ivImage` ancho completo, ratio 16:9, Glide.
- `TextView` `tvTitle` (h1).
- `TextView` `tvDate` (formato local del `createdAt`, helper `DateTimeUtils` ya existe).
- `TextView` `tvContent` (sin `maxLines`).
- `ProgressBar` y `errorState` similares al fragment lista.

### `res/drawable/chip_offer.xml` y `chip_destacado.xml`

`<shape android:shape="rectangle">` con `<corners android:radius="..."/>` y `<solid>` (verde y azul respectivamente). Sin gradients ni efectos.

### `res/drawable/ic_news_24.xml`

Vector drawable de 24×24, ícono simple de noticia (rectángulos con líneas). Mantener consistencia con los otros íconos del bottom nav.

## 7. Lógica de los Fragments

### `NewsFragment.onViewCreated`

Pseudocódigo:

```
1. Mostrar caché si existe:
   List<News> cached = newsCache.read();
   if (cached != null) {
       adapter.setItems(cached);
       cachedShown = true;
   } else {
       loading.setVisibility(VISIBLE);
   }

2. newsApi.getNews(null, null).enqueue(new Callback<>() {
       onResponse:
           if (!isAdded()) return;
           loading.setVisibility(GONE);
           if (response.isSuccessful() && body.isSuccess()) {
               List<News> items = body.getData();
               adapter.setItems(items);
               newsCache.save(items);
               offlineBanner.setVisibility(GONE);
               errorState.setVisibility(GONE);
               emptyState.setVisibility(items.isEmpty() ? VISIBLE : GONE);
           } else {
               handleNetworkFailure(null);
           }
       onFailure:
           if (!isAdded()) return;
           handleNetworkFailure(t);
   });

3. handleNetworkFailure(Throwable t):
       loading.setVisibility(GONE);
       Log.e("NewsFragment", "fetch failed", t);
       if (cachedShown) {
           offlineBanner.setVisibility(VISIBLE);
       } else {
           errorState.setVisibility(VISIBLE);
       }
```

Botón "Reintentar" del `errorState` repite el paso 2.

### Click en ítem (`NewsFragment`)

```java
lvNews.setOnItemClickListener((parent, view, position, id) -> {
    News item = adapter.getItem(position);
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
});
```

### `NewsDetailFragment.onViewCreated`

```
1. String newsId = getArguments().getString("newsId");
2. loading.setVisibility(VISIBLE);
3. newsApi.getNewsById(newsId).enqueue(...);
   onResponse success:
       NewsDetail d = body.getData();
       Glide.with(this).load(d.getImage()).into(ivImage);
       tvTitle.setText(d.getTitle());
       tvDate.setText(DateTimeUtils.format(d.getCreatedAt()));
       tvContent.setText(d.getContent());
   onResponse error / onFailure:
       errorState.setVisibility(VISIBLE);
       Log.e("NewsDetailFragment", "fetch failed", t);
```

> `DateTimeUtils.format` ya existe en `util/`. Si el método para parsear "createdAt" ISO no existe, sumar uno (mismo patrón de los otros métodos del helper).

### `NewsAdapter`

`BaseAdapter` clásico con ViewHolder, mismo patrón que `HistorialAdapter`. Glide para `ivThumb`. Lógica del chip:

```java
if (item.hasRelatedActivity()) {
    tvChip.setText("Oferta");
    tvChip.setBackgroundResource(R.drawable.chip_offer);
} else {
    tvChip.setText("Destacado");
    tvChip.setBackgroundResource(R.drawable.chip_destacado);
}
```

## 8. Dependencias nuevas

`gradle/libs.versions.toml`:

```toml
[versions]
glide = "4.16.0"

[libraries]
glide = { group = "com.github.bumptech.glide", name = "glide", version.ref = "glide" }
```

`app/build.gradle.kts`:

```kotlin
implementation(libs.glide)
annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
```

Sin RxJava, sin Guava, sin DataStore.

## 9. Testing

### Unit (`src/test/java/.../ui/news/`)

- `NewsTest` — `hasRelatedActivity()` con `null`, `""`, `"a1"`.

### Instrumentado (`src/androidTest/java/.../ui/news/`)

- `NewsCacheInstrumentedTest` — usa `getInstrumentation().getTargetContext()` para `getFilesDir()` real. Round-trip `save() → read()` con lista no vacía y lista vacía. Verifica que `read()` devuelve `null` cuando el archivo no existe.
- `NewsFragmentTest` — render con `NewsApi` mockeado en Hilt (test module). Click en un ítem con `activityId` debe llamar `NavController.navigate(R.id.action_news_to_activity_detail, ...)`. Click en un ítem sin `activityId` debe ir a `R.id.action_news_to_news_detail`.

### Smoke manual

`./gradlew assembleDebug`, instalar, abrir con scrcpy:

1. Tab "Noticias" visible y clickeable.
2. Lista carga 3 ítems con sus chips correctos (n1 Oferta, n2 Oferta, n3 Destacado).
3. Click en n1 → `ActivityDetailFragment` con la actividad `a1`, botón Reservar visible.
4. Click en n3 → `NewsDetailFragment` con el `content` largo.
5. Modo avión + relanzar app → tab Noticias muestra la última lista cacheada con banner amarillo.
6. Modo avión + primera apertura limpia → `errorState` con botón Reintentar.

## 10. Out of scope

- Búsqueda y filtros dentro de Noticias.
- Pull-to-refresh.
- Paginación / scroll infinito.
- Notificaciones push de nuevas noticias.
- Caché del detalle individual (`/api/news/:id`) — solo se cachea la lista.
- Migrar `ActivityAdapter` y `HistorialAdapter` para usar Glide (PR aparte; esta sí integra Glide al proyecto, allana el camino).
- Marcar noticias como "vistas" / contar lecturas.

## 11. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| `activityId` apunta a una actividad inexistente o eliminada | El `ActivityDetailFragment` ya maneja respuestas 404 con su propio error state — no hace falta validación previa. |
| `image` URL rota o lenta | Glide muestra placeholder/error drawable; no bloquea el render del título y descripción. |
| Cache JSON malformado tras update de modelo | `read()` captura `JsonSyntaxException` y devuelve `null` → fetch de red normal, sobrescribe el cache. |
| Backend reinicia y la noticia con `activityId` queda apuntando a una actividad que ya no existe en memoria | Mismo manejo que el riesgo 1 (404 en el detalle de actividad). |
| Bottom nav llega a 5 ítems (Home, Mis Reservas, Favoritos, Perfil, Noticias) — el máximo soportado por Material en modo fixed | Aceptable hoy. Si la cátedra agrega un sexto destino, hay que decidir entre shifting mode o agrupar destinos secundarios en un menú "Más". |

## 12. Plan de integración

- Rama: `feature/news-and-offers` (prefijo `feature/` por PROJECT_CONTEXT.md).
- Una sola PR a `main`. Reviewer habitual del equipo.
- Sin pushes directos a `main` (regla de cátedra).
