# Integración con Telegram Bot API (notificaciones de turnos)

`ms-reservas` consume la API pública de **Telegram Bot API** (`POST https://api.telegram.org/bot<TOKEN>/sendMessage`) vía `RestClient` ([`NotificacionService`](../ms-reservas/src/main/java/utn/frc/backend/mediturnos/reservas/service/NotificacionService.java) y [`RestClientConfig`](../ms-reservas/src/main/java/utn/frc/backend/mediturnos/reservas/config/RestClientConfig.java)) para enviar notificaciones transaccionales en tiempo real al paciente.

| Evento del turno | Tipo de alerta (`tipoAlerta`) | Mensaje enviado |
|---|---|---|
| Reserva (`RESERVA`) | `RESERVA` | Confirmación de reserva inicial y monto a abonar |
| Pago confirmado (`CONFIRMACION`) | `CONFIRMACION` | Confirmación de pago y estado `CONFIRMADO` |
| Bloqueo médico (`FUNC-10`) | `REPROGRAMACION` | Aviso de pase a `PENDIENTE_REPROGRAMACION` con prioridad |
| Reprogramación exitosa (`FUNC-11`) | `REPROGRAMACION` | Confirmación de nueva fecha, horario y estado |
| Cancelación (`FUNC-09`) | `CANCELACION` | Aviso de cancelación (con o sin penalidad) |

Cada intento queda auditado en `schema_reservas.notificaciones` con `canal = TELEGRAM` (`ENVIADO` / `ERROR`). Al igual que Google Calendar (**RN-08**), cualquier fallo o timeout (`connect-timeout-ms: 3000`, `read-timeout-ms: 4000`) se degrada controladamente sin revertir la transacción del turno.

## Modo simulado (por defecto)

Con `NOTIFICACIONES_BOT_TOKEN=dummy_token_desarrollo` (valor por defecto en `application.yml`, `docker-compose.yml` y `.env.example`), no se realiza la llamada HTTP a Telegram: se registra `ENVIADO` en la auditoría y se loguea `[RN-08 SIMULADO]`.

Si el destinatario (`telefono_whatsapp` o `email`) contiene `error` o `fallo`, se simula una caída de red y se audita `ERROR`, útil para probar **RN-08** sin credenciales.

## Activar la integración real

1. **Crear el bot en Telegram (`@BotFather`):**
   - Abrí Telegram, buscá el chat oficial **`@BotFather`** y enviá `/newbot`.
   - Ingresá un nombre visible (ej. `MediTurnos Notificaciones`) y un username terminado en `bot` (ej. `mediturnos_g06_bot`).
   - Copiá el **HTTP API Token** devuelto (formato `<BOT_ID>:<CLAVE_BOTFATHER>`).
2. **Iniciar el chat con tu bot y obtener tu `chat_id`:**
   - Abrí el enlace `t.me/<tu_bot>` y presioná **Iniciar** (`/start`) o enviá un mensaje (`hola`). *(Telegram exige que el usuario inicie el chat antes de que el bot pueda escribirle).*
   - Consultá `https://api.telegram.org/bot<TU_TOKEN>/getUpdates` en el navegador y copiá el número entero dentro de `"chat": { "id": ... }` (también podés consultarlo con `@userinfobot`, recordando haber iniciado previamente el chat con tu bot).
3. **Configurar `.env`** (no se versiona):

   ```dotenv
   NOTIFICACIONES_API_URL=https://api.telegram.org
   NOTIFICACIONES_BOT_TOKEN=<TU_BOT_TOKEN>
   ```

4. **Asociar el `chat_id` al paciente semilla en PostgreSQL** (sin modificar `docker/postgres/init-db.sql`):

   ```bash
   docker exec mediturnos-postgres psql -U mediturnos_user -d mediturnos_db \
     -c "UPDATE schema_reservas.pacientes SET telefono_whatsapp = 'TU_CHAT_ID' WHERE id = 1;"
   ```

   Verificá el cambio:

   ```bash
   docker exec mediturnos-postgres psql -U mediturnos_user -d mediturnos_db \
     -c "SELECT id, nombre, email, telefono_whatsapp FROM schema_reservas.pacientes WHERE id = 1;"
   ```

5. **Reiniciar `ms-reservas` para aplicar `.env`:**

   ```bash
   docker compose up -d ms-reservas
   ```

   Esperá unos segundos y verificá que `http://localhost:8082/actuator/health` responda `{"status":"UP"}`.

6. **Probar en vivo:**
   Reservá un turno (con Postman o Newman usando cualquiera de las colecciones en [`docs/postman/`](postman/README.md)) logueado como `paciente.perez`.
   - Deberías recibir el mensaje real en tu chat de Telegram al instante.
   - En `GET /api/reservas/notificaciones?pacienteId=1` (rol `ROL_ADMIN`), la fila `TELEGRAM` figurará con `estadoEnvio: ENVIADO` y en los logs de `ms-reservas` ya no aparecerá `[RN-08 SIMULADO]`.

## Volver al modo simulado

En `.env` cambiá nuevamente:

```dotenv
NOTIFICACIONES_BOT_TOKEN=dummy_token_desarrollo
```

y corré `docker compose up -d ms-reservas`.

## Errores frecuentes

| Qué aparece | Causa | Solución |
|---|---|---|
| `getUpdates` devuelve `{"ok":true,"result":[]}` | Todavía no se envió ningún mensaje al bot | Abrí `t.me/<tu_bot>`, enviá `/start` y recargá `getUpdates` |
| Auditoría `TELEGRAM` en `ERROR` con `400 Bad Request: chat not found` | El `telefono_whatsapp` tiene formato telefónico (`+549...`) en lugar de un `chat_id` numérico, o el usuario nunca inició el chat con el bot | Ejecutá el `UPDATE` con tu `chat_id` numérico y asegurate de haber enviado `/start` al bot |
| Auditoría `TELEGRAM` en `ERROR` con `401 Unauthorized` | Token mal copiado o con espacios/comillas en `.env` | Revisá `NOTIFICACIONES_BOT_TOKEN` en `.env` y recreá `ms-reservas` |
