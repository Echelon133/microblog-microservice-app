package ml.echelon133.microblog.notification.repository;

import ml.echelon133.microblog.shared.notification.Notification;
import ml.echelon133.microblog.shared.notification.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Counts all unread notifications of a user with {@code userId}.
     * @param userId id of the user whose unread notifications will be counted
     * @return number of unread notifications
     */
    int countByUserToNotifyAndReadFalse(UUID userId);

    /**
     * Finds a {@link Page} of notifications of a user.
     *
     * @param userId id of the user whose notifications will be fetched
     * @param pageable all information about the wanted page
     * @return a {@link Page} of notifications
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.notification.NotificationDto(n.id, n.dateCreated, n.notificationSource, n.type, n.read) " +
            "FROM Notification n WHERE n.userToNotify = ?1 ORDER BY n.dateCreated DESC")
    Page<NotificationDto> findNotificationsOfUser(UUID userId, Pageable pageable);

    /**
     * Marks a single notification with {@code notificationId} as read.
     *
     * @param notificationId id of the notification to read
     * @return how many notifications have been marked as read
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = ?1 ")
    int readSingleNotification(UUID notificationId);

    /**
     * Marks all notifications of a user with {@code userId} as read.
     *
     * @param userId id of the user whose all notification will be marked as read
     * @return how many notifications have been marked as read
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET read = true WHERE n.userToNotify = ?1 ")
    int readAllNotificationsOfUser(UUID userId);
}
