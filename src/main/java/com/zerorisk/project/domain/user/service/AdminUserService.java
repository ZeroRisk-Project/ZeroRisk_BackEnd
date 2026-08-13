package com.zerorisk.project.domain.user.service;

import com.zerorisk.project.domain.openbanking.entity.OpenBankingAuth;
import com.zerorisk.project.domain.openbanking.repository.OpenBankingAuthRepository;
import com.zerorisk.project.domain.user.dto.AdminUserResponse;
import com.zerorisk.project.domain.user.dto.UserSuspendRequest;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.entity.UserStatus;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.audit.AdminActionLogger;
import com.zerorisk.project.global.exception.UserNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

        private final UserRepository userRepository;
        private final OpenBankingAuthRepository openBankingAuthRepository;
        private final AdminActionLogger adminActionLogger;

        public Page<AdminUserResponse> getUsers(String keyword, UserStatus status, Pageable pageable) {
                Page<User> users = userRepository.searchUsers(keyword, status, pageable);

                List<Long> userIds = users.getContent().stream()
                                .map(User::getId)
                                .toList();

                Map<Long, String> accountNumMap = userIds.isEmpty()
                                ? Map.of()
                                : openBankingAuthRepository.findByUserIdIn(userIds).stream()
                                                .collect(Collectors.toMap(OpenBankingAuth::getUserId,
                                                                OpenBankingAuth::getAccountNumMasked));

                return users.map(user -> AdminUserResponse.from(user, accountNumMap.get(user.getId())));
        }

        @Transactional
        public AdminUserResponse suspendUser(Long adminId, Long userId, UserSuspendRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(UserNotFoundException::new);

                user.suspend(request.suspendedUntil(), request.reason());

                adminActionLogger.log(adminId, "SUSPEND", "USER", userId,
                                String.format("%s님 정지 처리 (사유: %s)", user.getNickname(), request.reason()));

                String accountNumMasked = openBankingAuthRepository.findByUserId(userId)
                                .map(OpenBankingAuth::getAccountNumMasked)
                                .orElse(null);

                return AdminUserResponse.from(user, accountNumMasked);
        }

        @Transactional
        public AdminUserResponse unsuspendUser(Long adminId, Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(UserNotFoundException::new);

                user.unsuspend();

                adminActionLogger.log(adminId, "UNSUSPEND", "USER", userId,
                                String.format("%s님 정지 해제", user.getNickname()));

                String accountNumMasked = openBankingAuthRepository.findByUserId(userId)
                                .map(OpenBankingAuth::getAccountNumMasked)
                                .orElse(null);

                return AdminUserResponse.from(user, accountNumMasked);
        }
}
