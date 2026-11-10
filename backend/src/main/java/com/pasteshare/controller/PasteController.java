package com.pasteshare.controller;

import java.net.URI;

import com.pasteshare.dto.CreatePasteRequest;
import com.pasteshare.dto.CreatePasteResponse;
import com.pasteshare.dto.GetPasteResponse;
import com.pasteshare.service.PasteService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pastes")
public class PasteController {

    private final PasteService pasteService;

    public PasteController(PasteService pasteService) {
        this.pasteService = pasteService;
    }

    @PostMapping
    public ResponseEntity<CreatePasteResponse> createPaste(@Valid @RequestBody CreatePasteRequest request) {
        CreatePasteResponse response = pasteService.createPaste(
                request.content(),
                request.expiresAt(),
                request.maxViews());
        return ResponseEntity
                .created(URI.create("/api/v1/pastes/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetPasteResponse> getPaste(@PathVariable String id) {
        GetPasteResponse response = pasteService.getPaste(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
