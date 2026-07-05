# Trabajo Práctico Obligatorio

# Aplicación de Actividades y Experiencias Turísticas

## XploreNow

XploreNow es una plataforma de reserva de actividades y experiencias turísticas que busca desarrollar una aplicación móvil para conectar a los viajeros con los mejores tours, visitas guiadas, free tours, excursiones de día y experiencias locales en diferentes destinos.

La plataforma ya cuenta con un sistema interno que gestiona actividades, destinos, guías, disponibilidad y cupos.

La integración con el sistema interno de XploreNow garantizará que la disponibilidad, los precios y la información de las actividades estén siempre actualizados. Con esta solución, XploreNow busca digitalizar la experiencia del viajero y ofrecer una plataforma moderna orientada al turismo experiencial.

## Objetivo

Construir una **API REST** para acceder a la información mencionada y cumplir con las funcionalidades que tendrá la aplicación móvil.

---

# Funcionalidades

## 1. Autenticación y Registro de Usuarios

- Flujo OTP:
  - Solicitar email.
  - Enviar código de 6 dígitos.
  - Confirmar código y crear sesión.
- Recupero de acceso:
  - Reenvío del OTP cuando no fue recibido o expiró.
- Login clásico:
  - Usuario y contraseña como alternativa al flujo OTP.
- Biometría:
  - Una vez autenticado, el usuario puede habilitar el acceso mediante biometría para futuros ingresos.
  - Si la sesión expira, deberá autenticarse nuevamente con usuario y contraseña.

---

## 2. Perfil del Viajero

1. Ver y editar:
   - Nombre
   - Email
   - Teléfono
   - Foto de perfil (opcional)

2. Configurar preferencias de viaje:
   - Aventura
   - Cultura
   - Gastronomía
   - Naturaleza
   - Relax

   Estas preferencias personalizan las sugerencias mostradas en la Home.

3. Consultar un resumen de:
   - Actividades reservadas.
   - Actividades realizadas.

---

## 3. Catálogo de Actividades (Home)

- Listado paginado con:
  - Imagen
  - Nombre
  - Destino
  - Categoría
  - Duración
  - Precio
  - Cupos disponibles

- Filtros combinados:
  - Destino
  - Categoría
  - Fecha
  - Rango de precio

- Actividades destacadas o recomendadas según las preferencias del usuario.

### Detalle de una actividad

Incluye:

- Descripción completa
- Qué incluye
- Punto de encuentro
- Guía asignado
- Duración
- Idioma
- Política de cancelación
- Galería de fotos

---

## 4. Reservas

4. Reservar una actividad indicando:

- Fecha
- Horario disponible
- Cantidad de participantes

Con validación de cupos en tiempo real.

5. Cancelar una reserva mostrando la política de cancelación correspondiente.

6. Sección **Mis actividades** con estados:

- Confirmada
- Cancelada
- Finalizada

---

## 5. Historial de Actividades

7. Listado de actividades finalizadas mostrando:

- Fecha
- Nombre
- Destino
- Guía
- Duración

8. Filtros por:

- Rango de fechas
- Destino

9. Desde el historial acceder a:

- Detalle de la actividad
- Calificación realizada

---

## 6. Calificación de Actividades y Guías

10. Durante las 48 horas posteriores a la actividad, el viajero podrá calificarla.

11. Calificación mediante estrellas (1–5):

- Actividad
- Guía

12. Comentario opcional (máximo 300 caracteres).

13. La calificación y el comentario permanecen visibles en el historial del usuario.

---

## 7. Mis Favoritos y Lista de Deseos

14. Marcar actividades como favoritas mediante un ícono de corazón.

15. Sección **Mis favoritos** con acceso rápido para reservar.

16. Mostrar un indicador cuando una actividad favorita:

- Cambie de precio.
- Libere nuevos cupos.

17. Los favoritos persisten entre sesiones.

---

## 8. Modo Sin Conexión

### Acceso a Mis Actividades

18. Acceso offline a:

- Vouchers
- Próximas actividades

19. Guardar automáticamente en el dispositivo:

- Datos de la actividad
- Punto de encuentro
- Voucher

20. Al recuperar conexión:

- Sincronizar cancelaciones.
- Sincronizar reprogramaciones.

21. Mostrar un indicador visual cuando la aplicación funciona sin conexión.

---

## 9. Noticias, Ofertas y Destinos Destacados

22. Sección en Home (o pestaña independiente) para mostrar:

- Noticias
- Descuentos
- Nuevos destinos
- Promociones

23. Las noticias provienen de una API externa e incluyen:

- Imagen
- Título
- Descripción breve

24. Al seleccionar una noticia:

- Mostrar el detalle completo.
- O redirigir a la actividad correspondiente.

---

## 10. Mapa y Punto de Encuentro

25. Botón **Cómo llegar** que abre Google Maps (o la aplicación de mapas predeterminada) con la navegación hacia el punto de encuentro.

---

## 11. Voucher Digital y Check-in por QR

26. El voucher digital contiene:

- Nombre de la actividad
- Fecha
- Horario
- Punto de encuentro
- Nombre del guía
- Cantidad de participantes

27. Al llegar al punto de encuentro, el viajero escanea el código QR mostrado por el guía para confirmar su asistencia.

28. Resultado del escaneo:

- ✅ Asistencia confirmada (verde)
- ❌ QR inválido o error (rojo) con mensaje descriptivo

---

## 12. Recordatorios y Avisos

29. Notificación push 24 horas antes de la actividad con acceso directo al voucher.

30. Aviso inmediato cuando una actividad sea:

- Reprogramada
- Cancelada

---

# Entregables para Evaluación

## Primera entrega

Se entregarán las funcionalidades asignadas tanto del:

- Backend
- Aplicación Android Nativa (Java)

100% funcionales.

---

## Segunda entrega

Se entregarán las funcionalidades asignadas tanto del:

- Backend
- Aplicación React Native (JavaScript)

100% funcionales.

---

## Tercera entrega

Entrega de la aplicación completamente funcional con el **100% de los casos de uso**, desarrollada en una de las siguientes tecnologías asignadas al grupo:

- React Native con Expo (Android)
- Android Nativo (Java)