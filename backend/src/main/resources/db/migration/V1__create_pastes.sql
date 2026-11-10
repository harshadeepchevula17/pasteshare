CREATE TABLE pastes (
    id          varchar(22) PRIMARY KEY,
    content     text NOT NULL,
    created_at  timestamptz NOT NULL,
    expires_at  timestamptz NULL,
    max_views   integer NULL,
    view_count  bigint NOT NULL DEFAULT 0,

    CONSTRAINT pastes_id_length CHECK (char_length(id) = 22),
    CONSTRAINT pastes_content_nonempty CHECK (char_length(content) > 0),
    CONSTRAINT pastes_max_views_positive CHECK (max_views IS NULL OR max_views > 0),
    CONSTRAINT pastes_view_count_nonnegative CHECK (view_count >= 0),
    CONSTRAINT pastes_view_count_within_limit
        CHECK (max_views IS NULL OR view_count <= max_views)
);
