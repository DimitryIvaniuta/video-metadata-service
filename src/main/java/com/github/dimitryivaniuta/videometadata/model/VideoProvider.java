package com.github.dimitryivaniuta.videometadata.model;

import lombok.Getter;

/**
 * Video provider enumeration.
 * NOTE: Persisted by ORDINAL. **Never reorder or insert in the middle.**
 * Append new values ONLY at the end.
 */
@Getter
public enum VideoProvider {
    YOUTUBE(1),
    VIMEO(2),
    DAILYMOTION(3),
    INTERNAL(4),
    OTHER(5),
    UNSPECIFIED(6);

    @Getter
    private final int code;

    VideoProvider(int code) {
        this.code = code;
    }

    /**
     * Lookup by numeric code.
     * @param code the SMALLINT from the DB
     * @return matching provider or UNSPECIFIED
     */
    public static VideoProvider fromCode(int code) {
        for (VideoProvider p: values()) {
            if (p.code == code) {
                return p;
            }
        }
        return UNSPECIFIED;
    }
}