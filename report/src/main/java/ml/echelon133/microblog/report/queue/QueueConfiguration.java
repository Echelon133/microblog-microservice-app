package ml.echelon133.microblog.report.queue;

import ml.echelon133.microblog.report.repository.ReportRepository;
import ml.echelon133.microblog.shared.queue.QueueTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class QueueConfiguration {

    @Value("${spring.redis.host}")
    String host;

    @Value("${spring.redis.password}")
    String password;

    private ReportRepository reportRepository;

    @Autowired
    public QueueConfiguration(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host);
        config.setPassword(RedisPassword.of(password));
        return new JedisConnectionFactory(config);
    }

    @Bean
    MessageListenerAdapter messageListener() {
        return new MessageListenerAdapter(new ReportMessageListener(reportRepository));
    }

    @Bean
    RedisMessageListenerContainer redisContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(jedisConnectionFactory());
        container.addMessageListener(messageListener(), QueueTopic.REPORT);
        return container;
    }
}

