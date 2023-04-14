package ml.echelon133.microblog.shared.report;

import ml.echelon133.microblog.shared.base.BaseEntity;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Report extends BaseEntity {

    /**
     * Reasons for which content might get reported by users.
     */
    public enum Reason {
        SPAM, HARASSMENT, IMPERSONATION, DISTURBING_CONTENT
    }

    public Report() {}
    public Report(Reason reason, String context, UUID reportedPost, UUID reportingUser) {
        this.reason = reason;
        this.context = context;
        this.reportedPost = reportedPost;
        this.reportingUser = reportingUser;
        this.accepted = false;
        this.checked = false;
    }

    @Enumerated(value = EnumType.STRING)
    private Reason reason;

    @Column(nullable = false, updatable = false)
    private String context;

    @Column(nullable = false, updatable = false)
    private UUID reportedPost;

    @Column(nullable = false, updatable = false)
    private UUID reportingUser;

    @Column(nullable = false)
    private boolean checked;

    @Column(nullable = false)
    private boolean accepted;

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
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

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
