package ml.echelon133.microblog.report.repository;

import ml.echelon133.microblog.shared.report.Report;
import ml.echelon133.microblog.shared.report.ReportDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    /**
     * Finds a {@link Page} of reports filtered by their {@code checked} status.
     *
     * @param checked whether to fetch reports which had already been checked or not
     * @param pageable information about the wanted page
     * @return a {@link Page} of reports
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.report.ReportDto(r.id, r.dateCreated, r.reason, r.context, r.reportedPost, r.reportingUser, r.accepted, r.checked) " +
            "FROM Report r WHERE r.checked = ?1")
    Page<ReportDto> findReports(boolean checked, Pageable pageable);
}
