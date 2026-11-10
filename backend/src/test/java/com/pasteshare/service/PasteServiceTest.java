package com.pasteshare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.pasteshare.config.PasteShareProperties;
import com.pasteshare.dto.CreatePasteResponse;
import com.pasteshare.dto.GetPasteResponse;
import com.pasteshare.entity.Paste;
import com.pasteshare.exception.InvalidPasteException;
import com.pasteshare.exception.PasteGoneException;
import com.pasteshare.exception.PasteNotFoundException;
import com.pasteshare.repository.PasteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasteServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T10:00:00Z");

    @Mock
    private PasteRepository pasteRepository;

    @Mock
    private PasteIdGenerator pasteIdGenerator;

    private PasteService pasteService;

    @BeforeEach
    void setUp() {
        pasteService = new PasteService(
                pasteRepository,
                pasteIdGenerator,
                new PasteShareProperties("https://paste.example///", "http://localhost:5173", 10, 2),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsPasteWithConfiguredShareUrl() {
        when(pasteIdGenerator.generate()).thenReturn("0123456789abcdefghijkL");
        when(pasteRepository.insertIfIdAvailable(
                anyString(), eq("hello"), eq(NOW), eq(null), eq(3))).thenReturn(1);

        CreatePasteResponse response = pasteService.createPaste("hello", null, 3);

        assertEquals("0123456789abcdefghijkL", response.id());
        assertEquals("https://paste.example/p/0123456789abcdefghijkL", response.shareUrl());
        assertEquals(NOW, response.createdAt());
        assertEquals(3, response.maxViews());
    }

    @Test
    void retriesWhenGeneratedIdCollides() {
        when(pasteIdGenerator.generate()).thenReturn("collision", "0123456789abcdefghijkL");
        when(pasteRepository.insertIfIdAvailable(anyString(), anyString(), any(), any(), any()))
                .thenReturn(0, 1);

        CreatePasteResponse response = pasteService.createPaste("hello", null, null);

        assertEquals("0123456789abcdefghijkL", response.id());
        verify(pasteRepository).insertIfIdAvailable(
                "collision", "hello", NOW, null, null);
        verify(pasteRepository).insertIfIdAvailable(
                "0123456789abcdefghijkL", "hello", NOW, null, null);
    }

    @Test
    void rejectsContentExceedingConfiguredLimit() {
        assertThrows(InvalidPasteException.class, () -> pasteService.createPaste("12345678901", null, null));
        verify(pasteRepository, never()).insertIfIdAvailable(
                anyString(), anyString(), any(), any(), any());
    }

    @Test
    void retrievalReturnsLastAllowedViewAndUpdatesCount() {
        Paste paste = paste("paste-id", "text", null, 1, 0);
        when(pasteRepository.findByIdForUpdate("paste-id")).thenReturn(Optional.of(paste));

        GetPasteResponse response = pasteService.getPaste("paste-id");

        assertEquals("text", response.content());
        assertEquals(0L, response.viewsRemaining());
        assertEquals(1, paste.getViewCount());
    }

    @Test
    void rejectsRetrievalWhenViewLimitReached() {
        Paste paste = paste("paste-id", "text", null, 1, 1);
        when(pasteRepository.findByIdForUpdate("paste-id")).thenReturn(Optional.of(paste));

        assertThrows(PasteGoneException.class, () -> pasteService.getPaste("paste-id"));
        assertEquals(1, paste.getViewCount());
    }

    @Test
    void rejectsExpiredPasteAtExpirationInstant() {
        Paste paste = paste("paste-id", "text", NOW, null, 0);
        when(pasteRepository.findByIdForUpdate("paste-id")).thenReturn(Optional.of(paste));

        assertThrows(PasteGoneException.class, () -> pasteService.getPaste("paste-id"));
        assertEquals(0, paste.getViewCount());
    }

    @Test
    void rejectsMissingPaste() {
        when(pasteRepository.findByIdForUpdate("unknown")).thenReturn(Optional.empty());

        assertThrows(PasteNotFoundException.class, () -> pasteService.getPaste("unknown"));
    }

    @Test
    void unlimitedPasteHasNullRemainingViews() {
        Paste paste = paste("paste-id", "text", null, null, 0);
        when(pasteRepository.findByIdForUpdate("paste-id")).thenReturn(Optional.of(paste));

        GetPasteResponse response = pasteService.getPaste("paste-id");

        assertNull(response.viewsRemaining());
        assertEquals(1, paste.getViewCount());
    }

    private Paste paste(String id, String content, Instant expiresAt, Integer maxViews, long viewCount) {
        Paste paste = new Paste();
        ReflectionTestUtils.setField(paste, "id", id);
        ReflectionTestUtils.setField(paste, "content", content);
        ReflectionTestUtils.setField(paste, "createdAt", NOW.minusSeconds(60));
        ReflectionTestUtils.setField(paste, "expiresAt", expiresAt);
        ReflectionTestUtils.setField(paste, "maxViews", maxViews);
        ReflectionTestUtils.setField(paste, "viewCount", viewCount);
        return paste;
    }
}
