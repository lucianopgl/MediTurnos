package utn.frc.backend.mediturnos.gateway.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Conversor reactivo de tokens JWT emitidos por Keycloak para Spring Cloud Gateway / WebFlux.
 * Extrae roles a nivel realm (realm_access.roles) y a nivel cliente (resource_access.*.roles),
 * mapeándolos a GrantedAuthority (ROL_ADMIN, ROL_PROFESIONAL, ROL_PACIENTE).
 */
@Component
public class KeycloakReactiveJwtConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        return Mono.fromCallable(() -> {
            Collection<GrantedAuthority> authorities = extractRoles(jwt);
            String principalName = jwt.getClaimAsString("preferred_username");
            if (principalName == null || principalName.isBlank()) {
                principalName = jwt.getSubject();
            }
            return new JwtAuthenticationToken(jwt, authorities, principalName);
        });
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Roles del Realm (realm_access.roles)
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            for (Object roleObj : roles) {
                if (roleObj instanceof String role) {
                    authorities.add(new SimpleGrantedAuthority(role));
                    if (role.startsWith("ROL_")) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.substring(4)));
                    } else if (!role.startsWith("ROLE_")) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }
                }
            }
        }

        // 2. Roles de Cliente (resource_access.*.roles)
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            for (Object clientObj : resourceAccess.values()) {
                if (clientObj instanceof Map<?, ?> clientMap && clientMap.get("roles") instanceof Collection<?> clientRoles) {
                    for (Object roleObj : clientRoles) {
                        if (roleObj instanceof String role) {
                            authorities.add(new SimpleGrantedAuthority(role));
                            if (role.startsWith("ROL_")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.substring(4)));
                            } else if (!role.startsWith("ROLE_")) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                            }
                        }
                    }
                }
            }
        }

        return authorities;
    }
}
