package com.zerorisk.project.domain.inquiry.repository;

import com.zerorisk.project.domain.inquiry.entity.Inquiry;
import com.zerorisk.project.domain.inquiry.entity.InquiryStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Optional<Inquiry> findByIdAndUserId(Long id, Long userId);

    Page<Inquiry> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT i.id AS id, u.nickname AS authorNickname, i.category AS category,
                   i.title AS title, i.content AS content, i.answer AS answer,
                   i.status AS status, i.createdAt AS createdAt, i.answeredAt AS answeredAt
            FROM Inquiry i
            JOIN i.user u
            ORDER BY i.createdAt DESC
            """)
    Page<InquiryAdminProjection> findAllWithAuthorNickname(Pageable pageable);

    long countByStatus(InquiryStatus status);
}