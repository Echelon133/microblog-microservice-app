package ml.echelon133.microblog.shared.report;

import java.util.Date;
import java.util.UUID;

public class ReportDto {

    private UUID reportId;
    private Date dateCreated;
    private Report.Reason reason;
    private String context;
    private UUID reportedPostId;
    private UUID reportingUserId;
    private boolean accepted;
    private boolean checked;

    public ReportDto(UUID reportId, Date dateCreated, Report.Reason reason, String context, UUID reportedPostId, UUID reportingUserId, boolean accepted, boolean checked) {
        this.reportId = reportId;
        this.dateCreated = dateCreated;
        this.reason = reason;
        this.context = context;
        this.reportedPostId = reportedPostId;
        this.reportingUserId = reportingUserId;
        this.accepted = accepted;
        this.checked = checked;
    }

    public UUID getReportId() {
        return reportId;
    }

    public void setReportId(UUID reportId) {
        this.reportId = reportId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
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

    public UUID getReportedPostId() {
        return reportedPostId;
    }

    public void setReportedPostId(UUID reportedPostId) {
        this.reportedPostId = reportedPostId;
    }

    public UUID getReportingUserId() {
        return reportingUserId;
    }

    public void setReportingUserId(UUID reportingUserId) {
        this.reportingUserId = reportingUserId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
