package com.zerorisk.project.global.exception;

import com.zerorisk.project.domain.watchlist.exception.WatchlistException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.zerorisk.project.domain.account.exception.AccountException;
import com.zerorisk.project.domain.announcement.exception.AnnouncementException;
import com.zerorisk.project.domain.competition.exception.CompetitionException;
import com.zerorisk.project.domain.openbanking.exception.OpenBankingException;
import com.zerorisk.project.domain.order.exception.OrderException;
import com.zerorisk.project.domain.pricealert.exception.PriceAlertException;
import com.zerorisk.project.domain.systemnotice.exception.SystemNoticeException;

import jakarta.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("AUTH_001", e.getMessage()));
    }

    @ExceptionHandler(DuplicateNicknameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateNickname(DuplicateNicknameException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("AUTH_002", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_001", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("SERVER_001", "일시적인 오류가 발생했습니다."));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("AUTH_003", e.getMessage()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("AUTH_004", e.getMessage()));
    }

    @ExceptionHandler(SelfFollowException.class)
    public ResponseEntity<ErrorResponse> handleSelfFollow(SelfFollowException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("FOLLOW_001", e.getMessage()));
    }

    @ExceptionHandler(DuplicateFollowException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateFollow(DuplicateFollowException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("FOLLOW_002", e.getMessage()));
    }

    @ExceptionHandler(FollowNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFollowNotFound(FollowNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("FOLLOW_003", e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("USER_001", e.getMessage()));
    }

    @ExceptionHandler(PracticeCreditNotEligibleException.class)
    public ResponseEntity<ErrorResponse> handlePracticeCreditNotEligible(PracticeCreditNotEligibleException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("USER_002", e.getMessage()));
    }

    @ExceptionHandler(PendingOrdersExistException.class)
    public ResponseEntity<PendingOrdersExistResponse> handlePendingOrdersExist(PendingOrdersExistException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new PendingOrdersExistResponse("USER_003", e.getMessage(), e.getPendingOrders()));
    }

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFound(ReportNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("REPORT_001", e.getMessage()));
    }

    @ExceptionHandler(ReportTargetNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportTargetNotFound(ReportTargetNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("REPORT_002", e.getMessage()));
    }

    @ExceptionHandler(InquiryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInquiryNotFound(InquiryNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("INQUIRY_001", e.getMessage()));
    }

    @ExceptionHandler(InquiryAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleInquiryAccessDenied(InquiryAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("INQUIRY_002", e.getMessage()));
    }

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStockNotFound(StockNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("STOCK_001", e.getMessage()));
    }

    @ExceptionHandler(StockQuoteUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleStockQuoteUnavailable(StockQuoteUnavailableException e) {
        log.warn("현재가 조회 실패", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("STOCK_002", e.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("입력값이 올바르지 않습니다.");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_002", message));
    }

    @ExceptionHandler(MyRankingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMyRankingNotFound(MyRankingNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("RANKING_001", e.getMessage()));
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePostNotFound(PostNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("POST_001", e.getMessage()));
    }

    @ExceptionHandler(PostAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePostAccessDenied(PostAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("POST_002", e.getMessage()));
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFound(CommentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("COMMENT_001", e.getMessage()));
    }

    @ExceptionHandler(CommentAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCommentAccessDenied(CommentAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("COMMENT_002", e.getMessage()));
    }

    @ExceptionHandler(ChatAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleChatAccessDenied(ChatAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("CHAT_001", e.getMessage()));
    }

    @ExceptionHandler(ChatMessageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChatMessageNotFound(ChatMessageNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("CHAT_002", e.getMessage()));
    }

    @ExceptionHandler(ChatMessageEmptyException.class)
    public ResponseEntity<ErrorResponse> handleChatMessageEmpty(ChatMessageEmptyException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("CHAT_004", e.getMessage()));
    }

    @ExceptionHandler(ChatRateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleChatRateLimitExceeded(ChatRateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ErrorResponse("CHAT_003", e.getMessage()));
    }

    @ExceptionHandler(OpenBankingException.class)
    public ResponseEntity<ErrorResponse> handleOpenBanking(OpenBankingException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(CompetitionException.class)
    public ResponseEntity<ErrorResponse> handleCompetition(CompetitionException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(
            org.springframework.security.authorization.AuthorizationDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ADMIN_001", "관리자 권한이 필요합니다."));
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCode(InvalidVerificationCodeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("AUTH_006", e.getMessage()));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified(EmailNotVerifiedException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("AUTH_007", e.getMessage()));
    }

    @ExceptionHandler(AccountException.class)
    public ResponseEntity<ErrorResponse> handleAccount(AccountException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(SocialAccountPasswordChangeException.class)
    public ResponseEntity<ErrorResponse> handleSocialPasswordChange(SocialAccountPasswordChangeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("AUTH_008", e.getMessage()));
    }

    @ExceptionHandler(CaptchaRequiredException.class)
    public ResponseEntity<ErrorResponse> handleCaptchaRequired(CaptchaRequiredException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("AUTH_009", e.getMessage()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse("AUTH_010", e.getMessage()));
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ErrorResponse> handleOrder(OrderException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(AnnouncementException.class)
    public ResponseEntity<ErrorResponse> handleAnnouncement(AnnouncementException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(SystemNoticeException.class)
    public ResponseEntity<ErrorResponse> handleSystemNotice(SystemNoticeException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(WatchlistException.class)
    public ResponseEntity<ErrorResponse> handleWatchlist(WatchlistException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(PriceAlertException.class)
    public ResponseEntity<ErrorResponse> handlePriceAlert(PriceAlertException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(new ErrorResponse(e.getErrorCode().getCode(), e.getMessage()));
    }

    @ExceptionHandler(InvalidKakaoWebhookException.class)
    public ResponseEntity<ErrorResponse> handleInvalidKakaoWebhook(InvalidKakaoWebhookException e) {
        log.warn("유효하지 않은 카카오 웹훅 요청 차단");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("AUTH_011", e.getMessage()));
    }

    // 동시 요청으로 DB의 유니크/체크 제약을 위반한 경우(예: 팔로우 중복, 좋아요/투표 중복)의 최종
    // 방어선. 개별 서비스에서 더 구체적인 예외로 미리 잡아 처리하지 못한 나머지가 여기로 온다 -
    // 이런 경우는 서버 오류(500)가 아니라 "이미 처리된 요청" 충돌(409)로 보는 게 맞다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("DB 제약 위반으로 요청 거부", e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("COMMON_001", "이미 처리된 요청입니다."));
    }
}