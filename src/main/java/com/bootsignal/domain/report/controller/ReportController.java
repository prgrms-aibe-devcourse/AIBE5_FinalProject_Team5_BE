package com.bootsignal.domain.report.controller;

import com.bootsignal.domain.report.dto.ReportCreateRequest;
import com.bootsignal.domain.report.dto.ReportResponse;
import com.bootsignal.domain.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@RequestBody @Valid ReportCreateRequest request) {
        return reportService.create(request);
    }
}
