package ml.echelon133.microblog.report.service;

import ml.echelon133.microblog.report.repository.ReportRepository;
import ml.echelon133.microblog.shared.report.ReportDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private ReportRepository reportRepository;

    @Autowired
    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
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
}
