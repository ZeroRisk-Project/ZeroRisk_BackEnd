package com.zerorisk.project.domain.report.repository;

import com.zerorisk.project.domain.report.entity.Report;
import com.zerorisk.project.domain.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("""
            SELECT r.id AS id, r.targetType AS targetType, r.targetId AS targetId,
                   u.nickname AS reporterNickname, r.reason AS reason,
                   r.status AS status, r.createdAt AS createdAt
            FROM Report r
            JOIN User u ON u.id = r.reporter.id
            WHERE (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC
            """)
    Page<ReportProjection> findAllWithReporterNickname(@Param("status") ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);
}
