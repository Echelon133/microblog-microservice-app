package ml.echelon133.microblog.post.controller;

import ml.echelon133.microblog.post.exception.InvalidPostContentException;
import ml.echelon133.microblog.shared.exception.AbstractExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice(assignableTypes = PostController.class)
public class PostExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(value = InvalidPostContentException.class)
    protected ResponseEntity<ErrorMessage> handleInvalidPostContentException(InvalidPostContentException ex,
                                                                             WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessages());
        return error.asResponseEntity();
    }
}
