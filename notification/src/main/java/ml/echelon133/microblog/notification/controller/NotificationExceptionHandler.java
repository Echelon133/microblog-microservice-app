package ml.echelon133.microblog.notification.controller;

import ml.echelon133.microblog.notification.exception.NotificationNotFoundException;
import ml.echelon133.microblog.notification.exception.NotificationReadingForbiddenException;
import ml.echelon133.microblog.shared.exception.AbstractExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice(assignableTypes = NotificationController.class)
public class NotificationExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(value = NotificationNotFoundException.class)
    protected ResponseEntity<ErrorMessage> handleNotificationNotFoundException(NotificationNotFoundException ex,
                                                                               WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = NotificationReadingForbiddenException.class)
    protected  ResponseEntity<ErrorMessage> handleNotificationReadingForbiddenException(NotificationReadingForbiddenException ex,
                                                                                        WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.FORBIDDEN, request, ex.getMessage());
        return error.asResponseEntity();
    }
}
