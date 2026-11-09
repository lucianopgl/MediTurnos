package utn.frc.backend.mediturnos.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipamientoRequestDTO {

    @NotNull(message = "El identificador de la sede es obligatorio")
    private Long sedeId;

    @NotBlank(message = "El nombre del equipamiento es obligatorio")
    private String nombre;

    @NotBlank(message = "El código de inventario es obligatorio")
    private String codigoInventario;

    @Builder.Default
    private Boolean operativo = true;
}
