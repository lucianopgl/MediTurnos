package utn.frc.backend.mediturnos.reservas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoRequestDTO {

    @NotBlank(message = "El comprobante de referencia es obligatorio")
    private String comprobanteReferencia;

    @NotBlank(message = "El medio de pago es obligatorio")
    private String medioPago;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser positivo")
    private BigDecimal monto;
}
