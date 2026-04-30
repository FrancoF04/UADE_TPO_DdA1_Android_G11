# Backend Reference — UADE TPO DdA1

> **Para Claude en una nueva sesión:** Este archivo es el contexto completo del proyecto. Leelo antes de responder cualquier pregunta.
> El usuario está desarrollando el **frontend Android en Java** que consume este backend.
> El backend YA ESTÁ HECHO y deployado. El trabajo actual es el frontend Android.
> Si el usuario trae modificaciones del backend, reflejarlo en este archivo.

> Backend Node.js + Express, sin base de datos real (datos en memoria).
> URL de producción: `https://uadetpodda1backend-production.up.railway.app`
> Repo backend: `UADE_TPO_DdA1_Backend` (este repo)
> Repo frontend Android: separado (Java)

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
- `itineraryPoints` = array de paradas (solo algunas actividades tienen datos, las demás retornan `[]`)
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
  "image": "https://...",
  "title": "Promo especial en Buenos Aires",
  "description": "Descuentos en actividades...",
  "content": "Texto largo...",
  "activityId": "a1",
  "createdAt": "2026-04-01T10:00:00Z"
}
```

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
| `booking.status` | `confirmed`, `finalized`, `cancelled` |

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
{ "userId": "u1", "fullName": "Juan Perez", "jti": "<uuid>" }
```

- Se envía en el header: `Authorization: Bearer <token>`
- Dura **24 horas** desde que se crea la sesión
- Las sesiones viven en memoria: si el servidor se reinicia, el token deja de funcionar

### Flujo OTP (recomendado para onboarding)

```
1. POST /api/auth/otp/request   { "email": "..." }
   → Backend genera código de 6 dígitos (se imprime en consola del server)

2. POST /api/auth/otp/verify    { "email": "...", "code": "123456" }
   → Si el usuario no existe, se crea automáticamente
     username = email.split('@')[0]
   → Retorna token

3. (Opcional) POST /api/auth/otp/resend  { "email": "..." }
   → Invalida OTPs anteriores y genera uno nuevo
```

### Flujo Login Clásico

```
POST /api/auth/login
Body: { "username": "juanperez", "password": "password123" }
→ Retorna token
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
→ Retorna token (201)
```

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
| POST | `/otp/verify` | No | `{ email, code }` | `{ token }` |
| POST | `/otp/resend` | No | `{ email }` | `{ message }` |
| POST | `/register` | No | `{ email, username, password, fullName, phoneNumber }` | `{ token }` 201 |
| POST | `/login` | No | `{ username, password }` | `{ token }` |
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

**Notas:**
- `PUT /me` solo actualiza los campos presentes en el body
- `PUT /preferences` reemplaza el objeto preferences completo

---

### Perfil — `/api/profile` (alias: `/profile`)

Ruta alternativa y más flexible para operaciones de perfil. Acepta nombres de campo alternativos.

| Método | Path | Auth | Body | Respuesta |
|--------|------|------|------|-----------|
| GET | `/` o `/me` | **Sí** | — | `{ user: { id, name, fullName, email, phone, phoneNumber, profilePhotoUrl, photoUrl, preferences } }` |
| PATCH | `/` o `/me` | **Sí** | `{ name?, fullName?, phone?, phoneNumber?, photoUrl?, profilePhotoUrl? }` | `{ user }` |
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
    "byStatus": { "active": 2, "cancelled": 1 }
  }
}
```

> **Nota:** `/api/profile` y `/api/users` coexisten. `/api/profile` acepta aliases (`name` → `fullName`, `phone` → `phoneNumber`, `photoUrl` → `profilePhotoUrl`) y usa PATCH en lugar de PUT.

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
      "status": "active",
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

**`POST /api/ratings` — body:**
```json
{
  "bookingId": "b-...",
  "activityRating": 5,
  "guideRating": 4,
  "comment": "Excelente experiencia"
}
```

**Reglas de calificación:**
- `activityRating` y `guideRating`: enteros entre 1 y 5 (obligatorios)
- `comment`: string, máximo 300 caracteres (opcional)
- Solo se puede calificar **después** de que la actividad haya ocurrido (o status `finalized`)
- La ventana de calificación es de **48 horas** desde la fecha de la actividad
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
  "image": "https://...",
  "title": "Promo especial en Buenos Aires",
  "description": "Descuentos en actividades...",
  "activityId": "a1",
  "createdAt": "2026-04-01T10:00:00Z"
}
```

**`GET /api/news/:id` agrega `content`** (texto largo completo).

**Noticias pre-cargadas:**
| ID | Título | activityId |
|----|--------|------------|
| n1 | Promo especial en Buenos Aires | a1 |
| n2 | Ofertas en experiencias de vino | a7 |
| n3 | Destino destacado: Salta | null |

---

## Datos pre-cargados

### Usuarios de prueba

| Email | Username | Password |
|-------|----------|----------|
| juan@example.com | juanperez | password123 |
| maria@example.com | mariagarcia | password456 |

### Actividades (15 pre-cargadas)

| ID | Nombre | Destino | Categoría | Precio | Featured |
|----|--------|---------|-----------|--------|----------|
| a1 | Walking Tour por San Telmo | Buenos Aires | free_tour | 0 | ✓ |
| a2 | Free Tour por La Boca | Buenos Aires | free_tour | 0 | ✗ |
| a3 | Visita Guiada al Colón | Buenos Aires | guided_visit | 3500 | ✓ |
| a4 | Excursión Cataratas del Iguazú | Bariloche | excursion | 45000 | ✓ |
| a5 | Degustación de Vinos | Mendoza | gastronomic | 8000 | ✓ |
| a6 | Trekking Glaciar Perito Moreno | Bariloche | adventure | 35000 | ✗ |
| a7 | Tour Fin del Mundo | Ushuaia | guided_visit | 12000 | ✗ |
| a8 | Visita Bodega Premium | Mendoza | gastronomic | 15000 | ✗ |
| a9 | City Tour Córdoba | Córdoba | guided_visit | 5000 | ✗ |
| a10 | Tour de Salinas Grandes | Salta | excursion | 22000 | ✓ |
| a11 | Kayak en el Nahuel Huapi | Bariloche | adventure | 18000 | ✗ |
| a12 | Free Tour Barrio de Palermo | Buenos Aires | free_tour | 0 | ✗ |
| a13 | Tren de las Nubes | Salta | excursion | 28000 | ✓ |
| a14 | Paragliding en las Sierras | Córdoba | adventure | 25000 | ✗ |
| a15 | Asado y Folclore en Salta | Salta | gastronomic | 9500 | ✗ |

**Actividades con itineraryPoints** (resto retorna `[]`):
- `a13` (Tren de las Nubes): Isla de los Lobos, Isla de los Pájaros, Faro Les Eclaireurs
- `a14` (Paragliding): Angastaco, Quebrada de las Flechas
- `a12` (Free Tour Palermo): Salinas Grandes, Viaducto La Polvorilla

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

1. **Guardar el token**: persistirlo en `SharedPreferences` o equivalente. Se necesita en casi todas las pantallas post-login.

2. **El token no tiene firma criptográfica**: no lo valides localmente, confía en el servidor. Si el servidor retorna 401, el token expiró o el server se reinició.

3. **CORS habilitado**: el backend acepta requests desde cualquier origen.

4. **Sin imágenes propias**: los `imageUrl` y `galleryUrls` apuntan a URLs externas. Manéjalos como URLs remotas normales.

5. **Auto-registro OTP**: si usás el flujo OTP, no necesitás pantalla de registro completa. El usuario se crea automáticamente con username = parte antes del `@` del email.

6. **Reservas** (`GET /api/bookings`) retorna objetos con el campo `activity` embebido (objeto completo). Ya **no** hace falta hacer un segundo GET por cada reserva para mostrar nombre, foto o punto de encuentro.

7. **meetingPoint siempre es objeto**: en cualquier actividad o booking serializado, `meetingPoint` tiene `{ latitude, longitude, address }`. Nunca es string.

8. **Cancelación de reservas**: usar `DELETE /api/bookings/:id` o `POST /api/bookings/:id/cancel` (equivalentes). Si se intenta cancelar fuera de plazo, retorna 409.

9. **Calificaciones**: solo se pueden enviar después de la actividad y dentro de las 48hs. Verificar `booking.status === "finalized"` y `booking.selectedDate` antes de mostrar el formulario.

10b. **Status de reservas**: el estado real que retorna el servidor es `confirmed` (activa), `finalized` (pasada) y `cancelled`. El valor `active` NO existe en los bookings del array `/api/bookings` — existe internamente solo en `user.activities` pero nunca se expone al cliente.

10. **Favoritos**: al agregar, la respuesta incluye `priceChanged` y `spotsChanged` para mostrar alertas de precio/disponibilidad.

11. **Noticias**: `activityId` puede ser `null`. Si tiene valor, se puede navegar a `GET /api/activities/:activityId` desde la noticia.

12. **Recommended** requiere que el usuario tenga preferences configuradas. Si están vacías, puede retornar lista vacía.

13. **No hay endpoints para crear/editar/borrar actividades** desde el frontend.

14. **Sesiones en memoria**: en desarrollo, el server puede reiniciarse y los tokens dejan de funcionar. Implementar manejo de 401 que lleve al login.

15. **OTP se imprime en consola del server** (no hay envío de email real). Para desarrollo, revisar los logs del backend.

16. **Foto de perfil**: `PUT /api/users/me` NO acepta `profilePhotoUrl`. Para actualizar la foto usar `PATCH /api/profile/me` con el campo `photoUrl` o `profilePhotoUrl`. No hay endpoint de upload — se guarda como URL string externa.

17. **Offline bundle**: `GET /api/bookings/offline-bundle` retorna reservas con `status === "confirmed"` (las activas no finalizadas ni canceladas). Es correcto — filtra exactamente las reservas vigentes.

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
