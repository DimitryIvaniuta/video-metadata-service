package com.github.dimitryivaniuta.videometadata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Logs raw vs hashed passwords at DEBUG level for troubleshooting.
 */
public final class LoggingPasswordEncoder implements PasswordEncoder {
    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordEncoder.class);
    private final PasswordEncoder delegate = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        String hash = delegate.encode(rawPassword);
        log.debug("BCrypt encode: raw='{}' → hash='{}'", rawPassword, hash);
        return hash;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        boolean ok = delegate.matches(rawPassword, encodedPassword);
        log.debug("BCrypt match: raw='{}' against='{}' → {}", rawPassword, encodedPassword, ok);
        return ok;
    }
}