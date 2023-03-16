package ml.echelon133.microblog.user.config;

import ml.echelon133.microblog.shared.auth.MultiAuthorizationManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static ml.echelon133.microblog.shared.scope.MicroblogScope.*;

@Configuration
public class OAuth2ResourceServerConfig {

    @Value("${spring.security.oauth2.resourceserver.opaque.introspection-uri}")
    String introspectionUri;

    @Value("${spring.security.oauth2.resourceserver.opaque.introspection-client-id}")
    String clientId;

    @Value("${spring.security.oauth2.resourceserver.opaque.introspection-client-secret}")
    String clientSecret;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain resourceSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .antMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                        .antMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/users/me").hasAuthority(prefix(USER_READ))
                        .antMatchers(HttpMethod.PATCH, "/api/users/me").access(
                                MultiAuthorizationManager.hasAll(
                                        AuthorityAuthorizationManager.hasAuthority(prefix(USER_READ)),
                                        AuthorityAuthorizationManager.hasAuthority(prefix(USER_WRITE))
                                )
                        )
                        .antMatchers(HttpMethod.GET, "/api/users/*/follow").access(
                                MultiAuthorizationManager.hasAll(
                                        AuthorityAuthorizationManager.hasAuthority(prefix(USER_READ)),
                                        AuthorityAuthorizationManager.hasAuthority(prefix(FOLLOW_READ))
                                )
                        )
                        .antMatchers(HttpMethod.POST, "/api/users/*/follow").access(
                                MultiAuthorizationManager.hasAll(
                                        AuthorityAuthorizationManager.hasAuthority(prefix(USER_READ)),
                                        AuthorityAuthorizationManager.hasAuthority(prefix(FOLLOW_READ)),
                                        AuthorityAuthorizationManager.hasAuthority(prefix(FOLLOW_WRITE))
                                )
                        )
                        .antMatchers(HttpMethod.DELETE, "/api/users/*/follow").access(
                                MultiAuthorizationManager.hasAll(
                                        AuthorityAuthorizationManager.hasAuthority(prefix(USER_READ)),
                                        AuthorityAuthorizationManager.hasAuthority(prefix(FOLLOW_READ)),
                                        AuthorityAuthorizationManager.hasAuthority(prefix(FOLLOW_WRITE))
                                )
                        )
                        .antMatchers(HttpMethod.GET, "/api/users/*/profile-counters").access(
                                MultiAuthorizationManager.hasAll(
                                        AuthorityAuthorizationManager.hasAuthority(prefix(USER_READ)),
                                        AuthorityAuthorizationManager.hasAuthority(prefix(FOLLOW_READ))
                                )
                        )
                        .antMatchers(HttpMethod.GET, "/api/users").hasAuthority(prefix(USER_READ))
                        .antMatchers(HttpMethod.GET, "/api/users/*").hasAuthority(prefix(USER_READ))
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer((oauth2) -> oauth2
                        .opaqueToken((opaque) -> opaque
                                .introspectionUri(this.introspectionUri)
                                .introspectionClientCredentials(this.clientId, this.clientSecret)
                        )
                );
        return http.build();
    }
}
