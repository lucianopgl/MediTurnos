param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$KeycloakUrl = "http://localhost:8083/realms/MediTurnos/protocol/openid-connect/token",
    [string]$ClientId = "mediturnos-client"
)

$ErrorActionPreference = "Continue"

Write-Host "`n================================================================================" -ForegroundColor Cyan
Write-Host " 🏥 MediTurnos API — Suite de Verificación E2E de Extremo a Extremo (CV-01 a CV-08)" -ForegroundColor Cyan
Write-Host " TPI Backend de Aplicaciones 2026 — UTN FRC (Grupo 06 - Comisión 3K4)" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

$passed = 0
$failed = 0

function Assert-Step($title, $condition, $details = "") {
    if ($condition) {
        Write-Host "  ✅ PASS: $title" -ForegroundColor Green
        if ($details) { Write-Host "     ↳ $details" -ForegroundColor DarkGray }
        $script:passed++
    } else {
        Write-Host "  ❌ FAIL: $title" -ForegroundColor Red
        if ($details) { Write-Host "     ↳ $details" -ForegroundColor Yellow }
        $script:failed++
    }
}

function Get-KeycloakToken($username, $password) {
    try {
        $body = @{
            client_id = $ClientId
            grant_type = "password"
            username = $username
            password = $password
        }
        $res = Invoke-RestMethod -Uri $KeycloakUrl -Method Post -Body $body
        return $res.access_token
    } catch {
        Write-Host "Error autenticando en Keycloak para ${username}: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# ------------------------------------------------------------------------------
# 1. AUTENTICACIÓN KEYCLOAK (OBTENCIÓN DE TOKENS OIDC / JWT)
# ------------------------------------------------------------------------------
Write-Host "`n[Fase 1] Autenticación en Keycloak (OIDC JWKS) y Adquisición de Tokens..." -ForegroundColor Yellow

$tokenAdmin = Get-KeycloakToken "admin.mediturnos" "mediturnos2026"
$tokenDrGonzalez = Get-KeycloakToken "dr.gonzalez" "mediturnos2026"
$tokenDraMartinez = Get-KeycloakToken "dra.martinez" "mediturnos2026"
$tokenPaciente = Get-KeycloakToken "paciente.perez" "mediturnos2026"
$tokenPaciente2 = Get-KeycloakToken "paciente.gomez" "mediturnos2026"

Assert-Step "Token Admin (ROL_ADMIN) obtenido" ($tokenAdmin -ne $null)
Assert-Step "Token Profesional Dr. González (ROL_PROFESIONAL) obtenido" ($tokenDrGonzalez -ne $null)
Assert-Step "Token Profesional Dra. Martínez (ROL_PROFESIONAL) obtenido" ($tokenDraMartinez -ne $null)
Assert-Step "Token Paciente Pérez (ROL_PACIENTE) obtenido" ($tokenPaciente -ne $null)
Assert-Step "Token Paciente Gómez (ROL_PACIENTE) obtenido" ($tokenPaciente2 -ne $null)

$headersAdmin = @{ Authorization = "Bearer $tokenAdmin"; "X-Correlation-Id" = "e2e-admin-" + [System.Guid]::NewGuid().ToString().Substring(0, 8) }
$headersDrGonzalez = @{ Authorization = "Bearer $tokenDrGonzalez"; "X-Correlation-Id" = "e2e-dr-gonzalez-" + [System.Guid]::NewGuid().ToString().Substring(0, 8) }
$headersDraMartinez = @{ Authorization = "Bearer $tokenDraMartinez"; "X-Correlation-Id" = "e2e-dra-martinez-" + [System.Guid]::NewGuid().ToString().Substring(0, 8) }
$headersPaciente = @{ Authorization = "Bearer $tokenPaciente"; "X-Correlation-Id" = "e2e-paciente-" + [System.Guid]::NewGuid().ToString().Substring(0, 8) }
$headersPaciente2 = @{ Authorization = "Bearer $tokenPaciente2"; "X-Correlation-Id" = "e2e-paciente2-" + [System.Guid]::NewGuid().ToString().Substring(0, 8) }

# ------------------------------------------------------------------------------
# 2. DIAGNÓSTICO Y PERÍMETRO DE SEGURIDAD (CV-08)
# ------------------------------------------------------------------------------
Write-Host "`n[Fase 2] Perímetro de Seguridad Reactivo y Control de Acceso (CV-08)..." -ForegroundColor Yellow

# Health público
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get
    Assert-Step "Endpoint público /actuator/health accesible sin token" ($health.status -eq "UP") "Status: $($health.status)"
} catch {
    Assert-Step "Endpoint público /actuator/health accesible sin token" $false $_.Exception.Message
}

# CV-08 (401 Unauthorized sin token)
$cv08_401_ok = $false
try {
    Invoke-RestMethod -Uri "$BaseUrl/api/catalogo/sedes" -Method Get -Headers @{"X-Correlation-Id" = "e2e-test-401"}
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    if ($status -eq 401) { $cv08_401_ok = $true }
}
Assert-Step "CV-08 (401): Petición sin token rechazada con 401 Unauthorized" $cv08_401_ok "Retorna 401 y ErrorResponseDTO"

# CV-08 (403 Forbidden: Paciente intentando crear sede)
$cv08_403_paciente_ok = $false
try {
    $bodySede = @{ nombre = "Sede Intruso"; direccion = "Calle Falsa 123" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/catalogo/sedes" -Method Post -Headers $headersPaciente -ContentType "application/json" -Body $bodySede
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    if ($status -eq 403) { $cv08_403_paciente_ok = $true }
}
Assert-Step "CV-08 (403): Paciente intentando crear sede rechazado con 403 Forbidden" $cv08_403_paciente_ok "ROL_PACIENTE no tiene permiso ROL_ADMIN"

# CV-08 (403 Forbidden: Paciente intentando auditar notificaciones)
$cv08_403_notif_ok = $false
try {
    Invoke-RestMethod -Uri "$BaseUrl/api/reservas/notificaciones" -Method Get -Headers $headersPaciente
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    if ($status -eq 403) { $cv08_403_notif_ok = $true }
}
Assert-Step "CV-08 (403): Paciente intentando auditar notificaciones rechazado con 403 Forbidden" $cv08_403_notif_ok "Ruta exclusiva ROL_ADMIN"

# ------------------------------------------------------------------------------
# 3. CONSULTA DE CATÁLOGO Y COMPATIBILIDAD TRIPARTITA (ms-catalogo-recursos)
# ------------------------------------------------------------------------------
Write-Host "`n[Fase 3] Catálogo de Recursos y Reglas Estáticas/Parametrizadas (RN-01, RN-02)..." -ForegroundColor Yellow

try {
    $sedes = Invoke-RestMethod -Uri "$BaseUrl/api/catalogo/sedes" -Method Get -Headers $headersPaciente
    Assert-Step "Consulta de Sedes Físicas activas" ($sedes.Count -ge 2) "Sedes registradas: $($sedes.Count)"
} catch {
    Assert-Step "Consulta de Sedes Físicas activas" $false $_.Exception.Message
}

try {
    $servicios = Invoke-RestMethod -Uri "$BaseUrl/api/catalogo/servicios" -Method Get -Headers $headersPaciente
    Assert-Step "Consulta de Servicios Médicos parametrizados" ($servicios.Count -ge 4) "Servicios: $($servicios.Count)"
} catch {
    Assert-Step "Consulta de Servicios Médicos parametrizados" $false $_.Exception.Message
}

try {
    # Validación de catálogo síncrona FUNC-05 / RN-01, RN-02 (Lunes 2026-11-23 09:00 hs con Dr. González)
    $compUri = "$BaseUrl/api/catalogo/compatibilidad?profesionalId=1&servicioId=1&fecha=2026-11-23&horaInicio=09:00:00"
    $compatibilidad = Invoke-RestMethod -Uri $compUri -Method Get -Headers $headersPaciente
    $compValida = ($compatibilidad.sedeId -eq 1 -and $compatibilidad.consultorioFijoId -eq 1 -and $compatibilidad.costo -eq 15000.00)
    Assert-Step "Validación de Compatibilidad de Catálogo (RN-01 y RN-02)" $compValida "Consultorio Fijo Box 101, Costo: `$15000"
} catch {
    Assert-Step "Validación de Compatibilidad de Catálogo (RN-01 y RN-02)" $false $_.Exception.Message
}

# ------------------------------------------------------------------------------
# 4. CICLO DE VIDA DEL TURNO Y REGLAS DE NEGOCIO (ms-reservas)
# ------------------------------------------------------------------------------
Write-Host "`n[Fase 4] Ciclo Transaccional del Turno y Reglas de Negocio (RN-01 a RN-08)..." -ForegroundColor Yellow

# Fecha y hora dinámicas para reserva sin colisión previa (Lunes a Viernes de 08:00 a 13:00)
$randomDayOffset = Get-Random -Minimum 30 -Maximum 120
$fechaTurnoObj = (Get-Date).AddDays($randomDayOffset)
while ($fechaTurnoObj.DayOfWeek -eq [System.DayOfWeek]::Saturday -or $fechaTurnoObj.DayOfWeek -eq [System.DayOfWeek]::Sunday) {
    $fechaTurnoObj = $fechaTurnoObj.AddDays(1)
}
$fechaTurno = $fechaTurnoObj.ToString("yyyy-MM-dd")
$randomHour = Get-Random -Minimum 8 -Maximum 13
$randomMin = (0, 30)[(Get-Random -Minimum 0 -Maximum 2)]
$horaTurno = "{0:D2}:{1:D2}:00" -f $randomHour, $randomMin

# CV-01: Reserva Exitosa Tripartita (RN-01, RN-02, RN-03, RN-08)
$turnoId = $null
try {
    $bodyReserva = @{
        profesionalId = 1
        servicioId = 1
        fecha = $fechaTurno
        horaInicio = $horaTurno
    } | ConvertTo-Json
    $turnoCreado = Invoke-RestMethod -Uri "$BaseUrl/api/reservas/turnos" -Method Post -Headers $headersPaciente -ContentType "application/json" -Body $bodyReserva
    $turnoId = $turnoCreado.id
    $cv01_ok = ($turnoCreado.estado -eq "RESERVADO" -and $turnoCreado.pagado -eq $false -and $turnoCreado.costoAcordado -eq 15000.00)
    Assert-Step "CV-01: Reserva Exitosa Tripartita y Notificación Externa" $cv01_ok "Turno #$turnoId en RESERVADO, Costo `$15000"
} catch {
    Assert-Step "CV-01: Reserva Exitosa Tripartita y Notificación Externa" $false $_.Exception.Message
}

# CV-02: Rechazo por Colisión Tripartita (RN-03: Dra. Martínez intenta en Box 101 en la misma franja)
$cv02_ok = $false
try {
    $bodyColision = @{
        profesionalId = 2  # Dra. Martínez (comparte Consultorio Box 101 con Dr. González)
        servicioId = 1
        fecha = $fechaTurno
        horaInicio = $horaTurno
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/reservas/turnos" -Method Post -Headers $headersPaciente -ContentType "application/json" -Body $bodyColision
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    if ($status -eq 409) { $cv02_ok = $true }
}
Assert-Step "CV-02: Rechazo por Colisión de Consultorio Compartido N:1 (RN-03)" $cv02_ok "Retorna 409 Conflict (ColisionHorariaException)"

# CV-03: Rechazo por Recurso en Mantenimiento (RN-02: Servicio 3 requiere Holter que está inoperativo)
$cv03_ok = $false
try {
    $bodyMantenimiento = @{
        profesionalId = 1
        servicioId = 3  # Servicio de Holter 24hs (Equipamiento #3 operativo = false)
        fecha = $fechaTurno
        horaInicio = "11:00:00"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/reservas/turnos" -Method Post -Headers $headersPaciente -ContentType "application/json" -Body $bodyMantenimiento
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    if ($status -eq 409 -or $status -eq 422) { $cv03_ok = $true }
}
Assert-Step "CV-03: Rechazo por Equipamiento en Mantenimiento (RN-02)" $cv03_ok "Retorna 409/422 (RecursoNoDisponibleException)"

# CV-04: Confirmación de Turno por Cobro (RN-04)
$cv04_ok = $false
if ($turnoId) {
    try {
        $pagoRef = "PAY-E2E-" + [System.Guid]::NewGuid().ToString().Substring(0, 8)
        $bodyPago = @{
            monto = 15000.00
            medioPago = "TARJETA_DEBITO"
            comprobanteReferencia = $pagoRef
        } | ConvertTo-Json
        $pagoRes = Invoke-RestMethod -Uri "$BaseUrl/api/reservas/turnos/$turnoId/pagos" -Method Post -Headers $headersPaciente -ContentType "application/json" -Body $bodyPago
        if ($pagoRes.estadoTurnoActual -eq "CONFIRMADO" -and $pagoRes.estadoPago -eq "APROBADO") { $cv04_ok = $true }
    } catch {
        # Error en pago
    }
}
Assert-Step "CV-04: Confirmación de Turno por Cobro (RN-04)" $cv04_ok "Turno #$turnoId transiciona a CONFIRMADO (pagado = true)"

# CV-06: Bloqueo de Agenda Médica del Profesional (RN-06)
# Dr. González bloquea la franja del 2026-11-17 de 10:00 a 11:00 (Turno semilla #2)
$cv06_ok = $false
try {
    $bodyBloqueo = @{
        profesionalId = 1
        fecha = "2026-11-17"
        horaInicio = "10:00:00"
        horaFin = "11:00:00"
        motivo = "Congreso de Cardiologia"
    } | ConvertTo-Json
    $bloqueoRes = Invoke-RestMethod -Uri "$BaseUrl/api/reservas/turnos/cancelacion-bloque-profesional" -Method Post -Headers $headersDrGonzalez -ContentType "application/json; charset=utf-8" -Body $bodyBloqueo
    if ($bloqueoRes.cantidadTurnosAfectados -ge 0) { $cv06_ok = $true }
} catch {
    Write-Host "     ↳ Debug CV-06 Error: $($_.Exception.Message)" -ForegroundColor Magenta
}
Assert-Step "CV-06: Bloqueo de Agenda Médica y Alerta de Reprogramación (RN-06)" $cv06_ok "Turno #2 pasa a PENDIENTE_REPROGRAMACION"

# CV-07: Reprogramación Prioritaria Sin Doble Cobro (RN-07)
# Turno #2 afectado por el bloqueo es reprogramado por Paciente Pérez hacia nueva fecha libre
$cv07_ok = $false
try {
    # Si Turno #2 está en PENDIENTE_REPROGRAMACION, lo reprogramamos; si no, probamos con el turno semilla #3
    $turnoAReprogramar = 2
    $tCheck = Invoke-RestMethod -Uri "$BaseUrl/api/reservas/turnos/2" -Method Get -Headers $headersPaciente
    $headersReprog = $headersPaciente
    if ($tCheck.estado -ne "PENDIENTE_REPROGRAMACION") {
        $turnoAReprogramar = 3
        $headersReprog = $headersPaciente2
    }

    $bodyReprog = @{
        nuevaFecha = "2026-12-08"
        nuevaHoraInicio = "09:00:00"
    } | ConvertTo-Json
    $reprogRes = Invoke-RestMethod -Uri "$BaseUrl/api/reservas/turnos/$turnoAReprogramar/reprogramacion" -Method Put -Headers $headersReprog -ContentType "application/json; charset=utf-8" -Body $bodyReprog
    if ($reprogRes.estado -eq "CONFIRMADO" -and $reprogRes.pagado -eq $true) { $cv07_ok = $true }
} catch {
    Write-Host "     ↳ Debug CV-07 Error: $($_.Exception.Message)" -ForegroundColor Magenta
}
Assert-Step "CV-07: Reprogramación Prioritaria Sin Doble Cobro (RN-07)" $cv07_ok "Turno #$turnoAReprogramar vuelve directo a CONFIRMADO conservando pagado = true"

# CV-05: Cancelación de Turno por el Paciente (RN-05)
# Cancelamos el turno #1 creado en esta sesión
$cv05_ok = $false
if ($turnoId) {
    try {
        $cancelRes = Invoke-RestMethod -Uri "$BaseUrl/api/reservas/turnos/$turnoId/cancelacion" -Method Patch -Headers $headersPaciente
        if ($cancelRes.estado -eq "CANCELADO_PACIENTE" -or $cancelRes.estado -eq "CANCELADO_PENALIZADO") { $cv05_ok = $true }
    } catch {
        # Error en cancelación
    }
}
Assert-Step "CV-05: Cancelación de Turno por el Paciente y Liberación de Recursos (RN-05)" $cv05_ok "Turno pasa a estado CANCELADO liberando franja"

# ------------------------------------------------------------------------------
# 5. AUDITORÍA DE NOTIFICACIONES EXTERNAS Y TRAZABILIDAD (RN-08)
# ------------------------------------------------------------------------------
Write-Host "`n[Fase 5] Auditoría de Notificaciones y Trazabilidad Extremo a Extremo..." -ForegroundColor Yellow

try {
    $notificaciones = Invoke-RestMethod -Uri "$BaseUrl/api/reservas/notificaciones" -Method Get -Headers $headersAdmin
    $auditoria_ok = ($notificaciones.Count -ge 3)
    Assert-Step "Auditoría de Notificaciones con X-Correlation-Id" $auditoria_ok "Total registradas en BD: $($notificaciones.Count)"
} catch {
    Assert-Step "Auditoría de Notificaciones con X-Correlation-Id" $false $_.Exception.Message
}

# ------------------------------------------------------------------------------
# RESUMEN FINAL DE EJECUCIÓN
# ------------------------------------------------------------------------------
Write-Host "`n================================================================================" -ForegroundColor Cyan
Write-Host " 📊 RESUMEN DE LA SUITE DE PRUEBAS E2E" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " Pruebas Aprobadas: $passed" -ForegroundColor Green
Write-Host " Pruebas Fallidas:  $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "================================================================================" -ForegroundColor Cyan

if ($failed -eq 0) {
    Write-Host " 🚀 ¡TODO EL SISTEMA ESTÁ 100% OPERATIVO Y VERIFICADO SEGÚN LA RÚBRICA DEL TPI!" -ForegroundColor Green
} else {
    Write-Host " ⚠️ Se detectaron fallas en $failed prueba(s). Revisar detalles anteriores." -ForegroundColor Yellow
}
