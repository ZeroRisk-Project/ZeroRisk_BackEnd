package com.zerorisk.project.domain.systemnotice.service;

import com.zerorisk.project.domain.systemnotice.dto.SystemNoticeCreateRequest;
import com.zerorisk.project.domain.systemnotice.dto.SystemNoticeResponse;
import com.zerorisk.project.domain.systemnotice.entity.SystemNotice;
import com.zerorisk.project.domain.systemnotice.exception.SystemNoticeErrorCode;
import com.zerorisk.project.domain.systemnotice.exception.SystemNoticeException;
import com.zerorisk.project.domain.systemnotice.repository.SystemNoticeRepository;
import com.zerorisk.project.global.audit.AdminActionLogger;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemNoticeService {

    private final SystemNoticeRepository systemNoticeRepository;
    private final AdminActionLogger adminActionLogger;

    public List<SystemNoticeResponse> getActiveNotices() {
        return systemNoticeRepository.findAllByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(SystemNoticeResponse::from).toList();
    }

    public List<SystemNoticeResponse> getAllForAdmin() {
        return systemNoticeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(SystemNoticeResponse::from).toList();
    }

    @Transactional
    public Long create(Long adminId, SystemNoticeCreateRequest request) {
        SystemNotice notice = SystemNotice.builder()
                .severity(request.severity()).title(request.title()).message(request.message())
                .createdBy(adminId)
                .build();
        systemNoticeRepository.save(notice);
        adminActionLogger.log(adminId, "CREATE", "SYSTEM_NOTICE", notice.getId(),
                "[" + request.severity() + "] " + request.title());
        return notice.getId();
    }

    @Transactional
    public void deactivate(Long adminId, Long id) {
        SystemNotice notice = systemNoticeRepository.findById(id)
                .orElseThrow(() -> new SystemNoticeException(SystemNoticeErrorCode.NOT_FOUND));
        notice.deactivate();
        adminActionLogger.log(adminId, "DEACTIVATE", "SYSTEM_NOTICE", id, "[" + notice.getTitle() + "] 알림 팝업 종료");
    }
}
