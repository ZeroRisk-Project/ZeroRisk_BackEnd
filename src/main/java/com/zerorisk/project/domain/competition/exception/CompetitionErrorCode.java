package com.zerorisk.project.domain.competition.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CompetitionErrorCode {

    CANNOT_DELETE_ONGOING("COMP_001", "진행 중인 대회는 삭제할 수 없습니다.", HttpStatus.CONFLICT),
    NOT_FOUND("COMP_002", "대회를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_JOINABLE("COMP_003", "참가할 수 없는 대회입니다.", HttpStatus.BAD_REQUEST),
    ALREADY_JOINED("COMP_004", "이미 참가한 대회입니다.", HttpStatus.CONFLICT),
    CANNOT_UPDATE_ONGOING("COMP_005", "진행 중이거나 종료된 대회는 일정을 수정할 수 없습니다.", HttpStatus.CONFLICT),
    BASIC_ACCOUNT_REQUIRED("COMP_006", "대회 참가를 위해 먼저 오픈뱅킹 계좌 인증이 필요합니다.", HttpStatus.BAD_REQUEST),
    ARCHIVE_NOT_READY("COMP_007", "아직 종료되지 않은 대회입니다.", HttpStatus.BAD_REQUEST),
    CAPACITY_EXCEEDED("COMP_009", "대회 정원이 마감되었습니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}