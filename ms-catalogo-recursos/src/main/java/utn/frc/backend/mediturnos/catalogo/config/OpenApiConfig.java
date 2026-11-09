package utn.frc.backend.mediturnos.catalogo.config;

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
    public OpenAPI catalogoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediTurnos API — ms-catalogo-recursos")
                        .version("1.0.0")
                        .description("Microservicio de Catálogo de Recursos Físicos y Médicos (Puerto 8081). "
                                + "Valida reglas RN-01 y RN-02 para el flujo de reservas y compatibilidad tripartita.")
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
