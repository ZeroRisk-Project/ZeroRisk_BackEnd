package com.zerorisk.project.domain.user.service;

import com.zerorisk.project.domain.account.entity.Account;
import com.zerorisk.project.domain.account.entity.AccountType;
import com.zerorisk.project.domain.account.exception.AccountErrorCode;
import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.account.repository.AccountRepository;
import com.zerorisk.project.domain.openbanking.repository.OpenBankingAuthRepository;
import com.zerorisk.project.domain.order.dto.OrderSummaryResponse;
import com.zerorisk.project.domain.order.service.OrderService;
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
import com.zerorisk.project.global.exception.PendingOrdersExistException;
import com.zerorisk.project.global.exception.PracticeCreditNotEligibleException;
import com.zerorisk.project.global.exception.SocialAccountPasswordChangeException;
import com.zerorisk.project.global.exception.TooManyRequestsException;
import com.zerorisk.project.global.exception.UserNotFoundException;
import com.zerorisk.project.global.audit.UserActivityLogger;
import com.zerorisk.project.global.security.ratelimit.SlidingWindowCounter;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final BigDecimal PRACTICE_CREDIT_AMOUNT = new BigDecimal("1000000");
    private static final String SIGNUP_IP_KEY_PREFIX = "signup_ip:";
    private static final Duration SIGNUP_IP_WINDOW = Duration.ofHours(1);
    private static final int SIGNUP_IP_LIMIT = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final AccountRepository accountRepository;
    private final OpenBankingAuthRepository openBankingAuthRepository;
    private final UserActivityLogger userActivityLogger;
    private final SlidingWindowCounter slidingWindowCounter;
    private final OrderService orderService;

    @Transactional
    public SignupResponse signup(SignupRequest request, String clientIp) {
        String signupIpKey = SIGNUP_IP_KEY_PREFIX + clientIp;
        if (slidingWindowCounter.count(signupIpKey, SIGNUP_IP_WINDOW) >= SIGNUP_IP_LIMIT) {
            throw new TooManyRequestsException("잠시 후 다시 시도해주세요.");
        }
        slidingWindowCounter.record(signupIpKey, SIGNUP_IP_WINDOW);

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

        List<OrderSummaryResponse> pendingOrders = orderService.getPendingOrders(userId);
        if (!pendingOrders.isEmpty()) {
            throw new PendingOrdersExistException(pendingOrders);
        }

        user.withdraw();
        userActivityLogger.log(userId, "WITHDRAW", "회원 탈퇴");
    }

    @Transactional
    public void resetSeedMoney(Long userId) {
        // chargeSeedMoney()도 같은 락(findBasicAccountByUserIdForUpdate)을 쓰므로, 동시 요청 시
        // 방금 충전된 금액을 0으로 덮어쓰는 잔액 유실 없이 순서대로 직렬화된다.
        Account basicAccount = accountRepository.findBasicAccountByUserIdForUpdate(userId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        basicAccount.zeroBalance();

        openBankingAuthRepository.findByUserId(userId)
                .ifPresent(openBankingAuthRepository::delete);

        userActivityLogger.log(userId, "RESET_SEED_MONEY", "모의투자 자금 초기화");
    }

    @Transactional
    public void claimPracticeCredit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        boolean alreadyAuthenticated = openBankingAuthRepository.findByUserId(userId).isPresent();
        if (user.getHasClaimedPracticeCredit() || alreadyAuthenticated) {
            throw new PracticeCreditNotEligibleException();
        }

        Account basicAccount = accountRepository.findBasicAccountByUserIdForUpdate(userId)
                .orElseGet(() -> createBasicAccountSafely(userId));

        basicAccount.addSeedMoney(PRACTICE_CREDIT_AMOUNT);
        user.claimPracticeCredit();

        userActivityLogger.log(userId, "PRACTICE_CREDIT", "연습용 크레딧 100만원 지급");
    }

    // BASIC 계좌는 사용자당 하나만 있어야 한다(ACCOUNTS 유니크 제약으로 DB에서도 강제).
    // 동시에 여러 경로(연습용 크레딧/오픈뱅킹 인증)로 계좌가 없다고 판단해 동시에 생성을 시도하면
    // 나중에 커밋되는 쪽이 유니크 제약 위반을 받는데, 이 경우 먼저 만들어진 계좌를 락 걸어 다시 읽어온다.
    private Account createBasicAccountSafely(Long userId) {
        try {
            // 유니크 제약 위반이 이 안에서 바로 터지도록 flush까지 강제한다(save()만으로는
            // 실제 INSERT가 트랜잭션 커밋 시점까지 지연되어 여기서 못 잡을 수 있음).
            return accountRepository.saveAndFlush(
                    Account.builder()
                            .userId(userId)
                            .accountType(AccountType.BASIC)
                            .build());
        } catch (DataIntegrityViolationException e) {
            log.info("BASIC 계좌 동시 생성 감지 - userId: {}, 기존 계좌를 재사용합니다.", userId);
            return accountRepository.findBasicAccountByUserIdForUpdate(userId)
                    .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));
        }
    }
}