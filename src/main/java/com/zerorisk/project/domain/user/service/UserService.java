package com.zerorisk.project.domain.user.service;

import com.zerorisk.project.domain.user.dto.MyProfileResponse;
import com.zerorisk.project.domain.user.dto.SignupRequest;
import com.zerorisk.project.domain.user.dto.SignupResponse;
import com.zerorisk.project.domain.user.dto.UpdateProfileRequest;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.DuplicateEmailException;
import com.zerorisk.project.global.exception.DuplicateNicknameException;
import com.zerorisk.project.global.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new DuplicateNicknameException();
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
                .email(request.email())
                .nickname(request.nickname())
                .password(encodedPassword)
                .build();

        User savedUser = userRepository.save(user);

        return new SignupResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return MyProfileResponse.from(user);
    }

    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (!user.getNickname().equals(request.nickname())
                && userRepository.existsByNickname(request.nickname())) {
            throw new DuplicateNicknameException();
        }

        user.updateProfile(request.nickname(), request.profileImageUrl());
        return MyProfileResponse.from(user);
    }
}