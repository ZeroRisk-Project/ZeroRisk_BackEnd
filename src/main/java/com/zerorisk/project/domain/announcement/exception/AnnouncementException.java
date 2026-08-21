package com.zerorisk.project.domain.announcement.exception;

import lombok.Getter;

@Getter
public class AnnouncementException extends RuntimeException {

    private final AnnouncementErrorCode errorCode;

    public AnnouncementException(AnnouncementErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
