# Integración con Google Calendar (notificaciones de turnos)

`ms-reservas` crea un evento en el Google Calendar del centro médico cada vez que un paciente
reserva un turno y agrega al paciente como **invitado** con `sendUpdates=all`. Con eso Google:

- le envía al paciente un **email de invitación** con fecha, hora y botón para agregarlo a su calendario;
- muestra el **evento en su Google Calendar** (si su email es de Gmail/Workspace se agrega solo);
- le manda recordatorios (email 24 hs antes y popup 1 h antes).

| Evento del turno | Acción en Google Calendar | Email al paciente |
|---|---|---|
| Reserva (`RESERVA`) | Crea el evento `mediturnos<turnoId>` | Invitación |
| Pago confirmado (`CONFIRMACION`) | Actualiza el evento | Actualización |
| Reprogramación exitosa (`REPROGRAMACION`) | Mueve el evento al nuevo horario | Actualización |
| Bloqueo médico (`PENDIENTE_REPROGRAMACION`) | Sin cambios (todavía no hay nuevo horario) | — (sale por Telegram) |
| Cancelación (`CANCELACION`) | Cancela el evento | Aviso de cancelación |

Cada intento queda auditado en `schema_reservas.notificaciones` con `canal = GOOGLE_CALENDAR`
(`ENVIADO` / `ERROR`). Como el resto de las notificaciones (**RN-08**), un fallo de Google
**no revierte** el turno.

## Modo simulado (por defecto)

Con `GOOGLE_CALENDAR_ENABLED=false` no se llama a Google: se registra `ENVIADO [SIMULADO]` en la
auditoría. Si el email del paciente contiene `error` o `fallo` se simula una caída y se registra
`ERROR`, útil para probar RN-08 sin credenciales.

## Activar la integración real

1. **Proyecto en Google Cloud**: en <https://console.cloud.google.com/> crear un proyecto y habilitar
   **Google Calendar API** (APIs y servicios → Biblioteca).
2. **Pantalla de consentimiento OAuth** — ver [detalle del paso 2](#detalle-del-paso-2-pantalla-de-consentimiento-oauth):
   tipo *Externo*, completar los datos mínimos y agregar como *usuario de prueba* la cuenta de Google
   del centro médico (la dueña del calendario).
3. **Credenciales** → *Crear credenciales* → *ID de cliente de OAuth* → tipo **Aplicación web**,
   con URI de redirección autorizado `https://developers.google.com/oauthplayground`.
   Guardar el *Client ID* y el *Client Secret*.
4. **Obtener el refresh token** en <https://developers.google.com/oauthplayground>:
   - ⚙️ (arriba a la derecha) → marcar *Use your own OAuth credentials* y pegar Client ID/Secret.
   - En *Step 1* ingresar el scope `https://www.googleapis.com/auth/calendar.events` → *Authorize APIs*
     e iniciar sesión con la cuenta del centro médico.
   - En *Step 2* → *Exchange authorization code for tokens* → copiar el **Refresh token**.
5. **Configurar `.env`** (no se versiona):

   ```dotenv
   GOOGLE_CALENDAR_ENABLED=true
   GOOGLE_CALENDAR_CLIENT_ID=xxxxxxxx.apps.googleusercontent.com
   GOOGLE_CALENDAR_CLIENT_SECRET=GOCSPX-xxxxxxxx
   GOOGLE_CALENDAR_REFRESH_TOKEN=1//0xxxxxxxx
   GOOGLE_CALENDAR_ID=primary
   GOOGLE_CALENDAR_TIME_ZONE=America/Argentina/Cordoba
   ```

6. `docker compose up -d --build ms-reservas` y reservar un turno con un paciente cuyo email sea real.
   Verificar en `GET /api/reservas/notificaciones` (rol admin) el registro `GOOGLE_CALENDAR` → `ENVIADO`.

## Pruebas con Postman

La colección [`docs/postman/MediTurnos_Google_Calendar.postman_collection.json`](postman/MediTurnos_Google_Calendar.postman_collection.json)
(con el entorno `MediTurnos_Local_Docker`) recorre reserva → pago → bloqueo médico → reprogramación → cancelación
con `paciente.perez` y verifica la auditoría `GOOGLE_CALENDAR` de cada paso, y que Telegram siga notificando.
Sirve en modo simulado y en modo real; en modo real, al terminar, el paciente debería tener en su bandeja la
invitación, la actualización por la reprogramación y el aviso de cancelación.

```bash
npx newman run docs/postman/MediTurnos_Google_Calendar.postman_collection.json \
    -e docs/postman/MediTurnos_Local_Docker.postman_environment.json
```

> ⚠️ Mientras la app OAuth esté en estado *Testing*, Google invalida el refresh token a los **7 días**.
> Para la demo alcanza con regenerarlo (paso 4); para uso continuo hay que publicar la app.

## Cuentas para la demo (proyecto académico)

Los usuarios y la clínica son ficticios, así que se usan cuentas de Gmail de los integrantes:

- **Cuenta "clínica"** (dueña del calendario y organizadora de los eventos): el Gmail de un integrante.
  Con esa cuenta se hacen los pasos 1 a 4. No se precarga en ningún archivo: queda definida por el refresh token.
- **Cuenta "paciente"** (recibe el mail y el evento): el email del paciente semilla en
  `docker/postgres/init-db.sql` (tabla `schema_reservas.pacientes`). Hoy el paciente 1 (Juan Pérez →
  usuario Keycloak `paciente.perez`) tiene el mail de un integrante; para probar, reservar logueado con ese usuario.

Reglas:

- **No usar la misma cuenta para clínica y paciente**: Google no le envía la invitación al organizador.
- Para que un solo integrante reciba los mails de varios pacientes se pueden usar alias de Gmail
  (`nombre+perez@gmail.com`, `nombre+gomez@gmail.com`): llegan a la misma bandeja y son emails distintos para la base.
- El seed solo corre con la base vacía: después de cambiarlo, `docker compose down -v` y `docker compose up -d --build`.
- El repositorio es público: los emails cargados en el seed quedan visibles en GitHub.

## Detalle del paso 2: pantalla de consentimiento OAuth

**Para qué sirve.** El backend va a crear eventos en el calendario de una cuenta de Google, así que Google
exige registrar "una aplicación" y que el dueño de la cuenta la autorice. La *pantalla de consentimiento*
es la ventana "MediTurnos quiere acceder a tu Google Calendar → Permitir" que aparece en el paso 4; acá
solo se configura (nombre de la app y quién puede autorizarla). Como la app no está verificada por Google,
queda en modo **Testing**, y en ese modo **solo pueden autorizarla las cuentas cargadas como usuarios de prueba**.

**Paso a paso** (logueado con la cuenta de la clínica y con el proyecto del paso 1 seleccionado arriba;
Google cambió esta consola recientemente, los nombres pueden variar un poco):

1. Menú ☰ → **APIs y servicios** → **Pantalla de consentimiento de OAuth**. Lleva a **Google Auth Platform** → **Comenzar** (*Get started*).
2. **Información de la app**: nombre `MediTurnos`, correo de asistencia = el de la cuenta de la clínica → **Siguiente**.
3. **Público** (*Audience*): **Externo** → **Siguiente**. (*Interno* es solo para organizaciones con Google Workspace.)
4. **Información de contacto**: el mismo mail → **Siguiente**.
5. Aceptar la política de datos de usuario → **Crear**.
6. Menú izquierdo → **Público** (*Audience*) → **Usuarios de prueba** → **+ Add users** → agregar el Gmail de la clínica → **Guardar**.
   Dejar el estado de publicación en **Testing** (no tocar "Publicar app").

Los *scopes* no hace falta cargarlos acá: se piden en el paso 4 desde OAuth Playground.

## Errores frecuentes

| Qué aparece | Causa | Solución |
|---|---|---|
| **"Error 403: access_denied"** al autorizar en OAuth Playground | La cuenta con la que iniciás sesión no está en *Usuarios de prueba* | Agregarla (paso 2, punto 6) |
| **"Google no verificó esta app"** | Normal en modo Testing | **Configuración avanzada → Ir a MediTurnos (no seguro)** → **Continuar / Permitir** |
| **"Error 400: redirect_uri_mismatch"** | Falta el URI de redirección en el cliente OAuth | En el cliente (paso 3) agregar `https://developers.google.com/oauthplayground` |
| Auditoría `GOOGLE_CALENDAR` en `ERROR` con `invalid_grant` | Refresh token vencido (7 días en Testing) o revocado | Repetir el paso 4 y actualizar `GOOGLE_CALENDAR_REFRESH_TOKEN` |
| El evento se crea pero no llega el mail | Paciente = misma cuenta que la clínica | Usar otra cuenta para el paciente |

## Detalles técnicos

- Cliente: [`GoogleCalendarClient`](../ms-reservas/src/main/java/utn/frc/backend/mediturnos/reservas/client/GoogleCalendarClient.java),
  `RestClient` puro contra `oauth2.googleapis.com/token` y `www.googleapis.com/calendar/v3` (sin SDK de Google).
- El access token se obtiene con `grant_type=refresh_token` y se cachea hasta 60 s antes de vencer.
- El ID del evento es determinístico (`<GOOGLE_CALENDAR_EVENT_ID_PREFIX><turnoId>`, base32hex), así se
  puede actualizar/cancelar sin guardar el ID de Google en la base. La creación es un *upsert*:
  `PUT` sobre el ID y, si devuelve 404, `POST`.
- Si se reinicia la base y se reutilizan IDs de turno, cambiar `GOOGLE_CALENDAR_EVENT_ID_PREFIX`
  (solo letras `a-v` y dígitos) para no pisar eventos viejos.
