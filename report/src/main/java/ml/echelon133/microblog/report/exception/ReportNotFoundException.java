package ml.echelon133.microblog.report.exception;

import ml.echelon133.microblog.shared.report.Report;

import java.util.UUID;

/**
 * Exception thrown when {@link Report} with specified {@link UUID}
 * could not be found in the database.
 */
public class ReportNotFoundException extends Exception {

    public ReportNotFoundException(UUID id) {
        super(String.format("Report with id %s could not be found", id.toString()));
    }
}
