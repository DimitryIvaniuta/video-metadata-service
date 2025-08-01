package com.github.dimitryivaniuta.videometadata.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class DateTimeUtil {

    private DateTimeUtil() {
        //
    }

    public static Instant toInstant(OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : null;
    }

    public static OffsetDateTime toOffset(Instant i) {
        return (i == null) ? null : OffsetDateTime.ofInstant(i, ZoneOffset.UTC);
    }
}
