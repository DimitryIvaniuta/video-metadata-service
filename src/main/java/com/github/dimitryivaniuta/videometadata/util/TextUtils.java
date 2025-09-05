package com.github.dimitryivaniuta.videometadata.util;

import java.text.Normalizer;

/** Small helpers for accent-insensitive matching. */
public final class TextUtils {
    private TextUtils() {}

    public static String foldToAsciiLower(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", ""); // strip diacritics
        return n.toLowerCase();
    }
}
