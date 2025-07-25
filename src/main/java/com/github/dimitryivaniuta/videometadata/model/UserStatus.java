package com.github.dimitryivaniuta.videometadata.model;

/**
 * User account status, stored in DB as ordinal (0,1,…).
 */
public enum UserStatus {
    ACTIVE((short)0),        // 0
    SUSPENDED((short)1),     // 1
    DEACTIVATED((short)2);    // 2  (add more as needed)

    private final short code;
    UserStatus(short code) { this.code = code; }
    public short getCode()  { return code; }

    public static UserStatus fromCode(short code) {
        for (UserStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown UserStatus code: " + code);
    }

}