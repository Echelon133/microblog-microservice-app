package ml.echelon133.microblog.report.queue;

import ml.echelon133.microblog.shared.queue.QueueTopic;
import ml.echelon133.microblog.shared.report.ReportActionDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes report-action messages to a redis queue.
 */
@Service
public class ReportActionPublisher {

    private static final Logger LOGGER = LogManager.getLogger(ReportActionPublisher.class);
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public ReportActionPublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Publishes messages containing information about report-action taken against reported posts.
     * @param dto contains information required to delete a post because of an accepted report
     */
    public void publishReportAction(ReportActionDto dto) {
        LOGGER.debug(String.format(
                "Publishing a report-action: post '%s' to be deleted for reason '%s'",
                dto.getPostToDelete(), dto.getReason()
        ));
        redisTemplate.convertAndSend(QueueTopic.REPORT_ACTION.getTopic(), dto);
    }
}
