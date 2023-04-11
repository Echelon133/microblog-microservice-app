package ml.echelon133.microblog.report.queue;

import ml.echelon133.microblog.shared.queue.QueueTopic;
import ml.echelon133.microblog.shared.report.ReportActionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReportActionPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public ReportActionPublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publishReportAction(ReportActionDto dto) {
        redisTemplate.convertAndSend(QueueTopic.REPORT_ACTION.getTopic(), dto);
    }
}
