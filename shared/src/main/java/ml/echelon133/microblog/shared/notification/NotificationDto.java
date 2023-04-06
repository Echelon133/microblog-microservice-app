package ml.echelon133.microblog.shared.notification;

import java.util.Date;
import java.util.UUID;

public class NotificationDto {

    private UUID notificationId;
    private Date dateCreated;
    private UUID notificationSource;
    private Notification.Type type;
    private boolean read;

    public NotificationDto() {}
    public NotificationDto(UUID notificationId, Date dateCreated, UUID notificationSource, Notification.Type type, boolean read) {
        this.notificationId = notificationId;
        this.dateCreated = dateCreated;
        this.notificationSource = notificationSource;
        this.type = type;
        this.read = read;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
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
