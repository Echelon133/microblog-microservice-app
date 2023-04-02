package ml.echelon133.microblog.user.queue;

import ml.echelon133.microblog.shared.queue.QueueTopic;
import ml.echelon133.microblog.shared.user.follow.FollowInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FollowPublisher {

    private final RedisTemplate<String, Object> followsTemplate;

    @Autowired
    public FollowPublisher(RedisTemplate<String, Object> followsTemplate) {
        this.followsTemplate = followsTemplate;
    }

    public void publishCreateFollowEvent(FollowInfoDto dto) {
        followsTemplate.convertAndSend(QueueTopic.FOLLOW.getTopic(), dto);
    }

    public void publishRemoveFollowEvent(FollowInfoDto dto) {
        followsTemplate.convertAndSend(QueueTopic.UNFOLLOW.getTopic(), dto);
    }
}
