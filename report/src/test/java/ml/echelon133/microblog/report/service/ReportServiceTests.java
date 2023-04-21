package ml.echelon133.microblog.report.service;

import ml.echelon133.microblog.report.exception.ReportAlreadyCheckedException;
import ml.echelon133.microblog.report.queue.ReportActionPublisher;
import ml.echelon133.microblog.report.repository.ReportRepository;
import ml.echelon133.microblog.shared.exception.ResourceNotFoundException;
import ml.echelon133.microblog.shared.report.Report;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of ReportService")
public class ReportServiceTests {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportActionPublisher reportActionPublisher;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("findReports correctly calls repository")
    public void findReports_ArgumentsProvided_CorrectlyCallsRepository() {
        // when
        reportService.findReports(true, Pageable.ofSize(30));
        reportService.findReports(false, Pageable.ofSize(7));

        // then
        verify(reportRepository, times(1)).findReports(
                eq(true),
                argThat(a -> a.getPageSize() == 30)
        );
        verify(reportRepository, times(1)).findReports(
                eq(false),
                argThat(a -> a.getPageSize() == 7)
        );
    }

    @Test
    @DisplayName("checkReport throws a ResourceNotFoundException when report does not exist")
    public void checkReport_ReportNotFound_ThrowsException() {
        var reportId = UUID.randomUUID();

        // given
        given(reportRepository.findById(reportId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(ResourceNotFoundException.class, () ->
                reportService.checkReport(reportId, false)
        ).getMessage();

        // then
        assertEquals(String.format("report %s could not be found", reportId), message);
    }

    @Test
    @DisplayName("checkReport throws a ReportAlreadyCheckedException when report's 'checked' is true")
    public void checkReport_ReportAlreadyChecked_ThrowsException() {
        var reportId = UUID.randomUUID();
        var report = new Report(Report.Reason.SPAM, "", UUID.randomUUID(), UUID.randomUUID());
        report.setChecked(true);

        // given
        given(reportRepository.findById(reportId)).willReturn(Optional.of(report));

        // when
        String message = assertThrows(ReportAlreadyCheckedException.class, () ->
                reportService.checkReport(reportId, false)
        ).getMessage();

        // then
        assertEquals("report has already been checked", message);
    }

    @Test
    @DisplayName("checkReport rejecting a report only sets it's 'checked' field to true and does not change 'accepted' field")
    public void checkReport_RejectReport_SetsCheckedField() throws Exception {
        var reportId = UUID.randomUUID();
        var report = new Report(Report.Reason.SPAM, "", UUID.randomUUID(), UUID.randomUUID());

        // given
        given(reportRepository.findById(reportId)).willReturn(Optional.of(report));

        // when
        reportService.checkReport(reportId, false);

        // then
        verify(reportRepository, times(1)).save(argThat(
                a -> a.isChecked() && !a.isAccepted()
        ));
    }

    @Test
    @DisplayName("checkReport accepting a report sets both 'checked' and 'accepted' fields to true")
    public void checkReport_AcceptReport_SetsCheckedAndAcceptedFields() throws Exception {
        var reportId = UUID.randomUUID();
        var report = new Report(Report.Reason.SPAM, "", UUID.randomUUID(), UUID.randomUUID());

        // given
        given(reportRepository.findById(reportId)).willReturn(Optional.of(report));

        // when
        reportService.checkReport(reportId, true);

        // then
        verify(reportRepository, times(1)).save(argThat(
                a -> a.isChecked() && a.isAccepted()
        ));
    }

    @Test
    @DisplayName("checkReport rejecting a report does not publish a message using ReportActionPublisher")
    public void checkReport_RejectReport_DoesNotPublishReportAction() throws Exception {
        var reportId = UUID.randomUUID();
        var report = new Report(Report.Reason.SPAM, "", UUID.randomUUID(), UUID.randomUUID());

        // given
        given(reportRepository.findById(reportId)).willReturn(Optional.of(report));

        // when
        reportService.checkReport(reportId, false);

        // then
        verify(reportActionPublisher, times(0)).publishReportAction(any());
    }

    @Test
    @DisplayName("checkReport accepting a report publishes a message using ReportActionPublisher")
    public void checkReport_AcceptReport_PublishesReportAction() throws Exception {
        var reportId = UUID.randomUUID();
        var report = new Report(Report.Reason.SPAM, "", UUID.randomUUID(), UUID.randomUUID());

        // given
        given(reportRepository.findById(reportId)).willReturn(Optional.of(report));

        // when
        reportService.checkReport(reportId, true);

        // then
        verify(reportActionPublisher, times(1)).publishReportAction(argThat(
                a -> a.getPostToDelete().equals(report.getReportedPost()) &&
                     a.getReason().equals(report.getReason())
        ));
    }
}
