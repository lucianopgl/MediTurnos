# 📮 Guía de Pruebas Integrales con Postman y Newman — MediTurnos API

Esta carpeta contiene la **Suite de Pruebas de Integración y Casos de Verificación E2E** para el sistema **MediTurnos API** (Grupo 06 — Comisión 3K4 — UTN FRC), diseñadas para ser ejecutadas directamente a través del **API Gateway** (`http://localhost:8080`) contra el entorno de contenedores Docker.

---

## 📁 Archivos Disponibles

1. **`MediTurnos_API.postman_collection.json`**:
   - Colección v2.1 de Postman con **20 peticiones organizadas en 5 fases lógicas**.
   - Contiene scripts de pre-solicitud (`Pre-request Scripts`) y validaciones automáticas (`Tests`) con aserciones `pm.test` en JavaScript.
   - Gestiona automáticamente la captura dinámica de tokens Bearer de Keycloak y los IDs de los turnos generados.

2. **`MediTurnos_Local_Docker.postman_environment.json`**:
   - Entorno con las variables base (`gateway_url: http://localhost:8080`, `keycloak_url: http://localhost:8083`, realm y client).
   - Variables dinámicas de sesión para almacenar tokens (`admin_token`, `profesional_token`, `dra_martinez_token`, `paciente_perez_token`, `paciente_gomez_token`) y correlaciones.

---

## 🚀 Cómo Importar y Ejecutar en la Interfaz de Postman

### Paso 1: Importar los archivos
1. Abre **Postman**.
2. Haz clic en el botón **Import** (esquina superior izquierda).
3. Arrastra o selecciona los dos archivos:
   - `MediTurnos_API.postman_collection.json`
   - `MediTurnos_Local_Docker.postman_environment.json`

### Paso 2: Seleccionar el Entorno
1. En la esquina superior derecha de Postman, abre el selector de entornos (dropdown que dice *No Environment*).
2. Selecciona **MediTurnos — Local Docker Environment**.

### Paso 3: Ejecución Automatizada (Collection Runner)
1. Haz clic derecho sobre la colección importada **`🏥 MediTurnos API — Suite E2E (TPI UTN FRC 2026)`**.
2. Selecciona **Run collection**.
3. Asegúrate de que todas las carpetas y peticiones estén tildadas y en orden secuencial (de `00` a `04`).
4. Haz clic en **Run 🏥 MediTurnos API...**.
5. Verás cómo todas las peticiones se ejecutan secuencialmente con **100% de tests aprobados (verde)**.

---

## ⚡ Ejecución desde Terminal con Newman (CLI)

Si tienes Node.js instalado, puedes ejecutar toda la suite directamente desde la línea de comandos sin abrir Postman:

```bash
# Desde la raíz del repositorio
npx newman run docs/postman/MediTurnos_API.postman_collection.json \
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
| | `CV-06` / `RN-06` | **Bloqueo de Agenda**: Profesional bloquea franja por congreso; turnos afectados pasan a `PENDIENTE_REPROGRAMACION`. | `201 Created` |
| | `CV-07` / `RN-07` | **Reprogramación Prioritaria Sin Doble Cobro**: Paciente reprograma turno afectado; pasa directo a `CONFIRMADO` sin nuevo cobro. | `200 OK` |
| | `CV-05` / `RN-05` | **Cancelación y Liberación**: Paciente cancela turno; se liberan inmediatamente los 4 recursos. | `200 OK` |
| | `FUNC-11` | **Cierre Clínico**: Dr. González completa observaciones clínicas y pasa turno a `ATENDIDO`. | `200 OK` |
| **`04. Auditoría y Trazabilidad`**| `RN-08` / `INT-01` | Auditoría de notificaciones persistidas en PostgreSQL con estado de entrega (`ENVIADO`/`ERROR`). | `200 OK` |
| | `TEC-01` | Verificación de trazabilidad distribuida e inyección de `X-Correlation-Id`. | `200 OK` |

---

## 🛡️ Principios Cumplidos en los Contratos (`API-01`)

- **Prohibición de Verbos en las URLs:** Todas las rutas usan sustantivos que representan recursos (`/api/catalogo/sedes`, `/api/catalogo/compatibilidad`, `/api/reservas/turnos/{id}/pagos`, `/api/reservas/turnos/{id}/reprogramacion`, `/api/reservas/turnos/{id}/cancelacion`, `/api/reservas/turnos/{id}/atencion`). La semántica de la operación se define exclusivamente mediante el verbo HTTP (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).
- **Formato Unificado de Errores:** Todos los rechazos de seguridad del Gateway y de negocio de los microservicios devuelven la estructura `ErrorResponseDTO` (`codigo`, `mensaje`, `status`, `timestamp`, `path`).
