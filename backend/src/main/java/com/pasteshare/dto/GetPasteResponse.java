package com.pasteshare.dto;

import java.time.Instant;

public record GetPasteResponse(
        String id,
        String content,
        Instant createdAt,
        Instant expiresAt,
        Long viewsRemaining) {
}
