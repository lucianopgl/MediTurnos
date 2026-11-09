package utn.frc.backend.mediturnos.catalogo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioRequestDTO {

    @NotNull(message = "El identificador de la especialidad es obligatorio")
    private Long especialidadId;

    @NotBlank(message = "El nombre del servicio es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "La duración en minutos es obligatoria")
    @Positive(message = "La duración en minutos debe ser mayor a cero")
    private Integer duracionMinutos;

    @NotNull(message = "El costo del servicio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El costo no puede ser negativo")
    private BigDecimal costo;

    @Builder.Default
    private Boolean activo = true;

    private List<Long> equipamientosIds;
}
