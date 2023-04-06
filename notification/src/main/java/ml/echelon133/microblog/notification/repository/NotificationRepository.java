package ml.echelon133.microblog.notification.repository;

import ml.echelon133.microblog.shared.notification.Notification;
import ml.echelon133.microblog.shared.notification.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    int countByUserToNotifyAndReadFalse(UUID userId);

    @Query("SELECT NEW ml.echelon133.microblog.shared.notification.NotificationDto(n.id, n.dateCreated, n.notificationSource, n.type, n.read) " +
            "FROM Notification n WHERE n.userToNotify = ?1 ORDER BY n.dateCreated DESC")
    Page<NotificationDto> findNotificationsOfUser(UUID userId, Pageable pageable);
}
