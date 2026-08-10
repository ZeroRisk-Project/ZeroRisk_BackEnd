package com.zerorisk.project.global.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminActionLogService {

    private final AdminActionLogRepository adminActionLogRepository;

    public Page<AdminActionLogResponse> getLogs(Pageable pageable) {
        return adminActionLogRepository.findAllWithAdminNickname(pageable)
                .map(p -> new AdminActionLogResponse(
                        p.getId(), p.getAdminNickname(), p.getActionType(),
                        p.getTargetType(), p.getTargetId(), p.getDetail(), p.getCreatedAt()));
    }
}
