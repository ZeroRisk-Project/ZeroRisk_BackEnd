package com.zerorisk.project.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record VerifyCodeRequest(
        @NotBlank @Email String email,

        @NotBlank String code) {
}