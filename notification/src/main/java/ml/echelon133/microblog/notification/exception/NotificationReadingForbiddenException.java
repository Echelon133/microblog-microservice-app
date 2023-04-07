package ml.echelon133.microblog.notification.exception;

import ml.echelon133.microblog.shared.notification.Notification;

import java.util.UUID;

/**
 * Exception thrown when user is not the recipient of a {@link Notification} with specified {@link UUID}.
 */
public class NotificationReadingForbiddenException extends Exception {

    public NotificationReadingForbiddenException(UUID userId, UUID notificationId) {
        super(String.format("User with id '%s' cannot read a notification with id '%s'", userId, notificationId));
    }
}
