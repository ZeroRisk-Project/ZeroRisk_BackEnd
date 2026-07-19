package com.zerorisk.project.domain.report.repository;

import com.zerorisk.project.domain.report.entity.Report;
import com.zerorisk.project.domain.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
}