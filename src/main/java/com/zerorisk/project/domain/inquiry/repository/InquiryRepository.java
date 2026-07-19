package com.zerorisk.project.domain.inquiry.repository;

import com.zerorisk.project.domain.inquiry.entity.Inquiry;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Optional<Inquiry> findByIdAndUserId(Long id, Long userId);

    Page<Inquiry> findByUserId(Long userId, Pageable pageable);
}