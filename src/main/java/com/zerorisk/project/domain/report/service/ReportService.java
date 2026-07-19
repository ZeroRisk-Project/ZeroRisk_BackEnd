package com.zerorisk.project.domain.report.service;

import com.zerorisk.project.domain.comment.repository.CommentRepository;
import com.zerorisk.project.domain.post.repository.PostRepository;
import com.zerorisk.project.domain.report.dto.ReportCreateRequest;
import com.zerorisk.project.domain.report.dto.ReportProcessRequest;
import com.zerorisk.project.domain.report.dto.ReportResponse;
import com.zerorisk.project.domain.report.entity.Report;
import com.zerorisk.project.domain.report.entity.ReportStatus;
import com.zerorisk.project.domain.report.entity.TargetType;
import com.zerorisk.project.domain.report.repository.ReportRepository;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.ReportNotFoundException;
import com.zerorisk.project.global.exception.ReportTargetNotFoundException;
import com.zerorisk.project.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public ReportResponse createReport(Long reporterId, ReportCreateRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(UserNotFoundException::new);

        validateTargetExists(request.targetType(), request.targetId());

        Report report = Report.builder()
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reporter(reporter)
                .reason(request.reason())
                .build();

        Report savedReport = reportRepository.save(report);

        return ReportResponse.from(savedReport);
    }

    // TARGET_TYPE에 따라 실제로 존재하는 대상인지 확인. FK가 없어서 이 로직이 유일한 방어선.
    private void validateTargetExists(TargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case POST -> postRepository.findByIdAndIsDeletedFalse(targetId).isPresent();
            case COMMENT -> commentRepository.findByIdAndIsDeletedFalse(targetId).isPresent();
            case USER -> userRepository.findById(targetId).isPresent();
            case CHAT -> true; // 채팅 메시지 도메인이 아직 없어서 우선 통과, 도메인 생기면 검증 추가
        };

        if (!exists) {
            throw new ReportTargetNotFoundException();
        }
    }

    public Page<ReportResponse> getReports(ReportStatus status, Pageable pageable) {
        Page<Report> reports = status != null
                ? reportRepository.findByStatus(status, pageable)
                : reportRepository.findAll(pageable);

        return reports.map(ReportResponse::from);
    }

    @Transactional
    public ReportResponse processReport(Long reportId, ReportProcessRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(ReportNotFoundException::new);

        if (request.status() == ReportStatus.PROCESSED) {
            report.process();
        } else {
            report.reject();
        }

        return ReportResponse.from(report);
    }
}