package ml.echelon133.microblog.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {

    @Bean
    @Order
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .authorizeRequests()
                    .mvcMatchers("/actuator/health/**").permitAll()
                    .mvcMatchers("/login").permitAll()
                    .anyRequest().denyAll()
                .and()
                // disable csrf because logins will be performed through API calls
                .csrf().disable()
                // oauth2 requires username+password during Authorization Code with PKCE flow
                .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
