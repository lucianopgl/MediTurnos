# 📮 Guía de Pruebas Integrales con Postman y Newman — MediTurnos API

Esta carpeta contiene la **Suite de Pruebas de Integración y Casos de Verificación E2E** para el sistema **MediTurnos API** (Grupo 06 — Comisión 3K4 — UTN FRC), diseñadas para ser ejecutadas directamente a través del **API Gateway** (`http://localhost:8080`) contra el entorno de contenedores Docker.

---

## 📁 Archivos Disponibles

1. **`MediTurnos_API.postman_collection.json`** *(Suite E2E de Reglas de Negocio)*:
   - Colección v2.1 de Postman con **25 peticiones organizadas en 5 fases lógicas**.
   - Valida de extremo a extremo los criterios de verificación de la cátedra (**CV-01 a CV-08**) y las reglas de negocio (**RN-01 a RN-08**), incluyendo rechazos esperados (`401`, `403`, `409`, `422`).
   - Contiene scripts de pre-solicitud (`Pre-request Scripts`) y validaciones automáticas (`Tests`) con aserciones `pm.test` en JavaScript.

2. **`MediTurnos_Endpoints_Referencia.postman_collection.json`** *(Catálogo Completo de Endpoints 31/31)*:
   - Colección v2.1 de Postman con **37 peticiones** (5 tokens OIDC + Actuator Health del Gateway + los **31 endpoints REST completos**: 19 de `ms-catalogo-recursos` y 12 de `ms-reservas`) organizadas por microservicio y controlador.
   - Pensada tanto para demostrar cada endpoint individualmente frente al tribunal como para correrla completa en *Collection Runner* o Newman sin alterar los turnos semilla.

3. **`MediTurnos_Google_Calendar.postman_collection.json`** *(Integración Google Calendar)*:
   - Colección v2.1 de Postman con **13 peticiones en 6 carpetas**: recorre el ciclo de vida de un turno (reserva → pago → bloqueo médico → reprogramación → cancelación) y verifica en la auditoría de notificaciones la fila del canal `GOOGLE_CALENDAR` de cada paso, y que Telegram siga notificando en paralelo.
   - Funciona en modo simulado (`GOOGLE_CALENDAR_ENABLED=false`, por defecto) y en modo real (el mail y el evento le llegan al paciente 1, `paciente.perez`). Ver [`docs/google-calendar.md`](../google-calendar.md).
   - Usa fechas y franjas aleatorias, así que se puede correr varias veces seguidas.

4. **`MediTurnos_Local_Docker.postman_environment.json`** *(Entorno Compartido)*:
   - Entorno con las variables base (`gateway_url: http://localhost:8080`, `keycloak_url: http://localhost:8083`, realm y client).
   - Variables dinámicas de sesión para almacenar tokens (`admin_token`, `profesional_token`, `dra_martinez_token`, `paciente_perez_token`, `paciente_gomez_token`) y correlaciones.

---

## 🚀 Cómo Importar y Ejecutar en la Interfaz de Postman

### Paso 1: Importar los archivos
1. Abre **Postman**.
2. Haz clic en el botón **Import** (esquina superior izquierda).
3. Arrastra o selecciona los archivos de `docs/postman/`:
   - `MediTurnos_API.postman_collection.json`
   - `MediTurnos_Endpoints_Referencia.postman_collection.json`
   - `MediTurnos_Google_Calendar.postman_collection.json`
   - `MediTurnos_Local_Docker.postman_environment.json`

### Paso 2: Seleccionar el Entorno
1. En la esquina superior derecha de Postman, abre el selector de entornos (dropdown que dice *No Environment*).
2. Selecciona **MediTurnos — Local Docker Environment**.

### Paso 3: Ejecución Automatizada (Collection Runner)
1. Haz clic derecho sobre cualquiera de las dos colecciones importadas (`🏥 MediTurnos API — Suite E2E...` o `📘 MediTurnos API — Referencia Completa de Endpoints...`).
2. Selecciona **Run collection**.
3. Asegúrate de que todas las carpetas y peticiones estén tildadas y en orden secuencial.
4. Haz clic en **Run** y verás el **100% de tests aprobados (verde)**.

---

## ⚡ Ejecución desde Terminal con Newman (CLI)

Si tienes Node.js instalado, puedes ejecutar cualquiera de las dos colecciones directamente desde la línea de comandos sin abrir Postman:

```bash
# 1. Suite E2E de Reglas de Negocio (CV-01 a CV-08)
npx newman run docs/postman/MediTurnos_API.postman_collection.json \
    -e docs/postman/MediTurnos_Local_Docker.postman_environment.json \
    --color on

# 2. Catálogo Completo de Referencia (31/31 Endpoints REST)
npx newman run docs/postman/MediTurnos_Endpoints_Referencia.postman_collection.json \
    -e docs/postman/MediTurnos_Local_Docker.postman_environment.json \
    --color on

# 3. Integración Google Calendar (evento + invitación por email)
npx newman run docs/postman/MediTurnos_Google_Calendar.postman_collection.json \
    -e docs/postman/MediTurnos_Local_Docker.postman_environment.json \
    --color on
```

---

## 🧩 Estructura y Mapeo de Casos de Verificación de la Cátedra

| Carpeta | Caso / Regla | Descripción de la Prueba | Código HTTP Esperado |
| :--- | :--- | :--- | :--- |
| **`00. Autenticación Keycloak`** | `SEG-01` | Adquisición de tokens JWT para `admin`, `dr.gonzalez`, `dra.martinez`, `paciente.perez` y `paciente.gomez`. | `200 OK` |
| **`01. Perímetro Gateway`** | `INF-01` | **Paso libre a salud**: `GET /actuator/health` sin token. | `200 OK` |
| | `CV-08` / `SEG-01` | **Rechazo 401**: Petición sin token devuelve `ErrorResponseDTO` con cabecera `X-Correlation-Id`. | `401 Unauthorized` |
| | `CV-08` / `SEG-01` | **Rechazo 403**: Paciente intentando crear una sede (`ROL_ADMIN`). | `403 Forbidden` |
| | `CV-08` / `SEG-01` | **Rechazo 403**: Paciente intentando auditar notificaciones. | `403 Forbidden` |
| **`02. Catálogo de Recursos`** | `FUNC-01` a `05` | Consulta de sedes activas, consultorios, equipamientos, servicios médicos y profesionales habilitados. | `200 OK` |
| | `RN-01`, `RN-02` | Validación estática de compatibilidad de recursos físicos y humanos. | `200 OK` |
| **`03. Ciclo de Vida Turnos`** | `CV-01` / `RN-03` | **Reserva exitosa tripartita**: Paciente Pérez reserva con Dr. González en Box 101. Notificación externa simulada. | `201 Created` |
| | `CV-02` / `RN-03` | **Colisión de Consultorio N:1**: Dra. Martínez intenta reservar el mismo consultorio Box 101 en la misma franja. | `409 Conflict` |
| | `CV-03` / `RN-02` | **Recurso No Disponible**: Intento de reservar servicio con equipamiento inoperativo (Holter). | `409 Conflict` / `422 Unprocessable` |
| | `CV-04` / `RN-04` | **Confirmación por Cobro**: Registro de pago exacto transiciona turno a `CONFIRMADO` (`pagado = true`). | `200 OK` |
| | `CV-06` / `RN-06` | **Bloqueo de Agenda**: Profesional bloquea franja por congreso (`POST /api/reservas/turnos/cancelacion-bloque-profesional`); turnos afectados pasan a `PENDIENTE_REPROGRAMACION`. | `200 OK` |
| | `CV-07` / `RN-07` | **Reprogramación Prioritaria Sin Doble Cobro**: Paciente reprograma turno afectado; pasa directo a `CONFIRMADO` sin nuevo cobro. | `200 OK` |
| | `CV-05` / `RN-05` | **Cancelación y Liberación**: Paciente cancela turno; se liberan inmediatamente los 4 recursos. | `200 OK` |
| | `FUNC-12` | **Cierre de Atención Médica**: Dr. González registra el cierre de atención (`PATCH /api/reservas/turnos/{id}/cierre-atencion`) y pasa turno a `ATENDIDO`. | `200 OK` |
| **`04. Auditoría y Trazabilidad`**| `RN-08` / `INT-01` | Auditoría de notificaciones persistidas en PostgreSQL con estado de entrega (`ENVIADO`/`ERROR`). | `200 OK` |
| | `TEC-01` | Verificación de trazabilidad distribuida e inyección de `X-Correlation-Id`. | `200 OK` |

---

## 🛡️ Principios Cumplidos en los Contratos (`API-01`)

- **Prohibición de Verbos en las URLs:** Todas las rutas usan sustantivos que representan recursos (`/api/catalogo/sedes`, `/api/catalogo/consultorios`, `/api/catalogo/compatibilidad`, `/api/reservas/turnos/{id}/pagos`, `/api/reservas/turnos/cancelacion-bloque-profesional`, `/api/reservas/turnos/{id}/reprogramacion`, `/api/reservas/turnos/{id}/cancelacion`, `/api/reservas/turnos/{id}/cierre-atencion`). La semántica de la operación se define exclusivamente mediante el verbo HTTP (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).
- **Formato Unificado de Errores:** Todos los rechazos de seguridad del Gateway y de negocio de los microservicios devuelven la estructura `ErrorResponseDTO` (`timestamp`, `status`, `error`, `ruleCode`, `message`, `path`, `correlationId`, `validationErrors`).
