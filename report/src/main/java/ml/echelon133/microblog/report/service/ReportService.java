package ml.echelon133.microblog.report.service;

import ml.echelon133.microblog.report.exception.ReportAlreadyCheckedException;
import ml.echelon133.microblog.report.exception.ReportNotFoundException;
import ml.echelon133.microblog.report.queue.ReportActionPublisher;
import ml.echelon133.microblog.report.repository.ReportRepository;
import ml.echelon133.microblog.shared.report.ReportActionDto;
import ml.echelon133.microblog.shared.report.ReportDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
@Transactional
public class ReportService {

    private ReportRepository reportRepository;
    private ReportActionPublisher reportActionPublisher;

    @Autowired
    public ReportService(ReportRepository reportRepository, ReportActionPublisher reportActionPublisher) {
        this.reportRepository = reportRepository;
        this.reportActionPublisher = reportActionPublisher;
    }

    /**
     * Finds a {@link Page} of reports filtered by their {@code checked} status.
     *
     * @param checked whether to fetch reports which had already been checked or not
     * @param pageable information about the wanted page
     * @return a {@link Page} of reports
     */
    public Page<ReportDto> findReports(boolean checked, Pageable pageable) {
        return reportRepository.findReports(checked, pageable);
    }

    /**
     * Marks a report as accepted or rejected based on the {@code accepted} flag. A report that's accepted
     * results in deletion of the reported post. Rejection of a report does not impact the reported post in any way.
     *
     * When a decision regarding a report is taken, report's {@code checked} flag is set and the report cannot be
     * evaluated again.
     *
     * @param reportId id of the report which is being checked
     * @param accept flag which decides whether the report will be accepted or rejected
     * @throws ReportNotFoundException thrown when the report is not found
     * @throws ReportAlreadyCheckedException thrown when the report has already been checked
     */
    public void checkReport(UUID reportId, boolean accept)
            throws ReportNotFoundException, ReportAlreadyCheckedException {

        var foundReport = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        if (foundReport.isChecked()) {
            throw new ReportAlreadyCheckedException(reportId);
        }

        if (accept) {
            foundReport.setAccepted(true);
            reportActionPublisher.publishReportAction(new ReportActionDto(
                    foundReport.getReportedPost(), foundReport.getReason()
            ));
        }

        foundReport.setChecked(true);
        reportRepository.save(foundReport);
    }
}
