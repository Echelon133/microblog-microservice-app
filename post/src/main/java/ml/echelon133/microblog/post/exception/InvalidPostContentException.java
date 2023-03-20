package ml.echelon133.microblog.post.exception;

import java.util.List;

/**
 * Exception thrown when content sent by a user cannot be used to create a new post.
 */
public class InvalidPostContentException extends Exception {

    private List<String> messages;

    public InvalidPostContentException(List<String> messages) {
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }
}
