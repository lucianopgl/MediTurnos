package utn.frc.backend.mediturnos.catalogo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfesionalRequestDTO {

    @NotBlank(message = "El keycloak_sub es obligatorio")
    private String keycloakSub;

    @NotNull(message = "El identificador de especialidad es obligatorio")
    private Long especialidadId;

    @NotNull(message = "El identificador de consultorio fijo es obligatorio")
    private Long consultorioFijoId;

    @NotBlank(message = "La matrícula profesional es obligatoria")
    private String matricula;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser una dirección válida")
    private String email;

    @Builder.Default
    private Boolean activo = true;

    private List<Long> serviciosIds;
}
