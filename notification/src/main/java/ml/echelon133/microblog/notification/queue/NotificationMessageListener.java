package ml.echelon133.microblog.notification.queue;

import ml.echelon133.microblog.notification.repository.NotificationRepository;
import ml.echelon133.microblog.shared.notification.Notification;
import ml.echelon133.microblog.shared.notification.NotificationCreationDto;
import ml.echelon133.microblog.shared.queue.QueueTopic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Listener of notification messages published in Redis.
 * Each received {@link NotificationCreationDto} message is transformed and then saved in the database as a {@link Notification} object.
 */
public class NotificationMessageListener implements MessageListener {

    private static final Logger LOGGER = LogManager.getLogger(NotificationMessageListener.class);

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationMessageListener(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String topicName = new String(message.getChannel());

        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(message.getBody()));
            var notification = (NotificationCreationDto)ois.readObject();
            if (topicName.equals(QueueTopic.NOTIFICATION.getTopic())) {
                var notificationType = notification.getType().toString();
                LOGGER.debug(String.format(
                        "Creating a '%s' notification for user '%s', where notification source is a post '%s'",
                                notificationType, notification.getUserToNotify(), notification.getNotificationSource()
                        ));
                notificationRepository.save(new Notification(
                        notification.getUserToNotify(),
                        notification.getNotificationSource(),
                        notification.getType()
                ));
            } else {
                LOGGER.warn("Received unexpected topic name: " + topicName);
            }
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.error("Failed to deserialize a message from topic " + topicName);
            e.printStackTrace();
        }
    }
}
