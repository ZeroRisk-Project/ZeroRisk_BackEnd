package com.zerorisk.project.global.websocket;

import java.security.Principal;
import lombok.Getter;

@Getter
public class UserPrincipal implements Principal {

    private final Long userId;

    public UserPrincipal(Long userId) {
        this.userId = userId;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
