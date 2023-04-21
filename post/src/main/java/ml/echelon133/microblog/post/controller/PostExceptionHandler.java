package ml.echelon133.microblog.post.controller;

import ml.echelon133.microblog.post.exception.*;
import ml.echelon133.microblog.shared.exception.AbstractExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice(assignableTypes = {PostController.class, TagController.class, FeedController.class})
public class PostExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(value = InvalidPostContentException.class)
    protected ResponseEntity<ErrorMessage> handleInvalidPostContentException(InvalidPostContentException ex,
                                                                             WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessages());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = InvalidReportContentException.class)
    protected ResponseEntity<ErrorMessage> handleInvalidReportContentException(InvalidReportContentException ex,
                                                                               WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessages());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = SelfReportException.class)
    protected ResponseEntity<ErrorMessage> handleSelfReportException(SelfReportException ex,
                                                                     WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = PostDeletionForbiddenException.class)
    protected  ResponseEntity<ErrorMessage> handlePostDeletionForbiddenException(PostDeletionForbiddenException ex,
                                                                                 WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.FORBIDDEN, request, ex.getMessage());
        return error.asResponseEntity();
    }
}
