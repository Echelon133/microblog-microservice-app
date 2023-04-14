package ml.echelon133.microblog.report.exception;

import ml.echelon133.microblog.shared.report.Report;

import java.util.UUID;

/**
 * Exception thrown when {@link Report} with specified {@link UUID}
 * has already been checked before.
 */
public class ReportAlreadyCheckedException extends Exception {

    public ReportAlreadyCheckedException(UUID id) {
        super(String.format("Report with id %s has already been checked", id.toString()));
    }
}
