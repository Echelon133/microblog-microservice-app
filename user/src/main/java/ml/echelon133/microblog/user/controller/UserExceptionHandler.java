package ml.echelon133.microblog.user.controller;

import ml.echelon133.microblog.shared.exception.AbstractExceptionHandler;
import ml.echelon133.microblog.user.exception.UserCreationFailedException;
import ml.echelon133.microblog.user.exception.UserDataInvalidException;
import ml.echelon133.microblog.user.exception.UserNotFoundException;
import ml.echelon133.microblog.user.exception.UsernameTakenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice(assignableTypes = UserController.class)
public class UserExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(value = UserCreationFailedException.class)
    protected ResponseEntity<ErrorMessage> handleUserCreationFailedException(UserCreationFailedException ex,
                                                                             WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = UsernameTakenException.class)
    protected ResponseEntity<ErrorMessage> handleUsernameTakenException(UsernameTakenException ex,
                                                                        WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = UserNotFoundException.class)
    protected ResponseEntity<ErrorMessage> handleUserNotFoundException(UserNotFoundException ex,
                                                                       WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = UserDataInvalidException.class)
    protected ResponseEntity<ErrorMessage> handleUserDataInvalidException(UserDataInvalidException ex,
                                                                          WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessages());
        return error.asResponseEntity();
    }
}