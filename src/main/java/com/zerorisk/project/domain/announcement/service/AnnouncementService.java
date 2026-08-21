package com.zerorisk.project.domain.announcement.service;

import com.zerorisk.project.domain.announcement.dto.AnnouncementRequest;
import com.zerorisk.project.domain.announcement.dto.AnnouncementResponse;
import com.zerorisk.project.domain.announcement.entity.Announcement;
import com.zerorisk.project.domain.announcement.exception.AnnouncementErrorCode;
import com.zerorisk.project.domain.announcement.exception.AnnouncementException;
import com.zerorisk.project.domain.announcement.repository.AnnouncementRepository;
import com.zerorisk.project.global.audit.AdminActionLogger;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AdminActionLogger adminActionLogger;

    public List<AnnouncementResponse> getAnnouncements() {
        return announcementRepository.findAllByOrderByIsImportantDescCreatedAtDesc().stream()
                .map(AnnouncementResponse::from).toList();
    }

    @Transactional
    public Long create(Long adminId, AnnouncementRequest request) {
        Announcement announcement = Announcement.builder()
                .tag(request.tag()).title(request.title()).content(request.content())
                .isImportant(request.isImportant()).createdBy(adminId)
                .build();
        announcementRepository.save(announcement);
        adminActionLogger.log(adminId, "CREATE", "ANNOUNCEMENT", announcement.getId(), "[" + request.title() + "] 공지사항 등록");
        return announcement.getId();
    }

    @Transactional
    public void update(Long adminId, Long id, AnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementException(AnnouncementErrorCode.NOT_FOUND));
        announcement.update(request.tag(), request.title(), request.content(), request.isImportant());
        adminActionLogger.log(adminId, "UPDATE", "ANNOUNCEMENT", id, "[" + request.title() + "] 공지사항 수정");
    }

    @Transactional
    public void delete(Long adminId, Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementException(AnnouncementErrorCode.NOT_FOUND));
        announcementRepository.delete(announcement);
        adminActionLogger.log(adminId, "DELETE", "ANNOUNCEMENT", id, "[" + announcement.getTitle() + "] 공지사항 삭제");
    }
}
