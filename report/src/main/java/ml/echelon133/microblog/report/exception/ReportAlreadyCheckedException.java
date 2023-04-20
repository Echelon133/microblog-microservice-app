package ml.echelon133.microblog.report.exception;

import ml.echelon133.microblog.shared.report.Report;

import java.util.UUID;

/**
 * Exception thrown when {@link Report} with specified {@link UUID}
 * had already been checked before.
 */
public class ReportAlreadyCheckedException extends Exception {

    public ReportAlreadyCheckedException() {
        super("report has already been checked");
    }
}
