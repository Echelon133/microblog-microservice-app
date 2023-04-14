package ml.echelon133.microblog.shared.report;

import java.io.Serializable;
import java.util.UUID;

public class ReportCreationDto implements Serializable {

    private Report.Reason reason;
    private String context;
    private UUID reportedPost;
    private UUID reportingUser;

    public ReportCreationDto(Report.Reason reason, String context, UUID reportedPost, UUID reportingUser) {
        this.reason = reason;
        this.context = context;
        this.reportedPost = reportedPost;
        this.reportingUser = reportingUser;
    }

    public Report.Reason getReason() {
        return reason;
    }

    public void setReason(Report.Reason reason) {
        this.reason = reason;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public UUID getReportedPost() {
        return reportedPost;
    }

    public void setReportedPost(UUID reportedPost) {
        this.reportedPost = reportedPost;
    }

    public UUID getReportingUser() {
        return reportingUser;
    }

    public void setReportingUser(UUID reportingUser) {
        this.reportingUser = reportingUser;
    }
}
