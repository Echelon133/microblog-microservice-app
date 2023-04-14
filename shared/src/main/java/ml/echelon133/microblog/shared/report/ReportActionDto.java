package ml.echelon133.microblog.shared.report;

import java.io.Serializable;
import java.util.UUID;

public class ReportActionDto implements Serializable {

    private UUID postToDelete;
    private Report.Reason reason;

    public ReportActionDto(UUID postToDelete, Report.Reason reason) {
        this.postToDelete = postToDelete;
        this.reason = reason;
    }

    public UUID getPostToDelete() {
        return postToDelete;
    }

    public void setPostToDelete(UUID postToDelete) {
        this.postToDelete = postToDelete;
    }

    public Report.Reason getReason() {
        return reason;
    }

    public void setReason(Report.Reason reason) {
        this.reason = reason;
    }
}
