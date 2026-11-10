package utn.frc.backend.mediturnos.reservas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "KeycloakBearerAuth";

    @Bean
    public OpenAPI reservasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediTurnos API — ms-reservas")
                        .version("1.0.0")
                        .description("Microservicio de Reservas, Turnos, Pacientes y Pagos (Puerto 8082). "
                                + "Gestiona el ciclo de vida transaccional del turno, validación de no colisión tripartita (RN-03), "
                                + "confirmación por cobro (RN-04), ventana de 24 hs (RN-05), reprogramación prioritaria (RN-07) "
                                + "y notificaciones externas con resiliencia (RN-08).")
                        .contact(new Contact().name("Grupo 06 - 3K4 - UTN FRC").email("lupaglino@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
