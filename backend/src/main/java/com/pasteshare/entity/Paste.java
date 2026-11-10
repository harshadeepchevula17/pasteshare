package com.pasteshare.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pastes")
public class Paste {

    @Id
    @Column(length = 22, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "expires_at", columnDefinition = "timestamptz")
    private Instant expiresAt;

    @Column(name = "max_views")
    private Integer maxViews;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    protected Paste() {
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Integer getMaxViews() {
        return maxViews;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void incrementViewCount() {
        viewCount++;
    }
}
