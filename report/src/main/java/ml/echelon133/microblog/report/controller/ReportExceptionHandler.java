package ml.echelon133.microblog.report.controller;

import ml.echelon133.microblog.report.exception.ReportAlreadyCheckedException;
import ml.echelon133.microblog.report.exception.ReportNotFoundException;
import ml.echelon133.microblog.shared.exception.AbstractExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice(assignableTypes = ReportController.class)
public class ReportExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(value = ReportNotFoundException.class)
    protected ResponseEntity<ErrorMessage> handleReportNotFoundException(ReportNotFoundException ex,
                                                                         WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.NOT_FOUND, request, ex.getMessage());
        return error.asResponseEntity();
    }

    @ExceptionHandler(value = ReportAlreadyCheckedException.class)
    protected ResponseEntity<ErrorMessage> handleReportAlreadyCheckedException(ReportAlreadyCheckedException ex,
                                                                               WebRequest request) {
        ErrorMessage error = new ErrorMessage(HttpStatus.UNPROCESSABLE_ENTITY, request, ex.getMessage());
        return error.asResponseEntity();
    }
}
