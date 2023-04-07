package ml.echelon133.microblog.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.support.PageJacksonModule;
import org.springframework.cloud.openfeign.support.SortJacksonModule;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;

@SpringBootApplication
@EntityScan({"ml.echelon133.microblog.shared.post", "ml.echelon133.microblog.shared.user.follow"})
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableFeignClients
public class PostApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostApplication.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModules(
                new PageJacksonModule(),
                new SortJacksonModule()
        );
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
