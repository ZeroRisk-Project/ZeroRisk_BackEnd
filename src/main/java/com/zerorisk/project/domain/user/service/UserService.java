package com.zerorisk.project.domain.user.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.openbanking.repository.OpenBankingAuthRepository;
import com.zerorisk.project.domain.user.dto.ChangePasswordRequest;
import com.zerorisk.project.domain.user.dto.MyProfileResponse;
import com.zerorisk.project.domain.user.dto.NicknameCheckResponse;
import com.zerorisk.project.domain.user.dto.SignupRequest;
import com.zerorisk.project.domain.user.dto.SignupResponse;
import com.zerorisk.project.domain.user.dto.UpdateProfileRequest;
import com.zerorisk.project.domain.user.dto.WithdrawRequest;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.exception.DuplicateEmailException;
import com.zerorisk.project.global.exception.DuplicateNicknameException;
import com.zerorisk.project.global.exception.EmailNotVerifiedException;
import com.zerorisk.project.global.exception.InvalidCredentialsException;
import com.zerorisk.project.global.exception.SocialAccountPasswordChangeException;
import com.zerorisk.project.global.exception.UserNotFoundException;
import com.zerorisk.project.global.audit.UserActivityLogger;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final AccountRepository accountRepository;
    private final OpenBankingAuthRepository openBankingAuthRepository;
    private final UserActivityLogger userActivityLogger;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (!emailVerificationService.isVerified(request.email())) {
            throw new EmailNotVerifiedException();
        }
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
        emailVerificationService.clearVerification(request.email());
        userActivityLogger.log(savedUser.getId(), "SIGNUP", "회원가입 (이메일)");

        return new SignupResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());
    }

    @Transactional(readOnly = true)
    public NicknameCheckResponse checkNickname(String nickname) {
        boolean available = !userRepository.existsByNickname(nickname);
        return new NicknameCheckResponse(available);
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
        userActivityLogger.log(userId, "UPDATE_PROFILE", "닉네임을 '" + request.nickname() + "'(으)로 변경");
        return MyProfileResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getPassword() == null) {
            throw new SocialAccountPasswordChangeException();
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        user.changePassword(encodedNewPassword);
        userActivityLogger.log(userId, "CHANGE_PASSWORD", "비밀번호 변경");
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getPassword() != null) {
            if (request.password() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new InvalidCredentialsException();
            }
        }

        user.withdraw();
        userActivityLogger.log(userId, "WITHDRAW", "회원 탈퇴");
    }

    @Transactional
    public void resetSeedMoney(Long userId) {
        Account basicAccount = accountRepository.findByUserIdAndAccountType(userId, AccountType.BASIC)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        basicAccount.zeroBalance();

        openBankingAuthRepository.findByUserId(userId)
                .ifPresent(openBankingAuthRepository::delete);

        userActivityLogger.log(userId, "RESET_SEED_MONEY", "모의투자 자금 초기화");
    }
}