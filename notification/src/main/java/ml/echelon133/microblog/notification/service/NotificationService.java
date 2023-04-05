package ml.echelon133.microblog.notification.service;

import ml.echelon133.microblog.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

    private NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Counts all unread notification of a user with {@code userId}.
     *
     * @param userId id of the user whose unread notification have to be counted
     * @return count of unread notifications
     */
    public Integer countUnreadOfUser(UUID userId) {
        return notificationRepository.countByUserToNotifyAndReadFalse(userId);
    }
}
