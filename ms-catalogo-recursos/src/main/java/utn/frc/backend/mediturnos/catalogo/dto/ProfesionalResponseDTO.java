package utn.frc.backend.mediturnos.catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfesionalResponseDTO {

    private Long id;
    private String keycloakSub;
    private Long especialidadId;
    private String especialidadNombre;
    private Long consultorioFijoId;
    private String consultorioCodigo;
    private Long sedeId;
    private String sedeNombre;
    private String matricula;
    private String nombre;
    private String apellido;
    private String email;
    private Boolean activo;
    private List<Long> serviciosHabilitadosIds;
}
