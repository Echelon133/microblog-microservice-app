package ml.echelon133.microblog.post.queue;

import ml.echelon133.microblog.post.repository.PostRepository;
import ml.echelon133.microblog.shared.queue.QueueTopic;
import ml.echelon133.microblog.shared.report.ReportActionDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Listener of report-action messages published in Redis.
 * Each received {@link ReportActionDto} message contains information about a post which is scheduled for deletion.
 * If the post from the message exists, it will be marked as deleted.
 */
public class ReportActionMessageListener implements MessageListener {

    private static final Logger LOGGER = LogManager.getLogger(ReportActionMessageListener.class);

    private final PostRepository postRepository;

    public ReportActionMessageListener(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String topicName = new String(message.getChannel());

        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(message.getBody()));
            var reportAction = (ReportActionDto)ois.readObject();
            if (topicName.equals(QueueTopic.REPORT_ACTION.getTopic())) {
                var post = postRepository.findById(reportAction.getPostToDelete());
                if (post.isPresent()) {
                    LOGGER.debug(String.format(
                            "Deleting a post %s for reason: %s",
                            reportAction.getPostToDelete(), reportAction.getReason()
                    ));

                    var unwrappedPost = post.get();
                    unwrappedPost.setDeleted(true);
                    postRepository.save(unwrappedPost);
                } else {
                    LOGGER.debug(String.format(
                            "Post %s could not be deleted because it could not be found",
                            reportAction.getPostToDelete()
                    ));
                }
            } else {
                LOGGER.warn("Received unexpected topic name: " + topicName);
            }
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.error("Failed to deserialize a message from topic " + topicName);
            e.printStackTrace();
        }
    }
}
