package ml.echelon133.microblog.report.repository;

import ml.echelon133.microblog.shared.report.Report;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
@DisplayName("Tests of ReportRepository")
public class ReportRepositoryTests {

    @Autowired
    private ReportRepository reportRepository;

    @Test
    @DisplayName("Custom findReports returns an empty page when there aren't any reports")
    public void findReports_NoReports_ReturnsEmptyPage() {
        // when
        var pageChecked = reportRepository.findReports(true, Pageable.unpaged());
        var pageUnchecked = reportRepository.findReports(false, Pageable.unpaged());

        // then
        assertEquals(0, pageChecked.getTotalElements());
        assertEquals(0, pageUnchecked.getTotalElements());
    }

    @Test
    @DisplayName("Custom findReports returns unchecked reports")
    public void findReports_CheckedSetFalse_ReturnsUncheckedReports() {
        // given
        var report1 =
                reportRepository.save(new Report(Report.Reason.SPAM, "", UUID.randomUUID(), UUID.randomUUID()));
        var report2 =
                reportRepository.save(new Report(Report.Reason.IMPERSONATION, "", UUID.randomUUID(), UUID.randomUUID()));
        // mark one report as checked
        report1.setChecked(true);
        reportRepository.save(report1);

        // when
        var page = reportRepository.findReports(false, Pageable.unpaged());

        // then
        assertEquals(1, page.getTotalElements());
        var r2ReceivedId = page.getContent().get(0).getReportId();
        assertEquals(report2.getId(), r2ReceivedId);
    }

    @Test
    @DisplayName("Custom findReports returns checked reports")
    public void findReports_CheckedSetTrue_ReturnsUncheckedReports() {
        // given
        var report1 =
                reportRepository.save(new Report(Report.Reason.SPAM, "", UUID.randomUUID(), UUID.randomUUID()));
        var report2 =
                reportRepository.save(new Report(Report.Reason.IMPERSONATION, "", UUID.randomUUID(), UUID.randomUUID()));
        // mark one report as checked
        report1.setChecked(true);
        reportRepository.save(report1);

        // when
        var page = reportRepository.findReports(true, Pageable.unpaged());

        // then
        assertEquals(1, page.getTotalElements());
        var r1ReceivedId = page.getContent().get(0).getReportId();
        assertEquals(report1.getId(), r1ReceivedId);
    }
}
