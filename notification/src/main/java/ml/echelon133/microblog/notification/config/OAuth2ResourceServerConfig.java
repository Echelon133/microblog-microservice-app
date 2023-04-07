package ml.echelon133.microblog.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static ml.echelon133.microblog.shared.auth.MultiAuthorizationManager.hasAll;
import static ml.echelon133.microblog.shared.scope.MicroblogScope.*;
import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAuthority;

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
                .csrf().disable()
                .authorizeHttpRequests((authorize) -> authorize
                        .antMatchers(HttpMethod.POST, "/api/notifications/read-all").access(hasAll(
                                hasAuthority(prefix(NOTIFICATION_READ)), hasAuthority(prefix(NOTIFICATION_WRITE)))
                        )
                        .antMatchers(HttpMethod.POST, "/api/notifications/*/read").access(hasAll(
                                hasAuthority(prefix(NOTIFICATION_READ)), hasAuthority(prefix(NOTIFICATION_WRITE)))
                        )
                        .antMatchers(HttpMethod.GET, "/api/notifications/unread-counter").hasAuthority(prefix(NOTIFICATION_READ))
                        .antMatchers(HttpMethod.GET, "/api/notifications*").hasAuthority(prefix(NOTIFICATION_READ))
                        .antMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .anyRequest().denyAll())
                .oauth2ResourceServer((oauth2) -> oauth2
                        .opaqueToken((opaque) -> opaque
                                .introspectionUri(this.introspectionUri)
                                .introspectionClientCredentials(this.clientId, this.clientSecret)
                        )
                );
        return http.build();
    }
}
