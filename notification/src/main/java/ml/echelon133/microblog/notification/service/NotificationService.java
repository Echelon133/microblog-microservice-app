package ml.echelon133.microblog.notification.service;

import ml.echelon133.microblog.notification.exception.NotificationNotFoundException;
import ml.echelon133.microblog.notification.exception.NotificationReadingForbiddenException;
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

    /**
     * Marks a single notification with {@code notificationId} as read.
     *
     * @param userRequesting id of the user who requests a notification be marked as read
     * @param notificationId id of the notification to read
     * @return how many notifications have been marked as read
     * @throws NotificationNotFoundException thrown when the notification with given id does not exist
     * @throws NotificationReadingForbiddenException thrown when a user is not the recipient
     * of the notification with specified id
     */
    public Integer readSingleNotification(UUID userRequesting, UUID notificationId)
            throws NotificationNotFoundException, NotificationReadingForbiddenException {

        var notificationToRead = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notificationToRead.getUserToNotify().equals(userRequesting)) {
            throw new NotificationReadingForbiddenException(userRequesting, notificationId);
        }

        return notificationRepository.readSingleNotification(notificationId);
    }

    /**
     * Marks all notifications of user {@code userId} as read.
     *
     * @param userId id of the user whose all notification will be marked as read
     * @return how many notifications have been marked as read
     */
    public Integer readAllNotificationsOfUser(UUID userId) {
        return notificationRepository.readAllNotificationsOfUser(userId);
    }
}
