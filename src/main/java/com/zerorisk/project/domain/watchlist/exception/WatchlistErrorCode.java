package com.zerorisk.project.domain.watchlist.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WatchlistErrorCode {

    GROUP_NOT_FOUND("WATCHLIST_001", "관심 그룹을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ACCESS_DENIED("WATCHLIST_002", "해당 관심 그룹에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
}