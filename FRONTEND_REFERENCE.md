# Backend Reference — UADE TPO DdA1

> **Para Claude en una nueva sesión:** Este archivo es el contexto completo del proyecto. Leelo antes de responder cualquier pregunta.
> El usuario está desarrollando el **frontend Android en Java** que consume este backend.
> El backend YA ESTÁ HECHO y deployado. El trabajo actual es el frontend Android.
> Si el usuario trae modificaciones del backend, reflejarlo en este archivo.

> Backend Node.js + Express, sin base de datos real (datos en memoria).
> URL de producción: `https://uadetpodda1backend-production.up.railway.app`
> Repo backend: `UADE_TPO_DdA1_Backend` (este repo)
> Repo frontend Android: separado (Java)
> Archivos subidos (fotos de perfil): `<base_url>/uploads/users/<filename>` — prefijar base URL siempre.

---

## Estructura del proyecto

```
src/
├── app.js                    Configuración Express + middlewares + montaje de rutas
├── server.js                 Entry point
├── data/data.js              "Base de datos" en memoria
├── routes/
│   ├── auth.routes.js
│   ├── user.routes.js
│   ├── activity.routes.js
│   ├── bookings.routes.js    NUEVO — reservas
│   ├── favorites.routes.js   NUEVO — favoritos
│   ├── ratings.routes.js     NUEVO — calificaciones
│   ├── news.routes.js        NUEVO — noticias
│   ├── profile.routes.js     NUEVO — perfil (alternativa a /api/users)
│   └── health.routes.js
├── middleware/
│   ├── auth.js               Validación de token por sesión
│   └── errorHandler.js
└── utils/
    ├── otp.js
    ├── token.js              Token custom base64url (NO JWT estándar)
    ├── validation.js
    ├── response.js
    ├── pagination.js
    └── activityView.js       Serializa actividades y bookings (agrega meetingPoint + itineraryPoints)
```

---

## Modelos de datos

### Usuario

```json
{
  "id": "u1",
  "email": "juan@example.com",
  "username": "juanperez",
  "password": "<hash bcrypt, nunca se retorna>",
  "fullName": "Juan Perez",
  "phoneNumber": "+5491112345678",
  "profilePhotoUrl": "",
  "activities": [],
  "preferences": {
    "categories": ["free_tour", "adventure"],
    "destinations": ["Buenos Aires"]
  },
  "createdAt": "2026-01-15T10:00:00Z"
}
```

### Actividad

```json
{
  "id": "a1",
  "name": "Walking Tour por San Telmo",
  "destination": "Buenos Aires",
  "category": "free_tour",
  "description": "Recorre las calles empedradas...",
  "imageUrl": "https://...",
  "galleryUrls": ["https://...", "https://..."],
  "duration": "2.5 horas",
  "price": 0,
  "currency": "ARS",
  "availableSpots": 56,
  "totalSpots": 80,
  "date": "2026-04-10T10:00:00Z",
  "dates": [
    "2026-04-10T10:00:00.000Z",
    "2026-04-17T10:00:00.000Z",
    "2026-04-24T10:00:00.000Z",
    "2026-05-01T10:00:00.000Z"
  ],
  "schedules": [
    { "id": "a1-s1", "date": "2026-04-10T10:00:00.000Z", "availableSpots": 15, "totalSpots": 20 },
    { "id": "a1-s2", "date": "2026-04-17T10:00:00.000Z", "availableSpots": 14, "totalSpots": 20 },
    { "id": "a1-s3", "date": "2026-04-24T10:00:00.000Z", "availableSpots": 13, "totalSpots": 20 },
    { "id": "a1-s4", "date": "2026-05-01T10:00:00.000Z", "availableSpots": 12, "totalSpots": 20 }
  ],
  "dateTimes": [
    { "date": "2026-04-10", "time": "10:00" },
    { "date": "2026-04-17", "time": "10:00" },
    { "date": "2026-04-24", "time": "10:00" },
    { "date": "2026-05-01", "time": "10:00" }
  ],
  "meetingPoint": {
    "latitude": -34.6083,
    "longitude": -58.3712,
    "address": "Plaza de Mayo, frente al Cabildo, Buenos Aires"
  },
  "itineraryPoints": [],
  "guide": { "name": "Carlos Rodriguez", "rating": 4.8 },
  "language": "Espanol",
  "included": ["Guia bilingue", "Mapa del recorrido"],
  "cancellationPolicy": "Cancelacion gratuita hasta 24 horas antes",
  "featured": true,
  "createdAt": "2026-01-10T10:00:00Z"
}
```

**Notas sobre disponibilidad:**
- `availableSpots` / `totalSpots` en el nivel raíz = suma de todos los schedules (dinámico)
- Usar `schedules[].availableSpots` para saber la disponibilidad **por fecha específica**
- Cada actividad genera **4 fechas** separadas por **7 días** desde la fecha base
- `dateTimes` = versión split de `dates` en `{ date: "YYYY-MM-DD", time: "HH:mm" }` (conveniente para UI)
- `meetingPoint` siempre retorna objeto con `latitude`, `longitude`, `address` (nunca string)
- `itineraryPoints` = array de paradas (solo algunas actividades tienen datos, las demás retornan `[]`). Cada parada: `{ name, description, latitude, longitude }`
- `userRating` se incluye en `GET /api/activities/:id` si el usuario está autenticado y ya calificó

### Booking (Reserva)

```json
{
  "id": "b-...",
  "userId": "u1",
  "activityId": "a1",
  "activityName": "Walking Tour por San Telmo",
  "selectedDate": "2026-04-10T10:00:00.000Z",
  "selectedScheduleId": "a1-s1",
  "quantity": 1,
  "cancellationHours": 24,
  "cancellationPolicy": "Cancelacion gratuita hasta 24 horas antes",
  "status": "confirmed",
  "voucherCode": "...",
  "createdAt": "2026-04-28T...",
  "activity": { /* objeto actividad serializado completo, incluyendo meetingPoint e itineraryPoints */ },
  "meetingPoint": { "latitude": -34.6083, "longitude": -58.3712, "address": "..." }
}
```

### Noticia

```json
{
  "id": "n1",
  "category": "promocion",
  "image": "https://...",
  "title": "Promo especial en Buenos Aires",
  "description": "Descuentos en actividades...",
  "content": "Texto largo...",
  "activityId": "a1",
  "createdAt": "2026-04-01T10:00:00Z"
}
```

> `category`: string. Default `"noticia"`. Valores en datos pre-cargados: `"promocion"`, `"descuento"`, `"nuevo_destino"`, `"noticia"`. Presente tanto en el listado como en el detalle.

### Favorito (respuesta)

```json
{
  /* campos del objeto actividad serializado */
  "favoriteCreatedAt": "2026-04-28T...",
  "priceAtFavorite": 0,
  "spotsAtFavorite": 56,
  "priceChanged": false,
  "spotsChanged": false
}
```

### Rating (Calificación)

```json
{
  "id": "r...",
  "bookingId": "b-...",
  "userId": "u1",
  "activityRating": 5,
  "guideRating": 4,
  "comment": "Excelente experiencia",
  "createdAt": "2026-04-28T..."
}
```

### Valores posibles

| Campo | Valores |
|-------|---------|
| `category` | `free_tour`, `guided_visit`, `excursion`, `gastronomic`, `adventure` |
| `destination` | `Buenos Aires`, `Bariloche`, `Mendoza`, `Ushuaia`, `Córdoba`, `Salta` |
| `currency` | `ARS` |
| `language` | `Espanol` |
| `booking.status` | `confirmed` (activa/futura), `finalized` (pasada), `cancelled` |

**Lógica de transición de estados de booking:**
- `confirmed` → `finalized`: cuando `selectedDate + duración_actividad < ahora` (se evalúa en UTC-3, Argentina)
- `confirmed` → `cancelled`: cuando el usuario cancela dentro del plazo permitido
- El frontend debe filtrar por `status == "confirmed"` en "Mis Actividades" — el backend devuelve todos los estados en `GET /api/bookings`

---

## Formato de respuestas

Todas las respuestas usan el mismo formato:

```json
{ "success": true, "data": { ... } }
{ "success": true, "data": [ ... ], "meta": { "total": 15, "page": 1, "page_size": 10, "limit": 10 } }
{ "success": false, "error": "Mensaje de error" }
```

> **Nota:** La paginación ahora incluye `page_size` además de `limit`. Ambos tienen el mismo valor.

**Códigos HTTP:**
| Código | Significado |
|--------|-------------|
| 200 | OK |
| 201 | Creado |
| 400 | Bad Request (validación) |
| 401 | Unauthorized (token inválido o expirado) |
| 404 | Not Found |
| 409 | Conflict (duplicado, sin cupos, fuera de plazo) |
| 500 | Server Error |

---

## Autenticación

### Token

El token **NO es un JWT estándar**. Es un objeto JSON codificado en base64url:

```json
{
  "userId": "u1",
  "fullName": "Juan Perez",
  "type": "access",
  "issuedAt": "2026-05-01T10:00:00.000Z",
  "jti": "<uuid>",
  "expiresAt": "2026-05-01T11:00:00.000Z"
}
```

- Se envía en el header: `Authorization: Bearer <token>`
- El **access token** dura **60 minutos** por defecto (env var `SESSION_TTL_MINUTES`, mínimo 5 min)
- El **refresh token** dura **7 días** (env var `REFRESH_TOKEN_TTL_DAYS`)
- El campo `expiresAt` está en el payload — se puede leer para hacer refresh proactivo antes de que expire
- Las sesiones viven en memoria: si el servidor se reinicia, el token deja de funcionar

### Flujo OTP (recomendado para onboarding)

```
1. POST /api/auth/otp/request   { "email": "..." }
   → Backend genera código de 6 dígitos (se imprime en consola del server)

2. POST /api/auth/otp/verify    { "email": "...", "code": "123456" }
   → Si el usuario no existe, se crea automáticamente
     username = email.split('@')[0]
   → Retorna { token, accessToken, refreshToken }

3. (Opcional) POST /api/auth/otp/resend  { "email": "..." }
   → Invalida OTPs anteriores y genera uno nuevo
```

### Flujo Login Clásico

```
POST /api/auth/login
Body: { "username": "juanperez", "password": "password123" }
→ Retorna { token, accessToken, refreshToken }
```

### Flujo Registro Completo

```
POST /api/auth/register
Body: {
  "email": "user@example.com",
  "username": "juanperez",       (3-20 chars, solo alfanuméricos y _)
  "password": "password123",     (mínimo 6 chars)
  "fullName": "Juan Perez",
  "phoneNumber": "+5491112345678"
}
→ Retorna { token, accessToken, refreshToken }  (201)
```

> `token` y `accessToken` son el mismo valor — aliases del access token. Guardar ambos o solo uno (son iguales).

### Refresh de Token

```
POST /api/auth/refresh
Body: { "refreshToken": "..." }   (o via Authorization: Bearer <refreshToken>)
→ Retorna { token, accessToken, refreshToken }   (nuevo par, el refreshToken viejo queda invalidado)
```

**Cuándo usarlo:** cuando el servidor retorna 401, intentar primero un refresh. Si el refresh también retorna 401, llevar al usuario al login.

### Logout

```
POST /api/auth/logout
Headers: Authorization: Bearer <token>
→ Invalida la sesión en el servidor
```

---

## Endpoints — Referencia completa

### Health

```
GET /health
→ { "status": "ok", "timestamp": "...", "uptime": 3456.7 }
```

---

### Auth — `/api/auth` (alias: `/auth`)

| Método | Path | Auth | Body | Respuesta |
|--------|------|------|------|-----------|
| POST | `/otp/request` | No | `{ email }` | `{ message }` |
| POST | `/otp/verify` | No | `{ email, code }` | `{ token, accessToken, refreshToken }` |
| POST | `/otp/resend` | No | `{ email }` | `{ message }` |
| POST | `/register` | No | `{ email, username, password, fullName, phoneNumber }` | `{ token, accessToken, refreshToken }` 201 |
| POST | `/login` | No | `{ username, password }` | `{ token, accessToken, refreshToken }` |
| POST | `/refresh` | No | `{ refreshToken }` (o Bearer) | `{ token, accessToken, refreshToken }` |
| POST | `/logout` | **Sí** | — | `{ message }` |

---

### Usuarios — `/api/users`

| Método | Path | Auth | Body / Params | Respuesta |
|--------|------|------|---------------|-----------|
| GET | `/me` | **Sí** | — | `{ user }` (sin password) |
| PUT | `/me` | **Sí** | `{ username?, email?, fullName?, phoneNumber? }` | `{ user }` |
| GET | `/activities` | **Sí** | — | `{ detailedActivities: [{...},...] }` |
| POST | `/activities` | **Sí** | `{ activityId, selectedDate?, selectedScheduleId?, quantity? }` | `{ detailedActivities }` 201 |
| PUT | `/preferences` | **Sí** | `{ categories: [], destinations: [] }` | `{ user }` |
| DELETE | `/activities/:activityId/:selectedScheduleId` | **Sí** | — | 200 sin body |

**Notas:**
- `PUT /me` solo actualiza los campos presentes en el body
- `PUT /preferences` reemplaza el objeto preferences completo
- `DELETE /activities/:activityId/:selectedScheduleId` es un mecanismo de cancelación legacy sobre `user.activities` (por índice de schedule), independiente de `DELETE /api/bookings/:id`. Para cancelar reservas usar siempre `DELETE /api/bookings/:id` o `POST /api/bookings/:id/cancel` — son la vía recomendada y la que devuelve `cancellationPolicy` en la respuesta

---

### Perfil — `/api/profile` (alias: `/profile`)

Ruta alternativa y más flexible para operaciones de perfil. Acepta nombres de campo alternativos.

| Método | Path | Auth | Body | Respuesta |
|--------|------|------|------|-----------|
| GET | `/` o `/me` | **Sí** | — | `{ user: { id, name, fullName, email, phone, phoneNumber, profilePhotoUrl, photoUrl, preferences } }` |
| PATCH | `/` o `/me` | **Sí** | `{ name?, fullName?, phone?, phoneNumber?, photoUrl?, profilePhotoUrl? }` | `{ user }` |
| POST | `/photo` | **Sí** | `multipart/form-data`, campo `photo` | `{ user, photoUrl }` |
| GET | `/preferences` | **Sí** | — | `{ preferences: { categories, destinations } }` |
| PUT | `/preferences` | **Sí** | `{ categories: [] }` | `{ user }` |
| GET | `/bookings-summary` | **Sí** | — | `{ summary }` |

**`GET /api/profile/bookings-summary` — respuesta:**
```json
{
  "summary": {
    "totalBookings": 3,
    "confirmedBookings": 2,
    "cancelledBookings": 1,
    "finalizedBookings": 0,
    "upcomingBookings": 2,
    "completedBookings": 0,
    "totalSpent": 8000,
    "byStatus": { "confirmed": 2, "cancelled": 1 }
  }
}
```

> **Nota:** `/api/profile` y `/api/users` coexisten. `/api/profile` acepta aliases (`name` → `fullName`, `phone` → `phoneNumber`, `photoUrl` → `profilePhotoUrl`) y usa PATCH en lugar de PUT.

**`POST /api/profile/photo` — upload de foto de perfil:**
- Content-Type: `multipart/form-data`
- Campo del archivo: `photo`
- Formatos aceptados: JPEG, PNG, GIF, WebP
- Tamaño máximo: **5 MB** (413 si se excede)
- La URL guardada es **relativa**: `/uploads/users/<filename>` — hay que prefijar la base URL del servidor para mostrarla: `https://uadetpodda1backend-production.up.railway.app/uploads/users/<filename>`
- Respuesta: `{ user: { ...perfil }, photoUrl: "/uploads/users/<filename>" }`

---

### Actividades — `/api/activities` (alias: `/activities`)

| Método | Path | Auth | Query Params | Respuesta |
|--------|------|------|--------------|-----------|
| GET | `/` | No | `page`, `limit`, `destination`, `category`, `date`, `priceMin`, `priceMax` | `[activities]` + `meta` |
| GET | `/featured` | No | `limit` (default 5) | `[activities]` |
| GET | `/recommended` | **Sí** | — | `[activities]` basadas en preferencias |
| GET | `/filters` | No | — | `{ destinations: [], categories: [], dates: [] }` |
| GET | `/history` | **Sí** | `fecha_desde?`, `fecha_hasta?`, `destination?`, `page`, `limit` | `[historyItems]` + `meta` |
| GET | `/:id` | No* | — | `{ activity }` (incluye `userRating` si está autenticado) |
| POST | `/:id/image` | **Sí** | `multipart/form-data`, campo `image` | `{ activity, imageUrl }` |

**Filtros en `GET /api/activities`:**
- Todos son opcionales y se combinan con AND
- `date`: filtra contra **todas las fechas** del array `dates`. Ej: `"2026-04-10"` devuelve actividades que tengan esa fecha en alguno de sus schedules
- `priceMin`/`priceMax`: numérico (en ARS)
- `page` mínimo 1, `limit` entre 1 y 100 (default 10)

**`GET /api/activities/history` — estructura de cada item:**
```json
{
  "bookingId": "b-...",
  "activityId": "a1",
  "activityName": "Walking Tour por San Telmo",
  "destination": "Buenos Aires",
  "guide": { "name": "Carlos Rodriguez", "rating": 4.8 },
  "duration": "2.5 horas",
  "date": "2026-04-10T10:00:00.000Z"
}
```
> Para obtener la calificación de un item del historial: `GET /api/ratings/:bookingId` (usando el `bookingId` del item).

**`POST /api/activities/:id/image` — upload de imagen principal de actividad:**
- Content-Type: `multipart/form-data`
- Campo del archivo: `image`
- Formatos aceptados: JPEG, PNG, GIF, WebP; tamaño máximo **5 MB** (413 si se excede) — mismas reglas que el upload de foto de perfil
- La URL guardada es relativa: `/uploads/activities/<filename>` — prefijar la base URL del servidor para mostrarla
- Reemplaza el `imageUrl` de la actividad (no es una galería, es la imagen principal)

**`GET /api/activities/:id` — campos extra:**
- `meetingPoint`: siempre objeto `{ latitude, longitude, address }` (nunca string)
- `itineraryPoints`: array de paradas del recorrido (puede ser `[]`)
- `userRating`: si se envía token Bearer válido, incluye la calificación del usuario para esta actividad (o `null`)

---

### Reservas — `/api/bookings` (alias: `/bookings`, `/api/users/reservations`, `/reservations`)

| Método | Path | Auth | Body / Params | Respuesta |
|--------|------|------|---------------|-----------|
| POST | `/` | **Sí** | `{ activityId\|activity_id, selectedDate\|date\|fecha?, selectedScheduleId?, quantity\|participants? }` | `{ booking }` 201 |
| GET | `/` | **Sí** | `page`, `limit` | `[bookings]` + `meta` |
| GET | `/:id` | **Sí** | — | `{ booking }` |
| DELETE | `/:id` | **Sí** | — | `{ message, cancellationPolicy }` |
| POST | `/:id/cancel` | **Sí** | — | `{ message, cancellationPolicy }` |
| GET | `/offline-bundle` | **Sí** | — | `{ bookings, vouchers, activities, meetingPoints }` |
| POST | `/sync` | **Sí** | `{ since?, timestamp?, localState? }` | `{ since, serverTime, localState, changes }` |

**`POST /api/bookings` — body:**

El body requiere `activityId` + (`selectedDate`/`date`/`fecha` **o** `selectedScheduleId`). Quantity/participants es opcional (default 1):

```json
// Opción A: por fecha ISO
{ "activityId": "a1", "selectedDate": "2026-04-10T10:00:00Z" }

// Opción A (aliases)
{ "activity_id": "a1", "date": "2026-04-10", "time": "10:00" }
{ "activity_id": "a1", "fecha": "2026-04-10", "horario": "10:00" }

// Opción B: por schedule ID (recomendado)
{ "activityId": "a1", "selectedScheduleId": "a1-s2" }

// Con cantidad
{ "activityId": "a1", "selectedScheduleId": "a1-s2", "quantity": 2 }
```

**`POST /api/bookings` — respuesta exitosa (201):**
```json
{
  "success": true,
  "data": {
    "booking": {
      "id": "b-...",
      "activityId": "a1",
      "activityName": "Walking Tour por San Telmo",
      "selectedDate": "2026-04-10T10:00:00.000Z",
      "selectedScheduleId": "a1-s1",
      "quantity": 1,
      "status": "confirmed",
      "voucherCode": "...",
      "cancellationPolicy": "Cancelacion gratuita hasta 24 horas antes",
      "createdAt": "...",
      "activity": { /* actividad completa serializada */ },
      "meetingPoint": { "latitude": -34.6083, "longitude": -58.3712, "address": "..." }
    }
  }
}
```

**`DELETE /api/bookings/:id` y `POST /api/bookings/:id/cancel` — errores:**
| Código | Error |
|--------|-------|
| 404 | `"Reserva no encontrada"` |
| 409 | `"No se puede cancelar dentro del plazo permitido"` |

**`GET /api/bookings/offline-bundle` — respuesta:**
```json
{
  "bookings": [ /* bookings serializados */ ],
  "vouchers": [ { "bookingId": "b-...", "voucherCode": "..." } ],
  "activities": [ /* actividades serializadas */ ],
  "meetingPoints": [ { "bookingId": "b-...", "activityId": "a1", "meetingPoint": {...} } ]
}
```

**Errores de `POST /api/bookings`:**
| Código | Error |
|--------|-------|
| 400 | `"activity_id es requerido"` |
| 400 | `"fecha o selectedScheduleId es requerido"` |
| 404 | `"Actividad no encontrada"` |
| 409 | `"No hay cupos disponibles para la fecha seleccionada"` |

---

### Favoritos — `/api/favorites` (alias: `/favorites`)

| Método | Path | Auth | Body / Params | Respuesta |
|--------|------|------|---------------|-----------|
| POST | `/` | **Sí** | `{ activityId\|activity_id }` | `{ favorite }` 201 |
| DELETE | `/:activityId` | **Sí** | — | `{ message }` |
| GET | `/` | **Sí** | `page`, `limit` | `[favorites]` + `meta` |

**`POST /api/favorites` — respuesta (201):**
```json
{
  "favorite": {
    /* campos del objeto actividad serializado completo */
    "favoriteCreatedAt": "2026-04-28T...",
    "priceAtFavorite": 0,
    "spotsAtFavorite": 56,
    "priceChanged": false,
    "spotsChanged": false
  }
}
```

**Errores:**
| Código | Error |
|--------|-------|
| 400 | `"activity_id es requerido"` |
| 404 | `"Actividad no encontrada"` |
| 404 | `"Favorito no encontrado"` (en DELETE) |

---

### Calificaciones — `/api/ratings` (alias: `/ratings`)

| Método | Path | Auth | Body / Params | Respuesta |
|--------|------|------|---------------|-----------|
| POST | `/` | **Sí** | `{ bookingId\|booking_id, activityRating\|activity_rating, guideRating\|guide_rating, comment? }` | `{ rating }` 201 |
| GET | `/:bookingId` | **Sí** | — | `{ rating }` |

> ⚠️ **Estructura de respuesta anidada**: ambos endpoints devuelven `{ "data": { "rating": {...} } }`, no `{ "data": { campos directos } }`. En el front usar `ApiResponse<RatingData>` y llamar `.getData().getRating()`.

**`POST /api/ratings` — body:**
```json
{
  "bookingId": "b-...",
  "activityRating": 5,
  "guideRating": 4,
  "comment": "Excelente experiencia"
}
```

**Respuesta exitosa (estructura real):**
```json
{
  "success": true,
  "data": {
    "rating": {
      "id": "r...",
      "bookingId": "b-...",
      "userId": "u1",
      "activityRating": 5,
      "guideRating": 4,
      "comment": "Excelente experiencia",
      "createdAt": "2026-04-30T..."
    }
  }
}
```

**Reglas de calificación:**
- `activityRating` y `guideRating`: enteros entre 1 y 5 (obligatorios)
- `comment`: string, máximo 300 caracteres (opcional, puede ser `null`)
- Solo se puede calificar cuando la reserva ya está `finalized` (el backend recalcula el status en cada lectura)
- La ventana de calificación es de **48 horas** desde la **finalización** de la actividad (`selectedDate + duración`), consistente con la lógica de transición `confirmed`→`finalized`
- Cada reserva solo puede calificarse **una vez**

**Errores:**
| Código | Error |
|--------|-------|
| 400 | `"booking_id es requerido"` |
| 400 | `"Las puntuaciones deben estar entre 1 y 5"` |
| 400 | `"El comentario no puede superar 300 caracteres"` |
| 404 | `"Reserva no encontrada"` |
| 409 | `"La actividad aun no finalizo"` |
| 409 | `"La calificacion debe enviarse dentro de las 48 horas posteriores a la finalizacion"` |
| 409 | `"La reserva ya fue calificada"` |

---

### Noticias — `/api/news` (alias: `/news`)

| Método | Path | Auth | Query Params | Respuesta |
|--------|------|------|--------------|-----------|
| GET | `/` | No | `page`, `limit` | `[newsItems]` + `meta` |
| GET | `/:id` | No | — | `{ news }` |

**Formato de ítem de lista:**
```json
{
  "id": "n1",
  "category": "promocion",
  "image": "https://...",
  "title": "Promo especial en Buenos Aires",
  "description": "Descuentos en actividades...",
  "activityId": "a1",
  "createdAt": "2026-04-01T10:00:00Z"
}
```

**`GET /api/news/:id` agrega `content`** (texto largo completo).

**Noticias pre-cargadas:**
| ID | Título | category | activityId |
|----|--------|----------|------------|
| n1 | Promo especial en Buenos Aires | promocion | a1 |
| n2 | 20% off en experiencias de vino | descuento | a7 |
| n3 | Nuevo destino: Salta | nuevo_destino | a12 |
| n4 | XploreNow renueva su catálogo de actividades | noticia | null |
| n5 | 15% off en aventura patagónica | descuento | a4 |
| n6 | Nuevo destino: Córdoba | nuevo_destino | a11 |

---

### Notificaciones — `/api/notifications` (alias: `/notifications`)

| Método | Path | Auth | Respuesta |
|--------|------|------|-----------|
| GET | `/poll` | **Sí** | `{ events: [...] }` (200) o sin body (204) |

**Contrato de Long Polling**: el servidor retiene la respuesta hasta que haya novedades para el usuario autenticado o se cumplan **~25 segundos**, lo que ocurra primero. Si no hubo novedades, responde `204 No Content` (sin body) — el cliente debe volver a pedir de inmediato. Si hay novedades, responde `200` con `{ "success": true, "data": { "events": [...] } }`.

**Forma de un evento `reminder_24h`** (Feature 12.29 — recordatorio 24hs antes de la actividad):
```json
{
  "type": "reminder_24h",
  "bookingId": "b-...",
  "activityId": "a1",
  "activityName": "Walking Tour por San Telmo",
  "selectedDate": "2026-04-10T10:00:00.000Z",
  "voucherCode": "VCH-..."
}
```

- Se dispara una única vez por reserva `confirmed`, cuando faltan 24hs o menos para `selectedDate` (y no se disparó antes — el backend guarda un flag `reminderSentAt` en el propio booking, en memoria).
- El campo `type` es un discriminador pensado para reusarse: otros tipos de evento (ej. cancelación/reprogramación de la Feature 12.30) pueden agregarse a futuro sin cambiar el contrato del endpoint ni el cliente existente — cada consumidor del lado Android simplemente ignora los `type` que no reconoce.

**`GET /api/bookings/sync/poll`** (Feature 12.30 — avisos de cancelación/reprogramación): mismo contrato de Long Polling que `/api/notifications/poll`, pero clon de `POST /api/bookings/sync` — devuelve `{ since, serverTime, changes }` en vez de `{ events }`. Requiere auth. Query param opcional `since` (ISO string; si se omite, devuelve todos los cambios existentes). `changes[].changeType` puede ser `cancelled`, `finalized` o `updated` (una reprogramación de horario también cae en `updated` — comparar `selectedDate`/`selectedScheduleId` contra lo que tenga cacheado el cliente para detectarla).

> ⚠️ Nota de implementación: `getSyncChangesSince` (usada tanto por `/sync` como por `/sync/poll`) no filtra por usuario — devuelve cambios de reservas de **todos** los usuarios del sistema, no solo las del que llama. Es un comportamiento preexistente del endpoint `/sync` original, no introducido por `/sync/poll`; queda así a propósito (proyecto académico, no es necesario resolverlo).

### Operador — `/api/operator` (alias: `/operator`)

Endpoints de simulación para la Feature 12.30 (avisos de cancelación/reprogramación "por la operadora"). **Sin autenticación** — no existe un rol admin real en el backend, son una herramienta de testing/demo para poder disparar estos cambios manualmente (ej. desde curl/Postman o una mini-app aparte) mientras corre el server.

Operan sobre un **schedule completo de una actividad** (no sobre una reserva puntual), fiel al enunciado ("se cancela/reprograma la actividad") — afectan a todas las reservas `confirmed` que compartan ese `selectedScheduleId`, no a una sola.

| Método | Path | Auth | Body | Respuesta |
|--------|------|------|------|-----------|
| POST | `/activities/:activityId/schedules/:scheduleId/cancel` | No | — | `{ affectedBookings: [...] }` |
| POST | `/activities/:activityId/schedules/:scheduleId/reschedule` | No | `{ toScheduleId }` | `{ affectedBookings: [...] }` |

- Cancelar: marca como `cancelled` todas las reservas confirmadas de ese `scheduleId` (equivalente a lo que hace el usuario con `DELETE /api/bookings/:id`, pero sin las validaciones de plazo — el operador puede cancelar en cualquier momento, y afecta a todos los que reservaron ese horario, no solo a uno).
- Reprogramar: mueve todas esas reservas a otro `toScheduleId` **ya existente** de la misma actividad (no crea horarios nuevos) y resetea `reminderSentAt` de cada una para que el recordatorio de 24hs se recalcule contra la nueva fecha.
- `affectedBookings` puede ser un array vacío (`[]`) si no había reservas confirmadas para ese schedule — no es un error, es un 200 normal. 404 solo si la actividad (o el schedule destino, en el reschedule) no existen.
- Ambos cambios quedan reflejados en `booking.updatedAt` de cada reserva afectada, por lo que `GET /api/bookings/sync/poll` los detecta automáticamente sin necesitar ningún mecanismo adicional.

---

## Datos pre-cargados

### Usuarios de prueba

| Email | Username | Password |
|-------|----------|----------|
| juan@example.com | juanperez | password123 |
| maria@example.com | mariagarcia | password456 |

### Actividades (16 pre-cargadas)

| ID | Nombre | Destino | Categoría | Precio | Featured |
|----|--------|---------|-----------|--------|----------|
| a1 | Walking Tour por San Telmo | Buenos Aires | free_tour | 0 | ✓ |
| a2 | Free Tour por La Boca | Buenos Aires | free_tour | 0 | ✓ |
| a3 | Visita Guiada al Teatro Colón | Buenos Aires | guided_visit | 8000 | ✓ |
| a4 | Recorrido por el Museo MALBA | Buenos Aires | guided_visit | 6000 | ✗ |
| a5 | Excursión Glaciar Perito Moreno | Ushuaia | excursion | 45000 | ✓ |
| a6 | Trekking en Cerro Catedral | Bariloche | excursion | 25000 | ✗ |
| a7 | Tour de Vinos en Mendoza | Mendoza | gastronomic | 35000 | ✓ |
| a8 | Experiencia de Asado Argentino | Buenos Aires | gastronomic | 28000 | ✗ |
| a9 | Rafting en el Río Mendoza | Mendoza | adventure | 30000 | ✓ |
| a10 | Tirolesa en Bariloche | Bariloche | adventure | 22000 | ✗ |
| a11 | Tour Histórico por Córdoba | Córdoba | guided_visit | 5000 | ✗ |
| a12 | Tren a las Nubes en Salta | Salta | excursion | 50000 | ✓ |
| a13 | Navegación por el Canal Beagle | Ushuaia | excursion | 32000 | ✗ |
| a14 | Cabalgata en la Quebrada de las Flechas | Salta | adventure | 20000 | ✗ |
| a15 | Degustación de Empanadas en Salta | Salta | gastronomic | 15000 | ✓ |
| a16 | Free Tour por Nuñez | Buenos Aires | free_tour | 0 | ✓ |

> Nota: `a5` (Excursión Glaciar Perito Moreno) tiene `destination: "Ushuaia"` en los datos pre-cargados aunque su `meetingPoint` real (Terminal de buses de El Calafate) corresponde geográficamente a Santa Cruz — inconsistencia propia de los datos de prueba del backend, no del frontend.

**Actividades con itineraryPoints** (resto retorna `[]`):
- `a12` (Tren a las Nubes en Salta): Salinas Grandes, Viaducto La Polvorilla
- `a13` (Navegación por el Canal Beagle): Isla de los Lobos, Isla de los Pájaros, Faro Les Eclaireurs
- `a14` (Cabalgata en la Quebrada de las Flechas): Angastaco, Quebrada de las Flechas

---

## Validaciones del servidor

| Campo | Regla |
|-------|-------|
| email | `^[^\s@]+@[^\s@]+\.[^\s@]+$` |
| username | `^[a-zA-Z0-9_]{3,20}$` |
| password | string, mínimo 6 caracteres |
| phoneNumber | `^\+?\d{8,15}$` |
| OTP code | `^\d{6}$` |

---

## Consideraciones para el frontend

1. **Guardar ambos tokens**: persistir `token` (access) y `refreshToken` en `SharedPreferences`. El access token expira en **60 minutos**; el refresh token dura **7 días**.

2. **Manejo de 401 con refresh**: cuando cualquier endpoint retorna 401, intentar `POST /api/auth/refresh` con el `refreshToken` guardado. Si el refresh también retorna 401, llevar al usuario al login. No invalidar la sesión local ante el primer 401 — intentar el refresh primero.

   ```java
   // Flujo sugerido en el interceptor OkHttp
   if (response.code() == 401) {
       String newToken = tryRefresh(savedRefreshToken); // POST /api/auth/refresh
       if (newToken != null) {
           // guardar nuevo par y reintentar el request original
       } else {
           // refreshToken expirado → navigate to login
       }
   }
   ```

3. **El token no tiene firma criptográfica**: no lo valides localmente, confía en el servidor. El campo `expiresAt` en el payload es legible (base64url decode) y se puede usar para hacer refresh proactivo antes de que expire.

3. **CORS habilitado**: el backend acepta requests desde cualquier origen.

4. **Sin imágenes propias**: los `imageUrl` y `galleryUrls` apuntan a URLs externas. Manéjalos como URLs remotas normales.

5. **Auto-registro OTP**: si usás el flujo OTP, no necesitás pantalla de registro completa. El usuario se crea automáticamente con username = parte antes del `@` del email.

6. **Reservas** (`GET /api/bookings`) retorna objetos con el campo `activity` embebido (objeto completo). Ya **no** hace falta hacer un segundo GET por cada reserva para mostrar nombre, foto o punto de encuentro.

7. **meetingPoint siempre es objeto**: en cualquier actividad o booking serializado, `meetingPoint` tiene `{ latitude, longitude, address }`. Nunca es string.

8. **Cancelación de reservas**: usar `DELETE /api/bookings/:id` o `POST /api/bookings/:id/cancel` (equivalentes). Si se intenta cancelar fuera de plazo, retorna 409.

9. **Calificaciones**: solo se pueden enviar después de la actividad y dentro de las 48hs. La respuesta es `{ data: { rating: {...} } }` — usar `RatingData` como wrapper. El formulario debe verificar `isWithin48Hours()` en el cliente como resguardo adicional antes de mostrarse.

10. **Status de reservas**: el estado real en `GET /api/bookings` es `confirmed`, `finalized` o `cancelled`. El valor `active` NO se expone al cliente — existe solo internamente en `user.activities`. Filtrar por `"confirmed"` para mostrar reservas vigentes en la UI.

11. **Favoritos**: al agregar, la respuesta incluye `priceChanged` y `spotsChanged` para mostrar alertas de precio/disponibilidad.

12. **Noticias**: `activityId` puede ser `null`. Si tiene valor, se puede navegar a `GET /api/activities/:activityId` desde la noticia.

13. **Recommended** requiere que el usuario tenga preferences configuradas. Si están vacías, puede retornar lista vacía.

14. **No hay endpoints para crear/editar/borrar actividades** desde el frontend.

15. **Sesiones en memoria**: en desarrollo, el server puede reiniciarse y los tokens dejan de funcionar. El manejo de 401 debe intentar refresh primero; si falla, llevar al login.

16. **OTP se imprime en consola del server** (no hay envío de email real). Para desarrollo, revisar los logs del backend.

17. **Foto de perfil — dos opciones:**
    - **URL externa (string):** `PATCH /api/profile/me` con campo `photoUrl` o `profilePhotoUrl`. Sirve si ya tenés la URL de la imagen.
    - **Upload real de archivo:** `POST /api/profile/photo` con `multipart/form-data`, campo `photo` (JPEG/PNG/GIF/WebP, máx 5 MB). La URL retornada es relativa (`/uploads/users/<filename>`); hay que prefijar la base URL del servidor para mostrarla en Android:
      ```java
      String fullUrl = BASE_URL + photoUrl; // ej: "https://...railway.app/uploads/users/foto.jpg"
      ```
    - `PUT /api/users/me` NO acepta foto — solo acepta `username`, `email`, `fullName`, `phoneNumber`.

18. **Offline bundle**: `GET /api/bookings/offline-bundle` retorna reservas con `status === "confirmed"` (las activas no finalizadas ni canceladas). Es correcto — filtra exactamente las reservas vigentes.

19. **Filtrado de actividades pasadas**: las fechas de actividades (`dates`, `schedules[].date`) son ISO 8601 con `Z` (UTC). Comparar siempre como `Instant` UTC contra `Instant.now()`. No usar `LocalDate` para esta comparación — la fecha calendario UTC puede diferir de la local cerca de la medianoche.

---

## Ejemplo de request en Android (OkHttp / Retrofit)

### Header de autenticación

```java
// OkHttp interceptor
request.newBuilder()
    .addHeader("Authorization", "Bearer " + token)
    .addHeader("Content-Type", "application/json")
    .build();
```

### Body de ejemplo (Gson)

```java
JsonObject body = new JsonObject();
body.addProperty("activityId", "a1");
body.addProperty("selectedScheduleId", "a1-s2");

RequestBody requestBody = RequestBody.create(
    body.toString(),
    MediaType.parse("application/json")
);
```
