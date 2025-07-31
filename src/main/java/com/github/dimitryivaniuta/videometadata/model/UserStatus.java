package com.github.dimitryivaniuta.videometadata.model;

import lombok.Getter;

/**
 * User account status, stored in DB as ordinal (0,1,…).
 */
@Getter
public enum UserStatus {
    ACTIVE((short)0),        // 0
    SUSPENDED((short)1),     // 1
    DEACTIVATED((short)2),    // 2  (add more as needed)
    LOCKED((short)3),
    PENDING((short)4);

    private final short code;
    UserStatus(short code) { this.code = code; }

    public static UserStatus fromCode(short code) {
        for (UserStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown UserStatus code: " + code);
    }

}