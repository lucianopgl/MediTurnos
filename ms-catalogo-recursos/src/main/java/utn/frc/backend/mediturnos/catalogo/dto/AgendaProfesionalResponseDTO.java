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
public class AgendaProfesionalResponseDTO {

    private Long profesionalId;
    private String profesionalNombreCompleto;
    private List<HorarioProfesionalResponseDTO> horariosSemanales;
    private List<BloqueoAgendaResponseDTO> bloqueos;
}
