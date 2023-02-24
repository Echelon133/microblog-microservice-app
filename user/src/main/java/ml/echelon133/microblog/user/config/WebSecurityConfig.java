package ml.echelon133.microblog.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .antMatcher("/api/**")
                    .authorizeRequests()
                        .antMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/users/**").permitAll()
                        .anyRequest().denyAll();
        return http.build();
    }
}
