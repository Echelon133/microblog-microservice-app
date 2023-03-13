package ml.echelon133.microblog.shared.auth.test;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class OAuth2TestPrincipal {

    public static class Builder {
        private List<String> scopes;
        private String tokenOwnerId;
        private String username;

        public Builder setScopes(List<String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public Builder setTokenOwnerId(String id) {
            this.tokenOwnerId = id;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public OAuth2IntrospectionAuthenticatedPrincipal build() {
            String username = "testuser";
            String tokenOwnerId = UUID.randomUUID().toString();
            List<String> scopes = List.of();

            if (this.username != null) {
                username = this.username;
            }
            if (this.tokenOwnerId != null) {
                tokenOwnerId = this.tokenOwnerId;
            }
            if (this.scopes != null) {
                scopes = this.scopes;
            }

            var issuedAt = Instant.now();
            var expiresAt = issuedAt.plus(3, ChronoUnit.HOURS);

            var attributes = Map.of(
                    "active", true,
                    "sub", username,
                    "iat", issuedAt,
                    "exp", expiresAt,
                    "client_id", "test-client",
                    "token_type", "Bearer",
                    "token-owner-id", tokenOwnerId,
                    "scope", scopes
            );

            List<GrantedAuthority> authorities = scopes.stream().map(
                    scope -> new SimpleGrantedAuthority("SCOPE_" + scope)
            ).collect(Collectors.toList());

            return new OAuth2IntrospectionAuthenticatedPrincipal(username, attributes, authorities);
        }
    }
}
