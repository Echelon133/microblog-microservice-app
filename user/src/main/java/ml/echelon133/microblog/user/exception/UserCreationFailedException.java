package ml.echelon133.microblog.user.exception;

/**
 * Exception thrown when there is some kind of unexpected problem with creation of new
 * {@link ml.echelon133.microblog.user.model.User}.
 */
public class UserCreationFailedException extends Exception {

    public UserCreationFailedException(String username) {
        super(String.format("Failed to create a user with username %s", username));
    }
}
