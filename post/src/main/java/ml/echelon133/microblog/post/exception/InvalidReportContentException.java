package ml.echelon133.microblog.post.exception;

import java.util.List;

/**
 * Exception thrown when report message sent by a user cannot be used to create a report.
 */
public class InvalidReportContentException extends Throwable {

    private List<String> messages;

    public InvalidReportContentException(List<String> messages) {
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }
}
