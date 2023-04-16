package ml.echelon133.microblog.user.exception;

import java.util.List;

/**
 * Exception thrown when data provided during creation of a new user was invalid, e.g. validators of username,
 * password or email rejected received values.
 */
public class UserDataInvalidException extends Exception {

    private List<String> messages;

    public UserDataInvalidException(List<String> messages) {
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }
}
