package utn.frc.backend.mediturnos.reservas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utn.frc.backend.mediturnos.common.dto.BloqueoAgendaRequestDTO;
import utn.frc.backend.mediturnos.common.dto.CompatibilidadRecursoDTO;
import utn.frc.backend.mediturnos.common.enums.EstadoPago;
import utn.frc.backend.mediturnos.common.enums.EstadoTurno;
import utn.frc.backend.mediturnos.common.enums.TipoAlertaNotificacion;
import utn.frc.backend.mediturnos.common.exception.EstadoTurnoInvalidoException;
import utn.frc.backend.mediturnos.common.exception.RecursoNoEncontradoException;
import utn.frc.backend.mediturnos.reservas.client.CatalogoClient;
import utn.frc.backend.mediturnos.reservas.dto.*;
import utn.frc.backend.mediturnos.reservas.entity.*;
import utn.frc.backend.mediturnos.reservas.repository.PagoRepository;
import utn.frc.backend.mediturnos.reservas.repository.TurnoRepository;
import utn.frc.backend.mediturnos.reservas.repository.TurnoSpecifications;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final PagoRepository pagoRepository;
    private final PacienteService pacienteService;
    private final ValidadorColisionService validadorColisionService;
    private final CatalogoClient catalogoClient;
    private final NotificacionService notificacionService;
    private final Clock clock;

    /**
     * Solicitar y reservar atómicamente un turno médico (FUNC-07 / RN-01, RN-02, RN-03, RN-08).
     */
    @Transactional
    public TurnoResponseDTO solicitarTurno(TurnoRequestDTO request) {
        Paciente paciente = resolverPacienteParaOperacion(request.getPacienteId());

        // 1. Validación de Catálogo (RN-01 y RN-02) vía RestClient síncrono
        CompatibilidadRecursoDTO compatibilidad = catalogoClient.obtenerCompatibilidad(
                request.getProfesionalId(),
                request.getServicioId(),
                request.getFecha(),
                request.getHoraInicio()
        );

        LocalTime horaFin = request.getHoraInicio().plusMinutes(compatibilidad.getDuracionMinutos());

        // 2. Validación de No Colisión Horaria Tripartita y de Paciente en schema_reservas (RN-03)
        validadorColisionService.validarNoColision(
                paciente.getId(),
                compatibilidad.getProfesionalId(),
                compatibilidad.getConsultorioFijoId(),
                compatibilidad.getEquipamientosIds(),
                request.getFecha(),
                request.getHoraInicio(),
                horaFin,
                null
        );

        // 3. Persistencia del Turno en estado RESERVADO
        Turno turno = Turno.builder()
                .paciente(paciente)
                .profesionalId(compatibilidad.getProfesionalId())
                .servicioId(compatibilidad.getServicioId())
                .consultorioId(compatibilidad.getConsultorioFijoId())
                .sedeId(compatibilidad.getSedeId())
                .fecha(request.getFecha())
                .horaInicio(request.getHoraInicio())
                .horaFin(horaFin)
                .costoAcordado(compatibilidad.getCosto())
                .estado(EstadoTurno.RESERVADO)
                .pagado(false)
                .build();

        if (compatibilidad.getEquipamientosIds() != null) {
            for (Long eqId : compatibilidad.getEquipamientosIds()) {
                turno.agregarEquipamiento(eqId);
            }
        }

        Turno guardado = turnoRepository.save(turno);

        // 4. Despacho de Notificación Externa con Resiliencia (RN-08)
        String mensaje = String.format("Reserva confirmada. Turno #%d para el %s a las %s hs. Monto a abonar: $%s.",
                guardado.getId(), guardado.getFecha(), guardado.getHoraInicio(), guardado.getCostoAcordado());
        notificacionService.enviarNotificacionResiliente(guardado, paciente, TipoAlertaNotificacion.RESERVA, mensaje);

        return mapToDTO(guardado);
    }

    /**
     * Registración de Pago y Confirmación de Turno (FUNC-08 / RN-04).
     */
    @Transactional
    public PagoResponseDTO registrarPago(Long turnoId, PagoRequestDTO request) {
        Turno turno = obtenerTurnoVerificandoAcceso(turnoId);

        if (turno.getEstado() != EstadoTurno.RESERVADO) {
            throw new EstadoTurnoInvalidoException(
                    "Solo pueden registrarse pagos sobre turnos en estado RESERVADO. Estado actual: " + turno.getEstado(),
                    "RN-04"
            );
        }

        if (Boolean.TRUE.equals(turno.getPagado())) {
            throw new EstadoTurnoInvalidoException("El turno ya se encuentra pagado", "RN-04");
        }

        if (request.getMonto().compareTo(turno.getCostoAcordado()) != 0) {
            throw new EstadoTurnoInvalidoException(
                    String.format("El monto ingresado ($%s) no coincide con el costo acordado del turno ($%s)",
                            request.getMonto(), turno.getCostoAcordado()),
                    "RN-04"
            );
        }

        if (pagoRepository.existsByComprobanteReferencia(request.getComprobanteReferencia())) {
            throw new EstadoTurnoInvalidoException("El comprobante de referencia ya fue registrado previamente", "RN-04");
        }

        Pago pago = Pago.builder()
                .turno(turno)
                .comprobanteReferencia(request.getComprobanteReferencia().trim())
                .medioPago(request.getMedioPago().trim())
                .monto(request.getMonto())
                .estadoPago(EstadoPago.APROBADO)
                .build();

        Pago guardado = pagoRepository.save(pago);

        // Transición de estado a CONFIRMADO y pagado=true
        turno.setPagado(true);
        turno.setEstado(EstadoTurno.CONFIRMADO);
        turnoRepository.save(turno);

        // Notificación de confirmación de pago (RN-08)
        String mensaje = String.format("Pago de $%s recibido con éxito. Su turno #%d para el %s ha sido CONFIRMADO.",
                guardado.getMonto(), turno.getId(), turno.getFecha());
        notificacionService.enviarNotificacionResiliente(turno, turno.getPaciente(), TipoAlertaNotificacion.CONFIRMACION, mensaje);

        return PagoResponseDTO.builder()
                .id(guardado.getId())
                .turnoId(turno.getId())
                .comprobanteReferencia(guardado.getComprobanteReferencia())
                .medioPago(guardado.getMedioPago())
                .monto(guardado.getMonto())
                .estadoPago(guardado.getEstadoPago())
                .fechaPago(guardado.getFechaPago())
                .estadoTurnoActual(turno.getEstado())
                .build();
    }

    /**
     * Cancelación de Turno por el Paciente con ventana de 24 hs (FUNC-09 / RN-05).
     */
    @Transactional
    public TurnoResponseDTO cancelarTurnoPaciente(Long turnoId) {
        Turno turno = obtenerTurnoVerificandoAcceso(turnoId);

        if (turno.getEstado() != EstadoTurno.RESERVADO && turno.getEstado() != EstadoTurno.CONFIRMADO) {
            throw new EstadoTurnoInvalidoException(
                    "Solo se pueden cancelar turnos en estado RESERVADO o CONFIRMADO. Estado actual: " + turno.getEstado(),
                    "RN-05"
            );
        }

        LocalDateTime inicioTurno = LocalDateTime.of(turno.getFecha(), turno.getHoraInicio());
        LocalDateTime ahora = LocalDateTime.now(clock);

        if (inicioTurno.isBefore(ahora)) {
            throw new EstadoTurnoInvalidoException("No es posible cancelar un turno cuya fecha y hora ya transcurrieron", "RN-05");
        }

        // Si faltan 24 horas o más: CANCELADO_PACIENTE sin penalidad
        // Si faltan menos de 24 horas: CANCELADO_PENALIZADO y +1 penalización
        boolean anticipado = !ahora.plusHours(24).isAfter(inicioTurno);

        if (anticipado) {
            turno.setEstado(EstadoTurno.CANCELADO_PACIENTE);
        } else {
            turno.setEstado(EstadoTurno.CANCELADO_PENALIZADO);
            Paciente paciente = turno.getPaciente();
            paciente.setCantidadPenalizaciones(paciente.getCantidadPenalizaciones() + 1);
        }

        Turno guardado = turnoRepository.save(turno);

        // Notificación de cancelación (RN-08)
        String detallePenalidad = anticipado ? "sin penalidad" : "con penalización por realizarse con menos de 24 hs de anticipación";
        String mensaje = String.format("El turno #%d del %s ha sido cancelado (%s).", guardado.getId(), guardado.getFecha(), detallePenalidad);
        notificacionService.enviarNotificacionResiliente(guardado, guardado.getPaciente(), TipoAlertaNotificacion.CANCELACION, mensaje);

        return mapToDTO(guardado);
    }

    /**
     * Bloqueo de Agenda del Profesional y Alerta de Reprogramación (FUNC-10 / RN-06).
     */
    @Transactional
    public CancelacionBloqueProfesionalResponseDTO cancelarBloqueProfesional(CancelacionBloqueProfesionalRequestDTO request) {
        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new EstadoTurnoInvalidoException("La hora de fin debe ser posterior a la hora de inicio", "RN-06");
        }

        // 1. Registra el bloqueo puntual en catálogo vía RestClient
        BloqueoAgendaRequestDTO bloqueoDTO = BloqueoAgendaRequestDTO.builder()
                .fecha(request.getFecha())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .motivo(request.getMotivo())
                .build();
        catalogoClient.registrarBloqueoAgenda(request.getProfesionalId(), bloqueoDTO);

        // 2. Busca turnos activos en reservas que colisionen con ese horario
        List<Turno> turnosAfectados = turnoRepository.buscarTurnosAfectadosPorBloqueo(
                request.getProfesionalId(),
                request.getFecha(),
                request.getHoraInicio(),
                request.getHoraFin()
        );

        List<Long> idsAfectados = new ArrayList<>();

        for (Turno t : turnosAfectados) {
            t.setEstado(EstadoTurno.PENDIENTE_REPROGRAMACION);
            turnoRepository.save(t);
            idsAfectados.add(t.getId());

            // 3. Notificación externa de alerta de reprogramación prioritaria (RN-08)
            String mensaje = String.format("Aviso importante: Su turno #%d para el %s ha pasado a PENDIENTE_REPROGRAMACION por ausencia médica. Dispone de prioridad para reprogramar.",
                    t.getId(), t.getFecha());
            notificacionService.enviarNotificacionResiliente(t, t.getPaciente(), TipoAlertaNotificacion.REPROGRAMACION, mensaje);
        }

        return CancelacionBloqueProfesionalResponseDTO.builder()
                .profesionalId(request.getProfesionalId())
                .fecha(request.getFecha())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .motivo(request.getMotivo())
                .cantidadTurnosAfectados(idsAfectados.size())
                .turnosAfectadosIds(idsAfectados)
                .build();
    }

    /**
     * Reprogramación Prioritaria de Turno Sin Doble Cobro (FUNC-11 / RN-07).
     */
    @Transactional
    public TurnoResponseDTO reprogramarTurno(Long turnoId, ReprogramacionTurnoRequestDTO request) {
        Turno turno = obtenerTurnoVerificandoAcceso(turnoId);

        if (turno.getEstado() != EstadoTurno.PENDIENTE_REPROGRAMACION) {
            throw new EstadoTurnoInvalidoException(
                    "Solo se pueden reprogramar turnos en estado PENDIENTE_REPROGRAMACION. Estado actual: " + turno.getEstado(),
                    "RN-07"
            );
        }

        // 1. Revalidar compatibilidad en catálogo para la nueva fecha y hora
        CompatibilidadRecursoDTO compatibilidad = catalogoClient.obtenerCompatibilidad(
                turno.getProfesionalId(),
                turno.getServicioId(),
                request.getNuevaFecha(),
                request.getNuevaHoraInicio()
        );

        LocalTime nuevaHoraFin = request.getNuevaHoraInicio().plusMinutes(compatibilidad.getDuracionMinutos());

        // 2. Revalidar no colisión horaria excluyendo este mismo turno
        validadorColisionService.validarNoColision(
                turno.getPaciente().getId(),
                compatibilidad.getProfesionalId(),
                compatibilidad.getConsultorioFijoId(),
                compatibilidad.getEquipamientosIds(),
                request.getNuevaFecha(),
                request.getNuevaHoraInicio(),
                nuevaHoraFin,
                turno.getId()
        );

        // 3. Actualizar datos del turno manteniendo el costoAcordado original
        turno.setFecha(request.getNuevaFecha());
        turno.setHoraInicio(request.getNuevaHoraInicio());
        turno.setHoraFin(nuevaHoraFin);
        turno.setConsultorioId(compatibilidad.getConsultorioFijoId());
        turno.setSedeId(compatibilidad.getSedeId());

        // Actualizar colección de equipamientos de forma segura sin colisión de UK
        if (compatibilidad.getEquipamientosIds() != null) {
            turno.getEquipamientos().removeIf(eq -> !compatibilidad.getEquipamientosIds().contains(eq.getEquipamientoId()));
            for (Long eqId : compatibilidad.getEquipamientosIds()) {
                boolean yaExiste = turno.getEquipamientos().stream().anyMatch(eq -> eq.getEquipamientoId().equals(eqId));
                if (!yaExiste) {
                    turno.agregarEquipamiento(eqId);
                }
            }
        } else {
            turno.getEquipamientos().clear();
        }

        // RN-07: Si ya estaba pagado pasa directamente a CONFIRMADO sin requerir nuevo pago
        if (Boolean.TRUE.equals(turno.getPagado())) {
            turno.setEstado(EstadoTurno.CONFIRMADO);
        } else {
            turno.setEstado(EstadoTurno.RESERVADO);
        }

        Turno guardado = turnoRepository.save(turno);

        // Notificación de reprogramación exitosa (RN-08)
        String mensaje = String.format("Su turno #%d fue reprogramado con éxito para el %s a las %s hs. Estado actual: %s.",
                guardado.getId(), guardado.getFecha(), guardado.getHoraInicio(), guardado.getEstado());
        notificacionService.enviarNotificacionResiliente(guardado, guardado.getPaciente(), TipoAlertaNotificacion.REPROGRAMACION, mensaje);

        return mapToDTO(guardado);
    }

    /**
     * Cierre de Atención Médica por el Profesional (FUNC-12).
     */
    @Transactional
    public TurnoResponseDTO cerrarAtencion(Long turnoId, CierreAtencionRequestDTO request) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado con id: " + turnoId));

        if (turno.getEstado() != EstadoTurno.CONFIRMADO) {
            throw new EstadoTurnoInvalidoException(
                    "Solo se puede registrar el cierre de atención sobre turnos CONFIRMADOS. Estado actual: " + turno.getEstado(),
                    "ESTADO_INVALIDO"
            );
        }

        if (request.getResultado() != EstadoTurno.ATENDIDO && request.getResultado() != EstadoTurno.AUSENTE) {
            throw new EstadoTurnoInvalidoException("El resultado de cierre de atención debe ser ATENDIDO o AUSENTE", "ESTADO_INVALIDO");
        }

        turno.setEstado(request.getResultado());
        Turno guardado = turnoRepository.save(turno);
        return mapToDTO(guardado);
    }

    /**
     * Consulta individual de turno con validación de roles y propiedad.
     */
    @Transactional(readOnly = true)
    public TurnoResponseDTO obtenerPorId(Long id) {
        Turno turno = obtenerTurnoVerificandoAcceso(id);
        return mapToDTO(turno);
    }

    /**
     * Listar turnos con filtros dinámicos (pacienteId, profesionalId, sedeId, fecha, estado).
     */
    @Transactional(readOnly = true)
    public List<TurnoResponseDTO> listarTurnos(Long pacienteId, Long profesionalId, Long sedeId,
                                              LocalDate fecha, EstadoTurno estado) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esPaciente = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROL_PACIENTE"));
        boolean esAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROL_ADMIN"));

        // Si es rol paciente y no es admin, restringe obligatoriamente a sus propios turnos
        if (esPaciente && !esAdmin) {
            Paciente miPerfil = pacienteService.obtenerEntidadAutenticada();
            pacienteId = miPerfil.getId();
        }

        Specification<Turno> spec = TurnoSpecifications.conFiltros(pacienteId, profesionalId, sedeId, fecha, estado);
        List<Turno> turnos = turnoRepository.findAll(spec);
        return turnos.stream().map(this::mapToDTO).toList();
    }

    private Turno obtenerTurnoVerificandoAcceso(Long turnoId) {
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Turno no encontrado con id: " + turnoId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean esAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROL_ADMIN"));
            boolean esProfesional = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROL_PROFESIONAL"));
            boolean esPaciente = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROL_PACIENTE"));

            if (esPaciente && !esAdmin && !esProfesional) {
                if (auth.getPrincipal() instanceof Jwt jwt) {
                    if (!turno.getPaciente().getKeycloakSub().equals(jwt.getSubject())) {
                        throw new AccessDeniedException("No tiene permisos para acceder o modificar un turno que pertenece a otro paciente");
                    }
                }
            }
        }

        return turno;
    }

    private Paciente resolverPacienteParaOperacion(Long requestPacienteId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROL_ADMIN"));

        if (esAdmin && requestPacienteId != null) {
            return pacienteService.obtenerEntidadPorId(requestPacienteId);
        }

        return pacienteService.obtenerEntidadAutenticada();
    }

    private TurnoResponseDTO mapToDTO(Turno t) {
        List<Long> equipamientosIds = t.getEquipamientos() != null
                ? t.getEquipamientos().stream().map(TurnoEquipamiento::getEquipamientoId).toList()
                : List.of();

        String nombreCompleto = t.getPaciente() != null
                ? t.getPaciente().getNombre() + " " + t.getPaciente().getApellido()
                : null;

        return TurnoResponseDTO.builder()
                .id(t.getId())
                .pacienteId(t.getPaciente() != null ? t.getPaciente().getId() : null)
                .pacienteNombreCompleto(nombreCompleto)
                .profesionalId(t.getProfesionalId())
                .servicioId(t.getServicioId())
                .consultorioId(t.getConsultorioId())
                .sedeId(t.getSedeId())
                .equipamientosIds(equipamientosIds)
                .fecha(t.getFecha())
                .horaInicio(t.getHoraInicio())
                .horaFin(t.getHoraFin())
                .costoAcordado(t.getCostoAcordado())
                .estado(t.getEstado())
                .pagado(t.getPagado())
                .fechaCreacion(t.getFechaCreacion())
                .fechaActualizacion(t.getFechaActualizacion())
                .build();
    }
}
