package utn.frc.backend.mediturnos.reservas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import utn.frc.backend.mediturnos.common.enums.CanalNotificacion;
import utn.frc.backend.mediturnos.common.enums.EstadoEnvioNotificacion;
import utn.frc.backend.mediturnos.common.enums.TipoAlertaNotificacion;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones", schema = "schema_reservas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_id")
    private Turno turno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CanalNotificacion canal;

    @Column(nullable = false, length = 150)
    private String destinatario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alerta", nullable = false, length = 50)
    private TipoAlertaNotificacion tipoAlerta;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false, length = 30)
    private EstadoEnvioNotificacion estadoEnvio;

    @Column(name = "detalle_mensaje", columnDefinition = "TEXT")
    private String detalleMensaje;

    @CreationTimestamp
    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio;
}
