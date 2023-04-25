package ml.echelon133.microblog.shared.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown when user-provided body of a request is not valid.
 */
public class ProvidedValuesInvalidException extends Exception {

    private final Map<String, List<String>> validationErrors;

    public ProvidedValuesInvalidException(Map<String, List<String>> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }
}
