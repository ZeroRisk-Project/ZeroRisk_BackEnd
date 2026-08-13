package com.zerorisk.project.global.monitoring;

import com.zerorisk.project.domain.inquiry.entity.InquiryStatus;
import com.zerorisk.project.domain.inquiry.repository.InquiryRepository;
import com.zerorisk.project.domain.report.entity.ReportStatus;
import com.zerorisk.project.domain.report.repository.ReportRepository;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.domain.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final AdminUserService adminUserService;
    private final ReportRepository reportRepository;
    private final InquiryRepository inquiryRepository;
    private final ServerMetrics serverMetrics;
    private final HealthEndpoint healthEndpoint;

    @GetMapping
    public DashboardSummaryResponse getSummary() {
        var metrics = serverMetrics.snapshot();

        return new DashboardSummaryResponse(
                userRepository.count(),
                adminUserService.getTodayNewUserCount(),
                reportRepository.countByStatus(ReportStatus.PENDING),
                inquiryRepository.countByStatus(InquiryStatus.PENDING),
                metrics.latestResponseTimeMs(),
                metrics.averageResponseTimeMs(),
                metrics.uptimeSeconds());
    }

    @GetMapping("/health")
    public ServerHealthResponse getHealth() {
        HealthComponent health = healthEndpoint.health();
        boolean isUp = health.getStatus().equals(Status.UP);

        boolean dbUp = isUp;
        if (health instanceof CompositeHealth composite) {
            HealthComponent db = composite.getComponents().get("db");
            if (db != null) {
                dbUp = db.getStatus().equals(Status.UP);
            }
        }

        return new ServerHealthResponse(isUp, dbUp);
    }
}
