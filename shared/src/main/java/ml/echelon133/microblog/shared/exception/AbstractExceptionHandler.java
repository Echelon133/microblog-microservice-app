package ml.echelon133.microblog.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Base exception handler for all controllers.
 */
public abstract class AbstractExceptionHandler extends ResponseEntityExceptionHandler {

    public static class ErrorMessage {
        private Date timestamp;
        private List<String> messages;
        private HttpStatus status;
        private String path;

        public ErrorMessage(HttpStatus status, WebRequest request, List<String> messages) {
            this.timestamp = new Date();
            this.path = ((ServletWebRequest)request).getRequest().getRequestURI();
            this.status = status;
            this.messages = messages;
        }

        public ErrorMessage(HttpStatus status, WebRequest request, String... messages) {
            this(status, request, Arrays.asList(messages));
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public List<String> getMessages() {
            return messages;
        }

        public String getPath() {
            return path;
        }

        public Integer getStatus() {
            return status.value();
        }

        public String getError() {
            return status.getReasonPhrase();
        }

        public ResponseEntity<ErrorMessage> asResponseEntity() {
            return new ResponseEntity<>(this, status);
        }
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    protected ResponseEntity<ErrorMessage> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.BAD_REQUEST, request, ex.getMessage());
        return error.asResponseEntity();
    }
}
