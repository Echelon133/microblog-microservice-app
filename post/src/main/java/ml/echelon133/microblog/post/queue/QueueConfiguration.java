package ml.echelon133.microblog.post.queue;

import ml.echelon133.microblog.post.repository.FollowRepository;
import ml.echelon133.microblog.post.repository.PostRepository;
import ml.echelon133.microblog.shared.queue.QueueTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class QueueConfiguration {

    @Value("${spring.redis.host}")
    String host;

    @Value("${spring.redis.password}")
    String password;

    private FollowRepository followRepository;
    private PostRepository postRepository;

    @Autowired
    public QueueConfiguration(FollowRepository followRepository, PostRepository postRepository) {
        this.followRepository = followRepository;
        this.postRepository = postRepository;
    }

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host);
        config.setPassword(RedisPassword.of(password));
        return new JedisConnectionFactory(config);
    }

    @Bean
    MessageListenerAdapter followMessageListener() {
        return new MessageListenerAdapter(new FollowMessageListener(followRepository));
    }

    @Bean
    MessageListenerAdapter reportActionMessageListener() {
        return new MessageListenerAdapter(new ReportActionMessageListener(postRepository));
    }

    @Bean
    RedisMessageListenerContainer redisContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(jedisConnectionFactory());
        container.addMessageListener(followMessageListener(), QueueTopic.FOLLOW);
        container.addMessageListener(followMessageListener(), QueueTopic.UNFOLLOW);
        container.addMessageListener(reportActionMessageListener(), QueueTopic.REPORT_ACTION);
        return container;
    }

    @Bean
    RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());
        return template;
    }
}
