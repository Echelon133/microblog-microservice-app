package ml.echelon133.microblog.notification;

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

@SpringBootApplication
@EntityScan("ml.echelon133.microblog.shared.notification")
@EnableJpaAuditing
@EnableDiscoveryClient
@EnableFeignClients
public class NotificationApplication {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModules(
                new PageJacksonModule(),
                new SortJacksonModule()
        );
    }

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
