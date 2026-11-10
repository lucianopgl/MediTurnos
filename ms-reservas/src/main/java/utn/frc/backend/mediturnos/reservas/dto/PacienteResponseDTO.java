package utn.frc.backend.mediturnos.reservas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteResponseDTO {

    private Long id;
    private String keycloakSub;
    private String dni;
    private String nombre;
    private String apellido;
    private String email;
    private String telefonoWhatsapp;
    private Integer cantidadPenalizaciones;
    private Boolean activo;
    private LocalDateTime fechaAlta;
}
