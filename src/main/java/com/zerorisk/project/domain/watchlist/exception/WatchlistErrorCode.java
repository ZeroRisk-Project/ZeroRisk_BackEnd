package com.zerorisk.project.domain.watchlist.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WatchlistErrorCode {

    GROUP_NOT_FOUND("WATCHLIST_001", "관심 그룹을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ACCESS_DENIED("WATCHLIST_002", "해당 관심 그룹에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    FAVORITE_ALREADY_EXISTS("WATCHLIST_003", "이미 해당 그룹에 추가된 종목입니다.", HttpStatus.CONFLICT),
    FAVORITE_NOT_FOUND("WATCHLIST_004", "관심종목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}