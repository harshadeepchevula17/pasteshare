package com.pasteshare.service;

import java.time.Clock;
import java.time.Instant;

import com.pasteshare.config.PasteShareProperties;
import com.pasteshare.dto.CreatePasteResponse;
import com.pasteshare.dto.GetPasteResponse;
import com.pasteshare.exception.InvalidPasteException;
import com.pasteshare.exception.PasteGoneException;
import com.pasteshare.exception.PasteNotFoundException;
import com.pasteshare.repository.PasteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasteService {

    private final PasteRepository pasteRepository;
    private final PasteIdGenerator pasteIdGenerator;
    private final PasteShareProperties properties;
    private final Clock clock;

    public PasteService(
            PasteRepository pasteRepository,
            PasteIdGenerator pasteIdGenerator,
            PasteShareProperties properties,
            Clock clock) {
        this.pasteRepository = pasteRepository;
        this.pasteIdGenerator = pasteIdGenerator;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public CreatePasteResponse createPaste(String content, Instant expiresAt, Integer maxViews) {
        if (content.length() > properties.maxContentLength()) {
            throw new InvalidPasteException(
                    "Content must not exceed " + properties.maxContentLength() + " characters.");
        }

        Instant createdAt = clock.instant();
        for (int attempt = 0; attempt < properties.idGenerationAttempts(); attempt++) {
            String id = pasteIdGenerator.generate();
            int inserted = pasteRepository.insertIfIdAvailable(id, content, createdAt, expiresAt, maxViews);
            if (inserted == 1) {
                return new CreatePasteResponse(
                        id,
                        shareUrl(id),
                        createdAt,
                        expiresAt,
                        maxViews);
            }
            if (inserted != 0) {
                throw new IllegalStateException("Paste insert affected an unexpected number of rows.");
            }
        }

        throw new IllegalStateException("Unable to generate a unique paste ID after the configured attempts.");
    }

    @Transactional
    public GetPasteResponse getPaste(String id) {
        var paste = pasteRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new PasteNotFoundException(id));

        Instant now = clock.instant();
        if (paste.getExpiresAt() != null && !paste.getExpiresAt().isAfter(now)) {
            throw new PasteGoneException();
        }
        if (paste.getMaxViews() != null && paste.getViewCount() >= paste.getMaxViews()) {
            throw new PasteGoneException();
        }

        paste.incrementViewCount();
        Long viewsRemaining = paste.getMaxViews() == null
                ? null
                : paste.getMaxViews().longValue() - paste.getViewCount();
        return new GetPasteResponse(
                paste.getId(),
                paste.getContent(),
                paste.getCreatedAt(),
                paste.getExpiresAt(),
                viewsRemaining);
    }

    private String shareUrl(String id) {
        String publicBaseUrl = properties.publicBaseUrl().replaceAll("/+$", "");
        return publicBaseUrl + "/p/" + id;
    }
}
