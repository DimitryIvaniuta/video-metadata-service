package com.github.dimitryivaniuta.videometadata.model;

/**
 * User account status, stored in DB as ordinal (0,1,…).
 */
public enum UserStatus {
    ACTIVE,        // 0
    SUSPENDED,     // 1
    DEACTIVATED    // 2  (add more as needed)
}