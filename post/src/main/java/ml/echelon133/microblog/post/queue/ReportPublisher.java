package ml.echelon133.microblog.post.queue;

import ml.echelon133.microblog.shared.queue.QueueTopic;
import ml.echelon133.microblog.shared.report.ReportCreationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReportPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public ReportPublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publishReport(ReportCreationDto dto) {
        redisTemplate.convertAndSend(QueueTopic.REPORT.getTopic(), dto);
    }
}
