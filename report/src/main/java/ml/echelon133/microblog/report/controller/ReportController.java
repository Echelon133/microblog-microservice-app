package ml.echelon133.microblog.report.controller;

import ml.echelon133.microblog.report.exception.ReportAlreadyCheckedException;
import ml.echelon133.microblog.report.service.ReportService;
import ml.echelon133.microblog.shared.exception.ResourceNotFoundException;
import ml.echelon133.microblog.shared.report.ReportDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public Page<ReportDto> getReports(@PageableDefault(size = 20) Pageable pageable,
                                      @RequestParam(required = false, defaultValue = "false") Boolean checked) {
        return reportService.findReports(checked, pageable);
    }

    @PostMapping("/{reportId}")
    public void checkReport(@PathVariable UUID reportId, @RequestParam Boolean accept)
            throws ResourceNotFoundException, ReportAlreadyCheckedException {

        reportService.checkReport(reportId, accept);
    }
}
