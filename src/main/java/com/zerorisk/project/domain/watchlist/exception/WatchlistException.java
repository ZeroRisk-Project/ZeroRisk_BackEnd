package com.zerorisk.project.domain.watchlist.exception;

import lombok.Getter;

@Getter
public class WatchlistException extends RuntimeException {

    private final WatchlistErrorCode errorCode;

    public WatchlistException(WatchlistErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}