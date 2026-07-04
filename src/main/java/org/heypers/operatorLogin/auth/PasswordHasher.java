package org.heypers.operatorLogin.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;


public final class PasswordHasher {
    private static final String PREFIX = "PBKDF2:";
    private static final String LEGACY_PREFIX = "HASH:";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derivedKey = pbkdf2(password, salt, ITERATIONS);
        return PREFIX + ITERATIONS + ':' + Base64.getEncoder().encodeToString(salt) + ':'
                + Base64.getEncoder().encodeToString(derivedKey);
    }


    public static boolean isModernHash(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public static boolean isLegacyHash(String stored) {
        return stored != null && stored.startsWith(LEGACY_PREFIX);
    }

    public static boolean matches(String password, String stored) {
        if (stored == null || stored.isBlank()) {
            return false;
        }
        if (isModernHash(stored)) {
            return matchesModern(password, stored);
        }
        if (isLegacyHash(stored)) {
            return MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8), legacyHash(password).getBytes(StandardCharsets.UTF_8));
        }
        return MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8), password.getBytes(StandardCharsets.UTF_8));
    }


    private static boolean matchesModern(String password, String stored) {
        String[] parts = stored.split(":", 4);
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String legacyHash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return LEGACY_PREFIX + Base64.getEncoder().encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash password", exception);
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash password", exception);
        }
    }
}