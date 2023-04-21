package ml.echelon133.microblog.shared.exception;

import java.util.List;

/**
 * Exception thrown when user-provided body of a request is not valid.
 */
public class ProvidedValuesInvalidException extends Exception {

    private List<String> messages;

    public ProvidedValuesInvalidException(List<String> messages) {
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }
}
