-- =============================================================================
-- MEDITURNOS API — INICIALIZACIÓN DE ESQUEMAS Y DATOS DE PRUEBA (TPI 2026)
-- Universidad Tecnológica Nacional — Facultad Regional Córdoba (UTN FRC)
-- Cátedra: Backend de Aplicaciones — Grupo 06 (Comisión 3K4)
--
-- Cumple estrictamente:
--   * ADR 001: Única instancia PostgreSQL 16 con aislamiento lógico estricto
--     en dos esquemas (`schema_catalogo` y `schema_reservas`) SIN claves
--     foráneas ni consultas cruzadas entre esquemas.
--   * DER Sección 7 del README.md (10 tablas en catálogo + 5 tablas en reservas).
--   * Seed Data preconfigurados y sincronizados con Keycloak (`realm-export.json`)
--     para verificar CV-01 a CV-08 de extremo a extremo.
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS schema_catalogo;
CREATE SCHEMA IF NOT EXISTS schema_reservas;

-- =============================================================================
-- ESQUEMA 1: schema_catalogo (Administrado exclusivamente por ms-catalogo-recursos)
-- =============================================================================

CREATE TABLE IF NOT EXISTS schema_catalogo.sedes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL UNIQUE,
    direccion VARCHAR(255) NOT NULL,
    telefono VARCHAR(50),
    activa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS schema_catalogo.consultorios (
    id BIGSERIAL PRIMARY KEY,
    sede_id BIGINT NOT NULL REFERENCES schema_catalogo.sedes(id),
    codigo_nombre VARCHAR(80) NOT NULL,
    operativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_consultorio_sede_codigo UNIQUE (sede_id, codigo_nombre)
);

CREATE TABLE IF NOT EXISTS schema_catalogo.especialidades (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    activa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS schema_catalogo.profesionales (
    id BIGSERIAL PRIMARY KEY,
    keycloak_sub VARCHAR(64) NOT NULL UNIQUE,
    especialidad_id BIGINT NOT NULL REFERENCES schema_catalogo.especialidades(id),
    consultorio_fijo_id BIGINT NOT NULL REFERENCES schema_catalogo.consultorios(id),
    matricula VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS schema_catalogo.horarios_profesional (
    id BIGSERIAL PRIMARY KEY,
    profesional_id BIGINT NOT NULL REFERENCES schema_catalogo.profesionales(id),
    dia_semana VARCHAR(20) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_horario_rango CHECK (hora_fin > hora_inicio)
);

CREATE TABLE IF NOT EXISTS schema_catalogo.bloqueos_profesional (
    id BIGSERIAL PRIMARY KEY,
    profesional_id BIGINT NOT NULL REFERENCES schema_catalogo.profesionales(id),
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    motivo VARCHAR(255),
    CONSTRAINT chk_bloqueo_rango CHECK (hora_fin > hora_inicio)
);

CREATE TABLE IF NOT EXISTS schema_catalogo.servicios (
    id BIGSERIAL PRIMARY KEY,
    especialidad_id BIGINT NOT NULL REFERENCES schema_catalogo.especialidades(id),
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    duracion_minutos INT NOT NULL CHECK (duracion_minutos > 0),
    costo NUMERIC(10, 2) NOT NULL CHECK (costo >= 0),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS schema_catalogo.profesional_servicios (
    id BIGSERIAL PRIMARY KEY,
    profesional_id BIGINT NOT NULL REFERENCES schema_catalogo.profesionales(id),
    servicio_id BIGINT NOT NULL REFERENCES schema_catalogo.servicios(id),
    CONSTRAINT uk_profesional_servicio UNIQUE (profesional_id, servicio_id)
);

CREATE TABLE IF NOT EXISTS schema_catalogo.equipamientos (
    id BIGSERIAL PRIMARY KEY,
    sede_id BIGINT NOT NULL REFERENCES schema_catalogo.sedes(id),
    nombre VARCHAR(150) NOT NULL,
    codigo_inventario VARCHAR(80) NOT NULL UNIQUE,
    operativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS schema_catalogo.servicio_equipamientos (
    id BIGSERIAL PRIMARY KEY,
    servicio_id BIGINT NOT NULL REFERENCES schema_catalogo.servicios(id),
    equipamiento_id BIGINT NOT NULL REFERENCES schema_catalogo.equipamientos(id),
    CONSTRAINT uk_servicio_equipamiento UNIQUE (servicio_id, equipamiento_id)
);

CREATE INDEX IF NOT EXISTS idx_horarios_prof_dia
    ON schema_catalogo.horarios_profesional(profesional_id, dia_semana, activo);

CREATE INDEX IF NOT EXISTS idx_bloqueos_prof_fecha
    ON schema_catalogo.bloqueos_profesional(profesional_id, fecha);

-- =============================================================================
-- ESQUEMA 2: schema_reservas (Administrado exclusivamente por ms-reservas)
-- SIN FOREIGN KEYS HACIA schema_catalogo (Bajo Acoplamiento — ADR 001)
-- =============================================================================

CREATE TABLE IF NOT EXISTS schema_reservas.pacientes (
    id BIGSERIAL PRIMARY KEY,
    keycloak_sub VARCHAR(64) NOT NULL UNIQUE,
    dni VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    telefono_whatsapp VARCHAR(40) NOT NULL,
    cantidad_penalizaciones INT NOT NULL DEFAULT 0 CHECK (cantidad_penalizaciones >= 0),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_alta TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS schema_reservas.turnos (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES schema_reservas.pacientes(id),
    profesional_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    consultorio_id BIGINT NOT NULL,
    sede_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    costo_acordado NUMERIC(10, 2) NOT NULL CHECK (costo_acordado >= 0),
    estado VARCHAR(40) NOT NULL,
    pagado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    CONSTRAINT chk_turno_rango CHECK (hora_fin > hora_inicio),
    CONSTRAINT chk_turno_estado CHECK (estado IN (
        'RESERVADO',
        'CONFIRMADO',
        'CANCELADO_PACIENTE',
        'CANCELADO_PENALIZADO',
        'PENDIENTE_REPROGRAMACION',
        'ATENDIDO',
        'AUSENTE'
    ))
);

CREATE TABLE IF NOT EXISTS schema_reservas.turno_equipamientos (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT NOT NULL REFERENCES schema_reservas.turnos(id) ON DELETE CASCADE,
    equipamiento_id BIGINT NOT NULL,
    CONSTRAINT uk_turno_equipamiento UNIQUE (turno_id, equipamiento_id)
);

CREATE TABLE IF NOT EXISTS schema_reservas.pagos (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT NOT NULL REFERENCES schema_reservas.turnos(id),
    comprobante_referencia VARCHAR(100) NOT NULL UNIQUE,
    medio_pago VARCHAR(50) NOT NULL,
    monto NUMERIC(10, 2) NOT NULL CHECK (monto > 0),
    estado_pago VARCHAR(30) NOT NULL,
    fecha_pago TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS schema_reservas.notificaciones (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT REFERENCES schema_reservas.turnos(id),
    paciente_id BIGINT NOT NULL REFERENCES schema_reservas.pacientes(id),
    canal VARCHAR(30) NOT NULL,
    destinatario VARCHAR(150) NOT NULL,
    tipo_alerta VARCHAR(50) NOT NULL,
    estado_envio VARCHAR(30) NOT NULL,
    detalle_mensaje TEXT,
    fecha_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_turnos_colision_fecha_estado
    ON schema_reservas.turnos(fecha, estado, hora_inicio, hora_fin);

CREATE INDEX IF NOT EXISTS idx_turnos_profesional_fecha
    ON schema_reservas.turnos(profesional_id, fecha);

CREATE INDEX IF NOT EXISTS idx_turnos_consultorio_fecha
    ON schema_reservas.turnos(consultorio_id, fecha);

-- =============================================================================
-- SEED DATA — CATÁLOGO DE RECURSOS (schema_catalogo)
-- =============================================================================

-- 1. Sedes Físicas
INSERT INTO schema_catalogo.sedes (id, nombre, direccion, telefono, activa) VALUES
    (1, 'Sede Centro Córdoba', 'Av. Vélez Sarsfield 250, Córdoba', '+54 351 420-1000', TRUE),
    (2, 'Sede Cerro de las Rosas', 'Av. Rafael Núñez 4150, Córdoba', '+54 351 481-2000', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 2. Consultorios Físicos (Relación N:1 con Profesionales)
-- Box 101 es compartido entre Dr. González (id=1) y Dra. Martínez (id=2) para demostrar CV-02 (Colisión N:1)
-- Box 103 está en mantenimiento (operativo = FALSE) para demostrar RN-02 / CV-03
INSERT INTO schema_catalogo.consultorios (id, sede_id, codigo_nombre, operativo) VALUES
    (1, 1, 'Box 101 - Cardiología', TRUE),
    (2, 1, 'Box 102 - Diagnóstico e Imágenes', TRUE),
    (3, 1, 'Box 103 - En Mantenimiento', FALSE),
    (4, 2, 'Box 201 - Consultorio Cerro', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 3. Especialidades Médicas
INSERT INTO schema_catalogo.especialidades (id, nombre, descripcion, activa) VALUES
    (1, 'Cardiología', 'Estudios cardiovasculares, clínicos y funcionales', TRUE),
    (2, 'Diagnóstico por Imágenes', 'Ecografías generales y Doppler vascular/cardíaco', TRUE),
    (3, 'Clínica Médica', 'Atención clínica ambulatoria integral', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 4. Equipamientos Médicos de Diagnóstico (Relación N:N con Servicios)
-- Equipo id=3 está en mantenimiento (operativo = FALSE) para demostrar CV-03
INSERT INTO schema_catalogo.equipamientos (id, sede_id, nombre, codigo_inventario, operativo) VALUES
    (1, 1, 'Ecógrafo Doppler Color Mindray #1', 'EQ-CEN-ECO-001', TRUE),
    (2, 1, 'Electrocardiógrafo Digital 12 Derivaciones #1', 'EQ-CEN-ECG-001', TRUE),
    (3, 1, 'Grabador Holter 24hs #1 (En Mantenimiento)', 'EQ-CEN-HOL-001', FALSE),
    (4, 2, 'Ecógrafo Doppler Sede Cerro #1', 'EQ-CER-ECO-001', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 5. Servicios Médicos (Duración en minutos y Costo parametrizados)
INSERT INTO schema_catalogo.servicios (id, especialidad_id, nombre, descripcion, duracion_minutos, costo, activo) VALUES
    (1, 1, 'Consulta Cardiológica con Electrocardiograma', 'Evaluación clínica cardiológica con ECG de 12 derivaciones', 30, 15000.00, TRUE),
    (2, 1, 'Ecocardiograma Doppler Color Completo', 'Estudio ecocardiográfico transtorácico con Doppler y sincronización ECG', 45, 32000.00, TRUE),
    (3, 1, 'Colocación de Holter 24 Horas', 'Monitoreo ambulatorio continuo de ritmo cardíaco (requiere equipo en mantenimiento para CV-03)', 20, 25000.00, TRUE),
    (4, 3, 'Consulta de Clínica Médica General', 'Consulta médica ambulatoria sin requerimiento de equipamiento adicional', 30, 12000.00, TRUE)
ON CONFLICT (id) DO NOTHING;

-- 6. Relación N:N Servicio <-> Equipamientos Requeridos
-- Servicio 1 requiere ECG (#2)
-- Servicio 2 requiere Ecógrafo Doppler (#1) + ECG (#2) -> demuestra validación N:N
-- Servicio 3 requiere Holter (#3, en mantenimiento -> falla con 409/422 en CV-03)
INSERT INTO schema_catalogo.servicio_equipamientos (id, servicio_id, equipamiento_id) VALUES
    (1, 1, 2),
    (2, 2, 1),
    (3, 2, 2),
    (4, 3, 3)
ON CONFLICT (id) DO NOTHING;

-- 7. Profesionales de la Salud (Sincronizados con UUIDs de Keycloak en realm-export.json)
-- Dr. Carlos González (id=1) y Dra. Laura Martínez (id=2) COMPARTEN el Consultorio Fijo id=1 (Box 101)
INSERT INTO schema_catalogo.profesionales (id, keycloak_sub, especialidad_id, consultorio_fijo_id, matricula, nombre, apellido, email, activo) VALUES
    (1, '00000000-0000-0000-0000-000000000002', 1, 1, 'MP-34501', 'Carlos', 'González', 'dr.gonzalez@mediturnos.com.ar', TRUE),
    (2, '00000000-0000-0000-0000-000000000003', 1, 1, 'MP-38920', 'Laura', 'Martínez', 'dra.martinez@mediturnos.com.ar', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 8. Servicios Habilitados por Profesional (N:N)
INSERT INTO schema_catalogo.profesional_servicios (id, profesional_id, servicio_id) VALUES
    (1, 1, 1),
    (2, 1, 2),
    (3, 1, 3),
    (4, 2, 1),
    (5, 2, 2)
ON CONFLICT (id) DO NOTHING;

-- 9. Plantillas Horarias Semanales de los Profesionales (RN-01)
INSERT INTO schema_catalogo.horarios_profesional (id, profesional_id, dia_semana, hora_inicio, hora_fin, activo) VALUES
    (1, 1, 'LUNES',     '08:00:00', '14:00:00', TRUE),
    (2, 1, 'MARTES',    '08:00:00', '14:00:00', TRUE),
    (3, 1, 'MIERCOLES', '08:00:00', '14:00:00', TRUE),
    (4, 1, 'JUEVES',    '08:00:00', '14:00:00', TRUE),
    (5, 1, 'VIERNES',   '08:00:00', '14:00:00', TRUE),
    (6, 2, 'LUNES',     '09:00:00', '15:00:00', TRUE),
    (7, 2, 'MARTES',    '09:00:00', '15:00:00', TRUE),
    (8, 2, 'MIERCOLES', '09:00:00', '15:00:00', TRUE),
    (9, 2, 'JUEVES',    '09:00:00', '15:00:00', TRUE),
    (10, 2, 'VIERNES',  '09:00:00', '15:00:00', TRUE)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- SEED DATA — RESERVAS, PACIENTES Y TURNOS DE PRUEBA (schema_reservas)
-- =============================================================================

-- 1. Pacientes (Sincronizados con UUIDs de Keycloak en realm-export.json)
INSERT INTO schema_reservas.pacientes (id, keycloak_sub, dni, nombre, apellido, email, telefono_whatsapp, cantidad_penalizaciones, activo) VALUES
    (1, '00000000-0000-0000-0000-000000000004', '40111222', 'Juan', 'Pérez', 'paciente.perez@mediturnos.com.ar', '+5493515550101', 0, TRUE),
    (2, '00000000-0000-0000-0000-000000000005', '42333444', 'María', 'Gómez', 'paciente.gomez@mediturnos.com.ar', '+5493515550202', 0, TRUE)
ON CONFLICT (id) DO NOTHING;

-- 2. Turnos Semilla para Pruebas Reproducibles (CV-02, CV-04, CV-06, CV-07)
-- Turno #1: RESERVADO (pagado=false) el próximo Lunes 2026-11-16 de 09:00 a 09:30 con Dr. González (Box 101, Equipo #2)
--           Sirve para probar CV-04 (Confirmación de pago) y CV-02 (Colisión si Dra. Martínez intenta reservar de 09:00 a 09:30 en Box 101).
-- Turno #2: CONFIRMADO (pagado=true) el Martes 2026-11-17 de 10:00 a 10:45 con Dr. González (Ecocardiograma Doppler, Equipos #1 y #2).
--           Sirve para probar CV-06 (Bloqueo de agenda médica del Dr. González en esa fecha).
-- Turno #3: PENDIENTE_REPROGRAMACION con pagado=true (ya abonado previamente).
--           Sirve para probar directamente CV-07 (Reprogramación prioritaria sin doble cobro -> pasa directo a CONFIRMADO).
INSERT INTO schema_reservas.turnos (
    id, paciente_id, profesional_id, servicio_id, consultorio_id, sede_id,
    fecha, hora_inicio, hora_fin, costo_acordado, estado, pagado
) VALUES
    (1, 1, 1, 1, 1, 1, '2026-11-16', '09:00:00', '09:30:00', 15000.00, 'RESERVADO', FALSE),
    (2, 1, 1, 2, 1, 1, '2026-11-17', '10:00:00', '10:45:00', 32000.00, 'CONFIRMADO', TRUE),
    (3, 2, 1, 1, 1, 1, '2026-11-18', '11:00:00', '11:30:00', 15000.00, 'PENDIENTE_REPROGRAMACION', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 3. Equipamientos asignados a los Turnos Semilla
INSERT INTO schema_reservas.turno_equipamientos (id, turno_id, equipamiento_id) VALUES
    (1, 1, 2),
    (2, 2, 1),
    (3, 2, 2),
    (4, 3, 2)
ON CONFLICT (id) DO NOTHING;

-- 4. Pagos registrados para los turnos ya abonados (#2 y #3)
INSERT INTO schema_reservas.pagos (id, turno_id, comprobante_referencia, medio_pago, monto, estado_pago) VALUES
    (1, 2, 'PAY-SEED-2026-0001', 'TARJETA_DEBITO', 32000.00, 'APROBADO'),
    (2, 3, 'PAY-SEED-2026-0002', 'TRANSFERENCIA',  15000.00, 'APROBADO')
ON CONFLICT (id) DO NOTHING;

-- 5. Notificaciones de auditoría inicial
INSERT INTO schema_reservas.notificaciones (id, turno_id, paciente_id, canal, destinatario, tipo_alerta, estado_envio, detalle_mensaje) VALUES
    (1, 1, 1, 'TELEGRAM', '+5493515550101', 'RESERVA', 'ENVIADO', 'Reserva inicial registrada en estado RESERVADO para el 2026-11-16 09:00 hs.'),
    (2, 2, 1, 'TELEGRAM', '+5493515550101', 'CONFIRMACION', 'ENVIADO', 'Pago aprobado y turno CONFIRMADO para el 2026-11-17 10:00 hs.'),
    (3, 3, 2, 'TELEGRAM', '+5493515550202', 'REPROGRAMACION', 'ENVIADO', 'Su turno del 2026-11-18 pasó a PENDIENTE_REPROGRAMACION por bloqueo de agenda médica.')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- ACTUALIZACIÓN DE SECUENCIAS BIGSERIAL TRAS INSERCIÓN DE SEED DATA
-- =============================================================================

SELECT setval(pg_get_serial_sequence('schema_catalogo.sedes', 'id'), COALESCE((SELECT MAX(id) FROM schema_catalogo.sedes), 1));
SELECT setval(pg_get_serial_sequence('schema_catalogo.consultorios', 'id'), COALESCE((SELECT MAX(id) FROM schema_catalogo.consultorios), 1));
SELECT setval(pg_get_serial_sequence('schema_catalogo.especialidades', 'id'), COALESCE((SELECT MAX(id) FROM schema_catalogo.especialidades), 1));
SELECT setval(pg_get_serial_sequence('schema_catalogo.equipamientos', 'id'), COALESCE((SELECT MAX(id) FROM schema_catalogo.equipamientos), 1));
SELECT setval(pg_get_serial_sequence('schema_catalogo.servicios', 'id'), COALESCE((SELECT MAX(id) FROM schema_catalogo.servicios), 1));
SELECT setval(pg_get_serial_sequence('schema_catalogo.servicio_equipamientos', 'id'), COALESCE((SELECT MAX(id) FROM schema_catalogo.servicio_equipamientos), 1));
SELECT setval(pg_get_serial_sequence('schema_catalogo.profesionales', 'id'), COALESCE((SELECT MAX(id) FROM schema_catalogo.profesionales), 1));
SELECT setval(pg_get_serial_sequence('schema_catalogo.profesional_servicios', 'id'), COALESCE((SELECT MAX(id) FROM schema_catalogo.profesional_servicios), 1));
SELECT setval(pg_get_serial_sequence('schema_catalogo.horarios_profesional', 'id'), COALESCE((SELECT MAX(id) FROM schema_catalogo.horarios_profesional), 1));
SELECT setval(pg_get_serial_sequence('schema_catalogo.bloqueos_profesional', 'id'), COALESCE((SELECT MAX(id) FROM schema_catalogo.bloqueos_profesional), 1), false);

SELECT setval(pg_get_serial_sequence('schema_reservas.pacientes', 'id'), COALESCE((SELECT MAX(id) FROM schema_reservas.pacientes), 1));
SELECT setval(pg_get_serial_sequence('schema_reservas.turnos', 'id'), COALESCE((SELECT MAX(id) FROM schema_reservas.turnos), 1));
SELECT setval(pg_get_serial_sequence('schema_reservas.turno_equipamientos', 'id'), COALESCE((SELECT MAX(id) FROM schema_reservas.turno_equipamientos), 1));
SELECT setval(pg_get_serial_sequence('schema_reservas.pagos', 'id'), COALESCE((SELECT MAX(id) FROM schema_reservas.pagos), 1));
SELECT setval(pg_get_serial_sequence('schema_reservas.notificaciones', 'id'), COALESCE((SELECT MAX(id) FROM schema_reservas.notificaciones), 1));
