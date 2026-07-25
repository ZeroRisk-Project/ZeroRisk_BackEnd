package com.zerorisk.project.domain.competition.exception;

import lombok.Getter;

@Getter
public class CompetitionException extends RuntimeException {

    private final CompetitionErrorCode errorCode;

    public CompetitionException(CompetitionErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}