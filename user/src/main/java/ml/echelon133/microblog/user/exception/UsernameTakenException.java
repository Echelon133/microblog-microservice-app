package ml.echelon133.microblog.user.exception;

import ml.echelon133.microblog.shared.user.User;

/**
 * Exception thrown when {@link User} with specified username
 * already exists in the database.
 */
public class UsernameTakenException extends Exception {

    public UsernameTakenException() {
        super("username has already been taken");
    }
}
