package ml.echelon133.microblog.notification.exception;

import ml.echelon133.microblog.shared.notification.Notification;
import java.util.UUID;

/**
 * Exception thrown when {@link Notification} with specified {@link UUID}
 * could not be found in the database.
 */
public class NotificationNotFoundException extends Exception {

    public NotificationNotFoundException(UUID id) {
        super(String.format("notification %s could not be found", id.toString()));
    }
}
