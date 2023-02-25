package ml.echelon133.microblog.user.exception;

import java.util.UUID;

/**
 * Exception thrown when {@link ml.echelon133.microblog.user.model.User} with specified {@link java.util.UUID}
 * could not be found in the database.
 */
public class UserNotFoundException extends Exception {

    public UserNotFoundException(UUID id) {
        super(String.format("User with id %s could not be found", id.toString()));
    }
}
