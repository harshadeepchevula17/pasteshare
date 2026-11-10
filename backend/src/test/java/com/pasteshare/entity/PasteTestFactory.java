package com.pasteshare.entity;

import java.time.Instant;

import org.springframework.test.util.ReflectionTestUtils;

public final class PasteTestFactory {

    private PasteTestFactory() {
    }

    public static Paste create(String id, String content, Instant createdAt, Instant expiresAt, Integer maxViews,
            long viewCount) {
        Paste paste = new Paste();
        ReflectionTestUtils.setField(paste, "id", id);
        ReflectionTestUtils.setField(paste, "content", content);
        ReflectionTestUtils.setField(paste, "createdAt", createdAt);
        ReflectionTestUtils.setField(paste, "expiresAt", expiresAt);
        ReflectionTestUtils.setField(paste, "maxViews", maxViews);
        ReflectionTestUtils.setField(paste, "viewCount", viewCount);
        return paste;
    }
}
