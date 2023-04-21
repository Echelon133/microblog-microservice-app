package ml.echelon133.microblog.shared.report;

import ml.echelon133.microblog.shared.report.validator.ValidReason;
import org.hibernate.validator.constraints.Length;

public class ReportBodyDto {

    @ValidReason
    private String reason;

    @Length(max = 300, message = "context's valid length between 0 and 300 characters")
    private String context;

    public ReportBodyDto() {}
    public ReportBodyDto(String reason, String context) {
        this.reason = reason;
        this.context = context;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
