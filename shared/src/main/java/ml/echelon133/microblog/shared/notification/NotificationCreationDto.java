package ml.echelon133.microblog.shared.notification;

import java.io.Serializable;
import java.util.UUID;

public class NotificationCreationDto implements Serializable {

    private UUID userToNotify;
    private UUID notificationSource;
    private Notification.Type type;

    public NotificationCreationDto(UUID userToNotify, UUID notificationSource, Notification.Type type) {
        this.userToNotify = userToNotify;
        this.notificationSource = notificationSource;
        this.type = type;
    }

    public UUID getUserToNotify() {
        return userToNotify;
    }

    public void setUserToNotify(UUID userToNotify) {
        this.userToNotify = userToNotify;
    }

    public UUID getNotificationSource() {
        return notificationSource;
    }

    public void setNotificationSource(UUID notificationSource) {
        this.notificationSource = notificationSource;
    }

    public Notification.Type getType() {
        return type;
    }

    public void setType(Notification.Type type) {
        this.type = type;
    }
}
