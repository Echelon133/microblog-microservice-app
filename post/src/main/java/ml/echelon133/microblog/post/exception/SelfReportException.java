package ml.echelon133.microblog.post.exception;

/**
 * Exception thrown when a user tries to report their own post.
 */
public class SelfReportException extends Exception {

    public SelfReportException() {
        super("users can only report posts of other users");
    }
}
