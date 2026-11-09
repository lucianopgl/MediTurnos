package utn.frc.backend.mediturnos.catalogo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utn.frc.backend.mediturnos.catalogo.entity.*;
import utn.frc.backend.mediturnos.catalogo.repository.BloqueoProfesionalRepository;
import utn.frc.backend.mediturnos.catalogo.repository.HorarioProfesionalRepository;
import utn.frc.backend.mediturnos.common.dto.CompatibilidadRecursoDTO;
import utn.frc.backend.mediturnos.common.enums.DiaSemana;
import utn.frc.backend.mediturnos.common.exception.CoherenciaSedeException;
import utn.frc.backend.mediturnos.common.exception.RecursoNoDisponibleException;
import utn.frc.backend.mediturnos.common.exception.ReglaNegocioException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompatibilidadServiceTest {

    @Mock
    private ProfesionalService profesionalService;

    @Mock
    private ServicioService servicioService;

    @Mock
    private HorarioProfesionalRepository horarioProfesionalRepository;

    @Mock
    private BloqueoProfesionalRepository bloqueoProfesionalRepository;

    @InjectMocks
    private CompatibilidadService compatibilidadService;

    private Sede sedeCentro;
    private Consultorio box101;
    private Especialidad cardiologia;
    private Equipamiento ecografo;
    private Equipamiento ecg;
    private Servicio ecocardiograma;
    private Profesional drGonzalez;
    private HorarioProfesional horarioLunes;

    private final LocalDate fechaLunes = LocalDate.of(2026, 11, 16); // Lunes
    private final LocalTime horaInicio = LocalTime.of(9, 0);

    @BeforeEach
    void setUp() {
        sedeCentro = Sede.builder()
                .id(1L)
                .nombre("Sede Centro")
                .direccion("Av. Vélez Sarsfield 250")
                .activa(true)
                .build();

        box101 = Consultorio.builder()
                .id(1L)
                .sede(sedeCentro)
                .codigoNombre("Box 101")
                .operativo(true)
                .build();

        cardiologia = Especialidad.builder()
                .id(1L)
                .nombre("Cardiología")
                .activa(true)
                .build();

        ecografo = Equipamiento.builder()
                .id(1L)
                .sede(sedeCentro)
                .nombre("Ecógrafo Doppler Mindray #1")
                .codigoInventario("EQ-CEN-ECO-001")
                .operativo(true)
                .build();

        ecg = Equipamiento.builder()
                .id(2L)
                .sede(sedeCentro)
                .nombre("Electrocardiógrafo Digital #1")
                .codigoInventario("EQ-CEN-ECG-001")
                .operativo(true)
                .build();

        ecocardiograma = Servicio.builder()
                .id(2L)
                .especialidad(cardiologia)
                .nombre("Ecocardiograma Doppler Color Completo")
                .duracionMinutos(45)
                .costo(BigDecimal.valueOf(32000.00))
                .activo(true)
                .equipamientosRequeridos(Set.of(ecografo, ecg))
                .build();

        drGonzalez = Profesional.builder()
                .id(1L)
                .keycloakSub("sub-dr-gonzalez")
                .especialidad(cardiologia)
                .consultorioFijo(box101)
                .matricula("MP-34501")
                .nombre("Carlos")
                .apellido("González")
                .email("dr.gonzalez@mediturnos.com.ar")
                .activo(true)
                .serviciosHabilitados(Set.of(ecocardiograma))
                .build();

        horarioLunes = HorarioProfesional.builder()
                .id(1L)
                .profesional(drGonzalez)
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(14, 0))
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("CV-01: Caso exitoso de validación RN-01 y RN-02 con entrega de IDs de recursos")
    void testEvaluarCompatibilidad_Exito() {
        when(profesionalService.buscarEntidadPorId(1L)).thenReturn(drGonzalez);
        when(servicioService.buscarEntidadPorId(2L)).thenReturn(ecocardiograma);
        when(horarioProfesionalRepository.findByProfesionalIdAndDiaSemanaAndActivoTrue(1L, DiaSemana.LUNES))
                .thenReturn(List.of(horarioLunes));
        when(bloqueoProfesionalRepository.findSolapamientos(1L, fechaLunes, horaInicio, horaInicio.plusMinutes(45)))
                .thenReturn(Collections.emptyList());

        CompatibilidadRecursoDTO resultado = compatibilidadService.evaluarCompatibilidad(1L, 2L, fechaLunes, horaInicio);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getProfesionalId()).isEqualTo(1L);
        assertThat(resultado.getServicioId()).isEqualTo(2L);
        assertThat(resultado.getDuracionMinutos()).isEqualTo(45);
        assertThat(resultado.getCosto()).isEqualByComparingTo("32000.00");
        assertThat(resultado.getSedeId()).isEqualTo(1L);
        assertThat(resultado.getConsultorioFijoId()).isEqualTo(1L);
        assertThat(resultado.getEquipamientosIds()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("RN-01: Falla si el profesional está inactivo")
    void testEvaluarCompatibilidad_ProfesionalInactivo() {
        drGonzalez.setActivo(false);
        when(profesionalService.buscarEntidadPorId(1L)).thenReturn(drGonzalez);

        assertThatThrownBy(() -> compatibilidadService.evaluarCompatibilidad(1L, 2L, fechaLunes, horaInicio))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    @DisplayName("RN-01: Falla si el servicio no está habilitado para el profesional")
    void testEvaluarCompatibilidad_ServicioNoHabilitado() {
        drGonzalez.setServiciosHabilitados(Collections.emptySet());
        when(profesionalService.buscarEntidadPorId(1L)).thenReturn(drGonzalez);
        when(servicioService.buscarEntidadPorId(2L)).thenReturn(ecocardiograma);

        assertThatThrownBy(() -> compatibilidadService.evaluarCompatibilidad(1L, 2L, fechaLunes, horaInicio))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no está habilitado");
    }

    @Test
    @DisplayName("RN-01: Falla si la franja horaria queda fuera de la plantilla semanal")
    void testEvaluarCompatibilidad_FueraDePlantilla() {
        when(profesionalService.buscarEntidadPorId(1L)).thenReturn(drGonzalez);
        when(servicioService.buscarEntidadPorId(2L)).thenReturn(ecocardiograma);
        // Horario termina a las 14:00, pedimos a las 13:30 (13:30 + 45 min = 14:15 -> fuera)
        LocalTime horaTarde = LocalTime.of(13, 30);
        when(horarioProfesionalRepository.findByProfesionalIdAndDiaSemanaAndActivoTrue(1L, DiaSemana.LUNES))
                .thenReturn(List.of(horarioLunes));

        assertThatThrownBy(() -> compatibilidadService.evaluarCompatibilidad(1L, 2L, fechaLunes, horaTarde))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("fuera de la plantilla semanal");
    }

    @Test
    @DisplayName("RN-01: Falla si existe un bloqueo puntual de agenda en la fecha y franja (CV-06)")
    void testEvaluarCompatibilidad_BloqueoVigente() {
        when(profesionalService.buscarEntidadPorId(1L)).thenReturn(drGonzalez);
        when(servicioService.buscarEntidadPorId(2L)).thenReturn(ecocardiograma);
        when(horarioProfesionalRepository.findByProfesionalIdAndDiaSemanaAndActivoTrue(1L, DiaSemana.LUNES))
                .thenReturn(List.of(horarioLunes));

        BloqueoProfesional bloqueo = BloqueoProfesional.builder()
                .id(1L)
                .profesional(drGonzalez)
                .fecha(fechaLunes)
                .horaInicio(LocalTime.of(8, 30))
                .horaFin(LocalTime.of(10, 30))
                .motivo("Congreso de Cardiología")
                .build();

        when(bloqueoProfesionalRepository.findSolapamientos(1L, fechaLunes, horaInicio, horaInicio.plusMinutes(45)))
                .thenReturn(List.of(bloqueo));

        assertThatThrownBy(() -> compatibilidadService.evaluarCompatibilidad(1L, 2L, fechaLunes, horaInicio))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Congreso de Cardiología");
    }

    @Test
    @DisplayName("RN-02: Falla si el consultorio fijo está en mantenimiento")
    void testEvaluarCompatibilidad_ConsultorioEnMantenimiento() {
        box101.setOperativo(false);
        when(profesionalService.buscarEntidadPorId(1L)).thenReturn(drGonzalez);
        when(servicioService.buscarEntidadPorId(2L)).thenReturn(ecocardiograma);
        when(horarioProfesionalRepository.findByProfesionalIdAndDiaSemanaAndActivoTrue(1L, DiaSemana.LUNES))
                .thenReturn(List.of(horarioLunes));
        when(bloqueoProfesionalRepository.findSolapamientos(1L, fechaLunes, horaInicio, horaInicio.plusMinutes(45)))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> compatibilidadService.evaluarCompatibilidad(1L, 2L, fechaLunes, horaInicio))
                .isInstanceOf(RecursoNoDisponibleException.class)
                .hasMessageContaining("mantenimiento");
    }

    @Test
    @DisplayName("RN-02 / CV-03: Falla si un equipamiento requerido está en mantenimiento")
    void testEvaluarCompatibilidad_EquipamientoEnMantenimiento() {
        ecografo.setOperativo(false);
        when(profesionalService.buscarEntidadPorId(1L)).thenReturn(drGonzalez);
        when(servicioService.buscarEntidadPorId(2L)).thenReturn(ecocardiograma);
        when(horarioProfesionalRepository.findByProfesionalIdAndDiaSemanaAndActivoTrue(1L, DiaSemana.LUNES))
                .thenReturn(List.of(horarioLunes));
        when(bloqueoProfesionalRepository.findSolapamientos(1L, fechaLunes, horaInicio, horaInicio.plusMinutes(45)))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> compatibilidadService.evaluarCompatibilidad(1L, 2L, fechaLunes, horaInicio))
                .isInstanceOf(RecursoNoDisponibleException.class)
                .hasMessageContaining("EQ-CEN-ECO-001");
    }

    @Test
    @DisplayName("RN-02: Falla con CoherenciaSedeException si un equipamiento pertenece a otra sede")
    void testEvaluarCompatibilidad_EquipamientoDistintaSede() {
        Sede sedeCerro = Sede.builder().id(2L).nombre("Sede Cerro").activa(true).build();
        ecografo.setSede(sedeCerro);

        when(profesionalService.buscarEntidadPorId(1L)).thenReturn(drGonzalez);
        when(servicioService.buscarEntidadPorId(2L)).thenReturn(ecocardiograma);
        when(horarioProfesionalRepository.findByProfesionalIdAndDiaSemanaAndActivoTrue(1L, DiaSemana.LUNES))
                .thenReturn(List.of(horarioLunes));
        when(bloqueoProfesionalRepository.findSolapamientos(1L, fechaLunes, horaInicio, horaInicio.plusMinutes(45)))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> compatibilidadService.evaluarCompatibilidad(1L, 2L, fechaLunes, horaInicio))
                .isInstanceOf(CoherenciaSedeException.class)
                .hasMessageContaining("Incoherencia física");
    }
}
