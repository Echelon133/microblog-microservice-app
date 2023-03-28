package ml.echelon133.microblog.post.controller;

import ml.echelon133.microblog.post.exception.InvalidPostContentException;
import ml.echelon133.microblog.post.exception.PostDeletionForbiddenException;
import ml.echelon133.microblog.post.exception.PostNotFoundException;
import ml.echelon133.microblog.shared.exception.AbstractExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice(assignableTypes = {PostController.class, TagController.class})
public class PostExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(value = InvalidPostContentException.class)
    protected ResponseEntity<ErrorMessage> handleInvalidPostContentException(InvalidPostContentException ex,
                                                                             WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessages());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = PostNotFoundException.class)
    protected  ResponseEntity<ErrorMessage> handlePostNotFoundException(PostNotFoundException ex,
                                                                        WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = PostDeletionForbiddenException.class)
    protected  ResponseEntity<ErrorMessage> handlePostDeletionForbiddenException(PostDeletionForbiddenException ex,
                                                                                 WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.FORBIDDEN, request, ex.getMessage());
        return error.asResponseEntity();
    }
}
