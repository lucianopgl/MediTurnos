package utn.frc.backend.mediturnos.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuración reactiva de seguridad perimetral para Spring Cloud Gateway (SEG-01 / ARQ-01).
 * Implementa el perímetro exterior de la Trusted Zone, validando tokens JWT contra Keycloak
 * y aplicando la matriz completa de roles (ROL_ADMIN, ROL_PROFESIONAL, ROL_PACIENTE)
 * comprometida en el cuestionario de la cátedra y la Sección 6 del README.md.
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class ReactiveSecurityConfig {

    private final KeycloakReactiveJwtConverter keycloakReactiveJwtConverter;
    private final ReactiveSecurityExceptionHandler reactiveSecurityExceptionHandler;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(reactiveSecurityExceptionHandler)
                        .accessDeniedHandler(reactiveSecurityExceptionHandler)
                )
                .authorizeExchange(exchanges -> exchanges
                        // 1. Diagnóstico y Documentación Pública (Actuator y OpenAPI/Swagger)
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/api/catalogo/v3/api-docs/**",
                                "/api/reservas/v3/api-docs/**"
                        ).permitAll()

                        // 2. Matriz Catálogo (/api/catalogo/** - Sección 6.1 README)
                        .pathMatchers(HttpMethod.POST, "/api/catalogo/sedes").hasAuthority("ROL_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/catalogo/sedes").authenticated()
                        .pathMatchers(HttpMethod.POST, "/api/catalogo/consultorios").hasAuthority("ROL_ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/catalogo/consultorios/*/estado-operativo").hasAuthority("ROL_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/catalogo/consultorios").hasAnyAuthority("ROL_ADMIN", "ROL_PROFESIONAL")
                        .pathMatchers(HttpMethod.POST, "/api/catalogo/equipamientos").hasAuthority("ROL_ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/catalogo/equipamientos/*/estado-operativo").hasAuthority("ROL_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/catalogo/equipamientos").hasAnyAuthority("ROL_ADMIN", "ROL_PROFESIONAL")
                        .pathMatchers(HttpMethod.POST, "/api/catalogo/especialidades").hasAuthority("ROL_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/catalogo/especialidades").authenticated()
                        .pathMatchers(HttpMethod.POST, "/api/catalogo/servicios").hasAuthority("ROL_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/catalogo/servicios").authenticated()
                        .pathMatchers(HttpMethod.POST, "/api/catalogo/profesionales").hasAuthority("ROL_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/catalogo/profesionales").authenticated()
                        .pathMatchers(HttpMethod.PUT, "/api/catalogo/profesionales/*/servicios").hasAuthority("ROL_ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/catalogo/profesionales/*/horarios").hasAnyAuthority("ROL_ADMIN", "ROL_PROFESIONAL")
                        .pathMatchers(HttpMethod.GET, "/api/catalogo/profesionales/*/horarios").authenticated()
                        .pathMatchers(HttpMethod.POST, "/api/catalogo/profesionales/*/bloqueos").hasAnyAuthority("ROL_ADMIN", "ROL_PROFESIONAL")
                        .pathMatchers(HttpMethod.GET, "/api/catalogo/compatibilidad").authenticated()

                        // 3. Matriz Reservas (/api/reservas/** - Sección 6.2 README)
                        .pathMatchers(HttpMethod.POST, "/api/reservas/pacientes").hasAnyAuthority("ROL_PACIENTE", "ROL_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/reservas/pacientes/mi-perfil").hasAuthority("ROL_PACIENTE")
                        .pathMatchers(HttpMethod.GET, "/api/reservas/pacientes/*").hasAnyAuthority("ROL_ADMIN", "ROL_PROFESIONAL")
                        .pathMatchers(HttpMethod.POST, "/api/reservas/turnos").hasAuthority("ROL_PACIENTE")
                        .pathMatchers(HttpMethod.GET, "/api/reservas/turnos/*").hasAnyAuthority("ROL_PACIENTE", "ROL_PROFESIONAL", "ROL_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/reservas/turnos").hasAnyAuthority("ROL_PACIENTE", "ROL_PROFESIONAL", "ROL_ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/reservas/turnos/*/pagos").hasAnyAuthority("ROL_PACIENTE", "ROL_ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/reservas/turnos/*/cancelacion").hasAuthority("ROL_PACIENTE")
                        .pathMatchers(HttpMethod.POST, "/api/reservas/turnos/cancelacion-bloque-profesional").hasAnyAuthority("ROL_PROFESIONAL", "ROL_ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/reservas/turnos/*/reprogramacion").hasAuthority("ROL_PACIENTE")
                        .pathMatchers(HttpMethod.PATCH, "/api/reservas/turnos/*/cierre-atencion").hasAnyAuthority("ROL_PROFESIONAL", "ROL_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/reservas/notificaciones").hasAuthority("ROL_ADMIN")

                        // 4. Cualquier otra ruta no especificada requiere estar autenticada
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakReactiveJwtConverter))
                        .authenticationEntryPoint(reactiveSecurityExceptionHandler)
                        .accessDeniedHandler(reactiveSecurityExceptionHandler)
                )
                .build();
    }
}
