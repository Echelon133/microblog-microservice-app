package ml.echelon133.microblog.notification.service;

import ml.echelon133.microblog.notification.repository.NotificationRepository;
import ml.echelon133.microblog.shared.notification.NotificationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Creates a {@link Page} containing projections of notifications of user {@code userId}.
     * The most recent notifications appear first, and the state of a notification (i.e. whether it is marked as
     * read or not) does not matter when it comes to ordering.
     *
     * @param userId id of the user whose notifications will be fetched
     * @param pageable information about the wanted page
     * @return a {@link Page} containing user's notifications
     */
    public Page<NotificationDto> findAllNotificationsOfUser(UUID userId, Pageable pageable) {
        return notificationRepository.findNotificationsOfUser(userId, pageable);
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
