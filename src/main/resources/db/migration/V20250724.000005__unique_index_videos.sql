-- Prevent duplicates by (source, external_video_id)
ALTER TABLE videos
    ADD CONSTRAINT uq_videos_source_external UNIQUE(source, external_video_id);