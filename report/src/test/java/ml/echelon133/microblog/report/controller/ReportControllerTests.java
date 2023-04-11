package ml.echelon133.microblog.report.controller;

import ml.echelon133.microblog.report.service.ReportService;
import ml.echelon133.microblog.shared.report.Report;
import ml.echelon133.microblog.shared.report.ReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static ml.echelon133.microblog.shared.auth.test.OAuth2RequestPostProcessor.customBearerToken;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of ReportController")
public class ReportControllerTests {

    private MockMvc mvc;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @BeforeEach
    public void beforeEach() {
        mvc = MockMvcBuilders
                .standaloneSetup(reportController)
                .setCustomArgumentResolvers(
                        // this is required to resolve Pageable objects in controller methods
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("getReports sets default values of request params and returns ok when there are notifications")
    public void getReports_RequestParamsNotProvided_SetsDefaultValuesAndReturnsOk() throws Exception {
        var expectedPageSize = 20;
        var expectedCheckedStatus = false;
        var dto = new ReportDto(UUID.randomUUID(), new Date(), Report.Reason.SPAM, "test", UUID.randomUUID(), UUID.randomUUID(), false, false);

        when(reportService.findReports(
                eq(expectedCheckedStatus),
                argThat(a -> a.getPageSize() == expectedPageSize)
        )).thenReturn(new PageImpl<>(List.of(dto), Pageable.ofSize(expectedPageSize), 1));

        mvc.perform(
                get("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(expectedPageSize)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].reportId", is(dto.getReportId().toString())))
                .andExpect(jsonPath("$.content[0].dateCreated", is(dto.getDateCreated().toInstant().toEpochMilli())))
                .andExpect(jsonPath("$.content[0].reason", is(dto.getReason().toString())))
                .andExpect(jsonPath("$.content[0].context", is(dto.getContext())))
                .andExpect(jsonPath("$.content[0].reportedPostId", is(dto.getReportedPostId().toString())))
                .andExpect(jsonPath("$.content[0].reportingUserId", is(dto.getReportingUserId().toString())))
                .andExpect(jsonPath("$.content[0].accepted", is(dto.isAccepted())))
                .andExpect(jsonPath("$.content[0].checked", is(dto.isChecked())));
    }

    @Test
    @DisplayName("getReports sets custom values of request params and returns ok when there are notifications")
    public void getReports_RequestParamsProvided_SetsCustomValuesAndReturnsOk() throws Exception {
        var expectedPageSize = 25;
        var expectedCheckedStatus = true;
        var dto = new ReportDto(UUID.randomUUID(), new Date(), Report.Reason.SPAM, "test", UUID.randomUUID(), UUID.randomUUID(), false, false);

        when(reportService.findReports(
                eq(expectedCheckedStatus),
                argThat(a -> a.getPageSize() == expectedPageSize)
        )).thenReturn(new PageImpl<>(List.of(dto), Pageable.ofSize(expectedPageSize), 1));

        mvc.perform(
                        get("/api/reports")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .param("checked", Boolean.valueOf(expectedCheckedStatus).toString())
                                .param("size", Integer.valueOf(expectedPageSize).toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(expectedPageSize)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].reportId", is(dto.getReportId().toString())))
                .andExpect(jsonPath("$.content[0].dateCreated", is(dto.getDateCreated().toInstant().toEpochMilli())))
                .andExpect(jsonPath("$.content[0].reason", is(dto.getReason().toString())))
                .andExpect(jsonPath("$.content[0].context", is(dto.getContext())))
                .andExpect(jsonPath("$.content[0].reportedPostId", is(dto.getReportedPostId().toString())))
                .andExpect(jsonPath("$.content[0].reportingUserId", is(dto.getReportingUserId().toString())))
                .andExpect(jsonPath("$.content[0].accepted", is(dto.isAccepted())))
                .andExpect(jsonPath("$.content[0].checked", is(dto.isChecked())));
    }
}
