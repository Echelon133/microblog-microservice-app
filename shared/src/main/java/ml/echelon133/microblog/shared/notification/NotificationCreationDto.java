package ml.echelon133.microblog.shared.notification;

import java.io.Serializable;
import java.util.UUID;

public class NotificationCreationDto implements Serializable {

    private UUID userToNotify;
    private UUID notificationSource;
    private Notification.Type type;
    private boolean read;

    public NotificationCreationDto() {}
    public NotificationCreationDto(UUID userToNotify, UUID notificationSource, Notification.Type type) {
        this.userToNotify = userToNotify;
        this.notificationSource = notificationSource;
        this.type = type;
        this.read = false;
    }
    public NotificationCreationDto(UUID userToNotify, UUID notificationSource, Notification.Type type, boolean read) {
        this(userToNotify, notificationSource, type);
        this.read = read;
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

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
