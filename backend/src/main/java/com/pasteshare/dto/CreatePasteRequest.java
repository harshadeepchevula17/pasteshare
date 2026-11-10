package com.pasteshare.dto;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreatePasteRequest(
        @NotBlank(message = "Content must not be blank.")
        String content,
        @Future(message = "Expiration must be in the future.")
        Instant expiresAt,
        @Positive(message = "Maximum views must be greater than zero.")
        Integer maxViews) {
}
