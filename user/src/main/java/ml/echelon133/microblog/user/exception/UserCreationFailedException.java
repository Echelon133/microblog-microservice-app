package ml.echelon133.microblog.user.exception;

import ml.echelon133.microblog.shared.user.User;

/**
 * Exception thrown when there is some kind of unexpected problem with creation of new
 * {@link User}.
 */
public class UserCreationFailedException extends Exception {

    public UserCreationFailedException(String username) {
        super(String.format("Failed to create a user with username %s", username));
    }
}
