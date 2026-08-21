package com.zerorisk.project.domain.announcement.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AnnouncementErrorCode {

    NOT_FOUND("ANNOUNCEMENT_001", "공지사항을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
