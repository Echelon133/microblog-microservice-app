package ml.echelon133.microblog.user.queue;

import ml.echelon133.microblog.shared.queue.QueueTopic;
import ml.echelon133.microblog.shared.user.follow.FollowInfoDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes follow-related messages to a redis queue.
 */
@Service
public class FollowPublisher {

    private static final Logger LOGGER = LogManager.getLogger(FollowPublisher.class);
    private final RedisTemplate<String, Object> followsTemplate;

    @Autowired
    public FollowPublisher(RedisTemplate<String, Object> followsTemplate) {
        this.followsTemplate = followsTemplate;
    }

    /**
     * Publishes messages containing information about users following other users.
     * @param dto contains information about who wants to follow whom
     */
    public void publishFollow(FollowInfoDto dto) {
        LOGGER.debug(String.format(
                "Publishing a follow: user '%s' follows '%s'", dto.getFollowingUser(), dto.getFollowedUser()
        ));
        followsTemplate.convertAndSend(QueueTopic.FOLLOW.getTopic(), dto);
    }

    /**
     * Publishes messages containing information about users unfollowing other users.
     * @param dto contains information about who wants to unfollow whom
     */
    public void publishUnfollow(FollowInfoDto dto) {
        LOGGER.debug(String.format(
                "Publishing an unfollow: user '%s' unfollows '%s'", dto.getFollowingUser(), dto.getFollowedUser()
        ));
        followsTemplate.convertAndSend(QueueTopic.UNFOLLOW.getTopic(), dto);
    }
}
