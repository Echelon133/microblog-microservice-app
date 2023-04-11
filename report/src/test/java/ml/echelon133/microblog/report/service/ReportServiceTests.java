package ml.echelon133.microblog.report.service;

import ml.echelon133.microblog.report.repository.ReportRepository;
import ml.echelon133.microblog.shared.notification.Notification;
import ml.echelon133.microblog.shared.notification.NotificationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of ReportService")
public class ReportServiceTests {

    @Mock
    private ReportRepository reportRepository;

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
}
