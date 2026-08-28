package com.zerorisk.project.global.websocket.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DisconnectEventDto {

    private Long userId;
    private String reason;

    @Builder
    public DisconnectEventDto(Long userId, String reason) {
        this.userId = userId;
        this.reason = reason;
    }
}
