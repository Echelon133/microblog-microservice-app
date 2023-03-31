package ml.echelon133.microblog.post.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static ml.echelon133.microblog.shared.scope.MicroblogScope.*;
import static ml.echelon133.microblog.shared.auth.MultiAuthorizationManager.hasAll;
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
                        .antMatchers(HttpMethod.GET, "/api/feed").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/tags/*/posts").hasAuthority(prefix(POST_READ))
                        .antMatchers(HttpMethod.GET, "/api/tags/popular").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/posts/*/post-counters").hasAuthority(prefix(POST_READ))
                        .antMatchers(HttpMethod.GET, "/api/posts/*/quotes").hasAuthority(prefix(POST_READ))
                        .antMatchers(HttpMethod.GET, "/api/posts/*/responses").hasAuthority(prefix(POST_READ))
                        .antMatchers(HttpMethod.POST, "/api/posts/*/quotes").access(hasAll(
                                hasAuthority(prefix(POST_READ)), hasAuthority(prefix(POST_WRITE)))
                        )
                        .antMatchers(HttpMethod.POST, "/api/posts/*/responses").access(hasAll(
                                hasAuthority(prefix(POST_READ)), hasAuthority(prefix(POST_WRITE)))
                        )
                        .antMatchers(HttpMethod.GET, "/api/posts/*/like").access(hasAll(
                                hasAuthority(prefix(POST_READ)), hasAuthority(prefix(LIKE_READ)))
                        )
                        .antMatchers(HttpMethod.POST, "/api/posts/*/like").access(hasAll(
                                hasAuthority(prefix(POST_READ)),
                                hasAuthority(prefix(LIKE_READ)),
                                hasAuthority(prefix(LIKE_WRITE)))
                        )
                        .antMatchers(HttpMethod.DELETE, "/api/posts/*/like").access(hasAll(
                                hasAuthority(prefix(POST_READ)),
                                hasAuthority(prefix(LIKE_READ)),
                                hasAuthority(prefix(LIKE_WRITE)))
                        )
                        .antMatchers(HttpMethod.GET, "/api/posts/*").hasAuthority(prefix(POST_READ))
                        .antMatchers(HttpMethod.DELETE, "/api/posts/*").access(hasAll(
                                hasAuthority(prefix(POST_READ)), hasAuthority(prefix(POST_WRITE))
                        ))
                        .antMatchers(HttpMethod.GET, "/api/posts").hasAuthority(prefix(POST_READ))
                        .antMatchers(HttpMethod.POST, "/api/posts").hasAuthority(prefix(POST_WRITE))
                        .antMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .anyRequest().permitAll())
                .oauth2ResourceServer((oauth2) -> oauth2
                        .opaqueToken((opaque) -> opaque
                                .introspectionUri(this.introspectionUri)
                                .introspectionClientCredentials(this.clientId, this.clientSecret)
                        )
                );
        return http.build();
    }
}
