package ml.echelon133.microblog.post.queue;

import ml.echelon133.microblog.post.repository.FollowRepository;
import ml.echelon133.microblog.shared.queue.QueueTopic;
import ml.echelon133.microblog.shared.user.follow.Follow;
import ml.echelon133.microblog.shared.user.follow.FollowId;
import ml.echelon133.microblog.shared.user.follow.FollowInfoDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class FollowMessageListener implements MessageListener {

    private static final Logger LOGGER = LogManager.getLogger(FollowMessageListener.class);

    private final FollowRepository followRepository;

    @Autowired
    public FollowMessageListener(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String topicName = new String(message.getChannel());

        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(message.getBody()));
            var follow = (FollowInfoDto)ois.readObject();
            if (topicName.equals(QueueTopic.FOLLOW.getTopic())) {
                LOGGER.debug(
                        String.format("Saving a follow of %s by user %s",
                                follow.getFollowedUser(), follow.getFollowingUser()
                        ));
                followRepository.save(new Follow(follow.getFollowingUser(), follow.getFollowedUser()));
            } else if (topicName.equals(QueueTopic.UNFOLLOW.getTopic())) {
                LOGGER.debug(
                        String.format("Removing a follow of %s by user %s",
                                follow.getFollowedUser(), follow.getFollowingUser()
                        ));
                followRepository.deleteById(new FollowId(follow.getFollowingUser(), follow.getFollowedUser()));
            } else {
                LOGGER.warn("Received unexpected topic name: " + topicName);
            }
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.error("Failed to deserialize a message from topic " + topicName);
            e.printStackTrace();
        }
    }
}
