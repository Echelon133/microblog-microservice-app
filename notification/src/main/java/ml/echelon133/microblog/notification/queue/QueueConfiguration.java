package ml.echelon133.microblog.notification.queue;

import ml.echelon133.microblog.notification.repository.NotificationRepository;
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

    private NotificationRepository notificationRepository;

    @Autowired
    public QueueConfiguration(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host);
        config.setPassword(RedisPassword.of(password));
        return new JedisConnectionFactory(config);
    }

    @Bean
    MessageListenerAdapter messageListener() {
        return new MessageListenerAdapter(new NotificationMessageListener(notificationRepository));
    }

    @Bean
    RedisMessageListenerContainer redisContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(jedisConnectionFactory());
        container.addMessageListener(messageListener(), QueueTopic.NOTIFICATION);
        return container;
    }
}
