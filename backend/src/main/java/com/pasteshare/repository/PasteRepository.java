package com.pasteshare.repository;

import java.time.Instant;
import java.util.Optional;

import com.pasteshare.entity.Paste;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasteRepository extends JpaRepository<Paste, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select paste from Paste paste where paste.id = :id")
    Optional<Paste> findByIdForUpdate(@Param("id") String id);

    @Modifying
    @Query(value = """
            INSERT INTO pastes (id, content, created_at, expires_at, max_views, view_count)
            VALUES (:id, :content, :createdAt, :expiresAt, :maxViews, 0)
            ON CONFLICT (id) DO NOTHING
            """, nativeQuery = true)
    int insertIfIdAvailable(
            @Param("id") String id,
            @Param("content") String content,
            @Param("createdAt") Instant createdAt,
            @Param("expiresAt") Instant expiresAt,
            @Param("maxViews") Integer maxViews);
}
