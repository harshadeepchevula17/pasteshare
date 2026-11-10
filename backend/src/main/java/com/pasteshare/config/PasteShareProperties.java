package com.pasteshare.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pasteshare")
public record PasteShareProperties(
        @NotBlank String publicBaseUrl,
        @NotBlank String allowedOrigin,
        @Min(1) int maxContentLength,
        @Min(1) int idGenerationAttempts) {
}
