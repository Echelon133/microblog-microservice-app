package ml.echelon133.microblog.shared.notification;

import ml.echelon133.microblog.shared.base.BaseEntity;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(
        indexes = @Index(name = "user_to_notify_index", columnList = "userToNotify")
)
@EntityListeners(AuditingEntityListener.class)
public class Notification extends BaseEntity {

    /**
     * Types of supported notifications.
     */
    public enum Type {
        /**
         * Used when a user has been followed by another user.
         */
        FOLLOW,
        /**
         * Used when a user has been mentioned by another user in that user's post.
         */
        MENTION,
        /**
         * Used when a user's post has been quoted by another user.
         */
        QUOTE,
        /**
         * Used when a user's post has been responded to by another user.
         */
        RESPONSE
    }

    @Column(nullable = false, updatable = false)
    private UUID userToNotify;

    @Column(nullable = false, updatable = false)
    private UUID notificationSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Type type;

    @Column(nullable = false)
    private boolean read;

    public Notification() {}
    public Notification(UUID userToNotify, UUID notificationSource, Type type) {
        this.userToNotify = userToNotify;
        this.notificationSource = notificationSource;
        this.type = type;
        this.read = false;
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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
