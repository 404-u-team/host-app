package dev.hostapp.backend.security;

import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class Passwords {
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    private static final Argon2PasswordEncoder LEGACY = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private static final String LONG_PREFIX = "{bcrypt-sha256}";
    private static final String DUMMY_HASH = BCRYPT.encode(Tokens.random());

    private Passwords() {}

    public static String encode(String raw) {
        if (bytes(raw) > 72) {
            return LONG_PREFIX + BCRYPT.encode(Tokens.hash(raw));
        }
        return BCRYPT.encode(raw);
    }

    public static boolean matches(String raw, String hash) {
        if (raw == null || raw.length() > 255) {
            return false;
        }
        if (hash == null || !hash.startsWith("$2") && !hash.startsWith("$argon2") && !hash.startsWith(LONG_PREFIX)) {
            BCRYPT.matches(Tokens.hash(raw), DUMMY_HASH);
            return false;
        }
        if (hash.startsWith("$argon2")) {
            return LEGACY.matches(raw, hash);
        }
        if (hash.startsWith(LONG_PREFIX)) {
            return BCRYPT.matches(Tokens.hash(raw), hash.substring(LONG_PREFIX.length()));
        }
        return bytes(raw) <= 72 && BCRYPT.matches(raw, hash);
    }

    public static void validateNew(String password) {
        if (password == null || password.isBlank() || password.length() < 12 || bytes(password) > 72) {
            throw new IllegalArgumentException("Новый пароль должен содержать от 12 символов и занимать не более 72 байт UTF-8");
        }
    }

    private static int bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
