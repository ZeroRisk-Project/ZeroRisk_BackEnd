package com.zerorisk.project.global.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

    @Query("""
            SELECT log.id AS id, u.nickname AS adminNickname, log.actionType AS actionType,
                   log.targetType AS targetType, log.targetId AS targetId,
                   log.detail AS detail, log.ipAddress AS ipAddress, log.createdAt AS createdAt
            FROM AdminActionLog log
            JOIN User u ON u.id = log.adminId
            ORDER BY log.createdAt DESC
            """)
    Page<AdminActionLogProjection> findAllWithAdminNickname(Pageable pageable);
}
