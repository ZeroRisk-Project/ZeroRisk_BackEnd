package com.zerorisk.project.domain.user.service;

import com.zerorisk.project.domain.user.dto.AdminUserResponse;
import com.zerorisk.project.domain.user.dto.UserSuspendRequest;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    public Page<AdminUserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(AdminUserResponse::from);
    }

    @Transactional
    public AdminUserResponse suspendUser(Long userId, UserSuspendRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        user.suspend(request.suspendedUntil(), request.reason());

        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse unsuspendUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        user.unsuspend();

        return AdminUserResponse.from(user);
    }
}