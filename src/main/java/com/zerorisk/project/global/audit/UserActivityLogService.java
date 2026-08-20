package com.zerorisk.project.global.audit;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActivityLogService {

    private final UserActivityLogRepository userActivityLogRepository;

    public List<UserActivityLogResponse> getLogs(Long userId) {
        return userActivityLogRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(UserActivityLogResponse::from)
                .toList();
    }
}
