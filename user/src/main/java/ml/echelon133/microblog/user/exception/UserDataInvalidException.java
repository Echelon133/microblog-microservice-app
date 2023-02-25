package ml.echelon133.microblog.user.exception;

import java.util.List;

/**
 * Exception thrown when provided data about the user is invalid, e.g. validators of username, password or email
 * failed.
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
