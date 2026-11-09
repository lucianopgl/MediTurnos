package utn.frc.backend.mediturnos.catalogo.config;

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

                        // Sedes
                        .requestMatchers(HttpMethod.POST, "/api/catalogo/sedes").hasAuthority("ROL_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/sedes").authenticated()

                        // Consultorios
                        .requestMatchers(HttpMethod.POST, "/api/catalogo/consultorios").hasAuthority("ROL_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/catalogo/consultorios/*/estado-operativo").hasAuthority("ROL_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/consultorios").hasAnyAuthority("ROL_ADMIN", "ROL_PROFESIONAL")

                        // Equipamientos
                        .requestMatchers(HttpMethod.POST, "/api/catalogo/equipamientos").hasAuthority("ROL_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/catalogo/equipamientos/*/estado-operativo").hasAuthority("ROL_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/equipamientos").hasAnyAuthority("ROL_ADMIN", "ROL_PROFESIONAL")

                        // Especialidades
                        .requestMatchers(HttpMethod.POST, "/api/catalogo/especialidades").hasAuthority("ROL_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/especialidades").authenticated()

                        // Servicios Médicos
                        .requestMatchers(HttpMethod.POST, "/api/catalogo/servicios").hasAuthority("ROL_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/servicios").authenticated()

                        // Profesionales
                        .requestMatchers(HttpMethod.POST, "/api/catalogo/profesionales").hasAuthority("ROL_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/profesionales").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/catalogo/profesionales/*/servicios").hasAuthority("ROL_ADMIN")

                        // Horarios y Bloqueos de Profesionales
                        .requestMatchers(HttpMethod.POST, "/api/catalogo/profesionales/*/horarios").hasAnyAuthority("ROL_ADMIN", "ROL_PROFESIONAL")
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/profesionales/*/horarios").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/catalogo/profesionales/*/bloqueos").hasAnyAuthority("ROL_ADMIN", "ROL_PROFESIONAL")

                        // Compatibilidad de Recursos (RN-01 y RN-02 para ms-reservas y clientes autorizados)
                        .requestMatchers(HttpMethod.GET, "/api/catalogo/compatibilidad").authenticated()

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
            Collection<GrantedAuthority> authorities = extractRealmRoles(jwt);
            return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("preferred_username"));
        };
    }

    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            for (Object roleObj : roles) {
                if (roleObj instanceof String role) {
                    authorities.add(new SimpleGrantedAuthority(role));
                    if (role.startsWith("ROL_")) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.substring(4)));
                    } else {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }
                }
            }
        }
        return authorities;
    }
}
