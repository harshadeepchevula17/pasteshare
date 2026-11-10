package com.pasteshare.dto;

import java.time.Instant;

public record CreatePasteResponse(
        String id,
        String shareUrl,
        Instant createdAt,
        Instant expiresAt,
        Integer maxViews) {
}
