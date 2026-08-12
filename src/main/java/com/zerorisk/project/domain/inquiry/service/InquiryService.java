package com.zerorisk.project.domain.inquiry.service;

import com.zerorisk.project.domain.inquiry.dto.AdminInquiryResponse;
import com.zerorisk.project.domain.inquiry.dto.InquiryAnswerRequest;
import com.zerorisk.project.domain.inquiry.dto.InquiryCreateRequest;
import com.zerorisk.project.domain.inquiry.dto.InquiryResponse;
import com.zerorisk.project.domain.inquiry.entity.Inquiry;
import com.zerorisk.project.domain.inquiry.repository.InquiryAdminProjection;
import com.zerorisk.project.domain.inquiry.repository.InquiryRepository;
import com.zerorisk.project.domain.notification.entity.NotificationType;
import com.zerorisk.project.domain.notification.service.NotificationService;
import com.zerorisk.project.domain.user.entity.User;
import com.zerorisk.project.domain.user.repository.UserRepository;
import com.zerorisk.project.global.audit.AdminActionLogger;
import com.zerorisk.project.global.exception.InquiryAccessDeniedException;
import com.zerorisk.project.global.exception.InquiryNotFoundException;
import com.zerorisk.project.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AdminActionLogger adminActionLogger;

    @Transactional
    public InquiryResponse createInquiry(Long userId, InquiryCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .category(request.category())
                .title(request.title())
                .content(request.content())
                .build();

        Inquiry savedInquiry = inquiryRepository.save(inquiry);

        return InquiryResponse.from(savedInquiry);
    }

    public Page<InquiryResponse> getMyInquiries(Long userId, Pageable pageable) {
        return inquiryRepository.findByUserId(userId, pageable)
                .map(InquiryResponse::from);
    }

    public InquiryResponse getInquiry(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(InquiryNotFoundException::new);

        if (!inquiry.isOwner(userId)) {
            throw new InquiryAccessDeniedException();
        }

        return InquiryResponse.from(inquiry);
    }

    // 관리자 전용 (컨트롤러/시큐리티 단에서 ADMIN 권한 체크)
    public Page<AdminInquiryResponse> getAllInquiriesForAdmin(Pageable pageable) {
        return inquiryRepository.findAllWithAuthorNickname(pageable)
                .map(p -> new AdminInquiryResponse(
                        p.getId(), p.getAuthorNickname(), p.getCategory(), p.getTitle(),
                        p.getContent(), p.getAnswer(), p.getStatus(), p.getCreatedAt(), p.getAnsweredAt()));
    }

    // 관리자 전용 (컨트롤러/시큐리티 단에서 ADMIN 권한 체크)
    @Transactional
    public InquiryResponse answerInquiry(Long adminId, Long inquiryId, InquiryAnswerRequest request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(InquiryNotFoundException::new);

        inquiry.answer(request.answer());

        notificationService.createNotification(
                inquiry.getUser().getId(),
                NotificationType.INQUIRY_ANSWERED,
                "문의 답변 등록",
                "등록하신 문의에 답변이 등록되었습니다.",
                "/mypage/inquiries/" + inquiry.getId());

        adminActionLogger.log(adminId, "ANSWER", "INQUIRY", inquiryId,
                String.format("문의 #%d 답변 등록", inquiryId));

        return InquiryResponse.from(inquiry);
    }
}