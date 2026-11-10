package utn.frc.backend.mediturnos.reservas.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomSecurityExceptionHandler customSecurityExceptionHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customSecurityExceptionHandler)
                        .accessDeniedHandler(customSecurityExceptionHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos de diagnóstico y documentación
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Pacientes
                        .requestMatchers(HttpMethod.POST, "/api/reservas/pacientes").hasAnyAuthority("ROL_PACIENTE", "ROL_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/pacientes/mi-perfil").hasAuthority("ROL_PACIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/pacientes/*").hasAnyAuthority("ROL_ADMIN", "ROL_PROFESIONAL")

                        // Turnos
                        .requestMatchers(HttpMethod.POST, "/api/reservas/turnos").hasAuthority("ROL_PACIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/turnos/*").hasAnyAuthority("ROL_PACIENTE", "ROL_PROFESIONAL", "ROL_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/turnos").hasAnyAuthority("ROL_PACIENTE", "ROL_PROFESIONAL", "ROL_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservas/turnos/*/pagos").hasAnyAuthority("ROL_PACIENTE", "ROL_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/reservas/turnos/*/cancelacion").hasAuthority("ROL_PACIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/reservas/turnos/cancelacion-bloque-profesional").hasAnyAuthority("ROL_PROFESIONAL", "ROL_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservas/turnos/*/reprogramacion").hasAuthority("ROL_PACIENTE")
                        .requestMatchers(HttpMethod.PATCH, "/api/reservas/turnos/*/cierre-atencion").hasAnyAuthority("ROL_PROFESIONAL", "ROL_ADMIN")

                        // Notificaciones (Auditoría)
                        .requestMatchers(HttpMethod.GET, "/api/reservas/notificaciones").hasAuthority("ROL_ADMIN")

                        // Cualquier otra petición debe estar autenticada
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter()))
                        .authenticationEntryPoint(customSecurityExceptionHandler)
                        .accessDeniedHandler(customSecurityExceptionHandler)
                );

        return http.build();
    }

    private Converter<Jwt, AbstractAuthenticationToken> keycloakJwtConverter() {
        return jwt -> {
            Collection<GrantedAuthority> authorities = extractKeycloakRoles(jwt);
            return new JwtAuthenticationToken(jwt, authorities);
        };
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Roles a nivel realm (realm_access.roles)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : role)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        // 2. Roles a nivel cliente (resource_access.*.roles)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(resource -> {
                if (resource instanceof Map<?, ?> resMap && resMap.containsKey("roles")) {
                    Collection<String> clientRoles = (Collection<String>) resMap.get("roles");
                    clientRoles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                }
            });
        }

        return authorities;
    }
}
