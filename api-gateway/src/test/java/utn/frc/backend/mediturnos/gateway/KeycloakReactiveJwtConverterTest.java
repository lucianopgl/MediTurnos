package utn.frc.backend.mediturnos.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import utn.frc.backend.mediturnos.gateway.config.KeycloakReactiveJwtConverter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakReactiveJwtConverterTest {

    private final KeycloakReactiveJwtConverter converter = new KeycloakReactiveJwtConverter();

    @Test
    @DisplayName("Convierte Jwt con roles de realm y cliente a GrantedAuthority ROL_* y ROLE_*")
    void givenJwtWithRealmAndClientRoles_whenConvert_thenExtractsAllAuthorities() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject("usr-uuid-1234")
                .claim("preferred_username", "juan_paciente")
                .claim("realm_access", Map.of("roles", List.of("ROL_PACIENTE", "offline_access")))
                .claim("resource_access", Map.of(
                        "mediturnos-backend", Map.of("roles", List.of("ROL_ADMIN"))
                ))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt).block();

        assertThat(token).isNotNull();
        assertThat(token.getName()).isEqualTo("juan_paciente");

        Set<String> authorities = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorities).contains(
                "ROL_PACIENTE", "ROLE_PACIENTE",
                "ROL_ADMIN", "ROLE_ADMIN",
                "offline_access", "ROLE_offline_access"
        );
    }

    @Test
    @DisplayName("Fallback a subject como principal name cuando preferred_username no está presente")
    void givenJwtWithoutPreferredUsername_whenConvert_thenUsesSubject() {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject("fallback-sub-uuid")
                .claim("realm_access", Map.of("roles", List.of("ROL_PROFESIONAL")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt).block();

        assertThat(token).isNotNull();
        assertThat(token.getName()).isEqualTo("fallback-sub-uuid");
        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROL_PROFESIONAL", "ROLE_PROFESIONAL");
    }
}
