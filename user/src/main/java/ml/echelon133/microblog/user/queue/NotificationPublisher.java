package ml.echelon133.microblog.user.queue;

import ml.echelon133.microblog.shared.notification.NotificationCreationDto;
import ml.echelon133.microblog.shared.queue.QueueTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public NotificationPublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publishNotification(NotificationCreationDto dto) {
        redisTemplate.convertAndSend(QueueTopic.NOTIFICATION.getTopic(), dto);
    }
}