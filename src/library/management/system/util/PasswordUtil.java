package library.management.system.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class PasswordUtil {

    private static final int SALT_LENGTH = 16;

    private PasswordUtil() {
        // Utility class
    }

    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return bytesToHex(salt);
    }

    /*
     * Passwords are stored as SHA-256(salt + password).
     * The salt is stored separately in the database.
     */
    public static String hashPassword(String password, String salt) {
        if (password == null || salt == null) {
            throw new IllegalArgumentException("Password and salt cannot be null.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            String saltedPassword = salt + password;

            byte[] hash = digest.digest(
                    saltedPassword.getBytes(StandardCharsets.UTF_8)
            );

            return bytesToHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available.",
                    ex
            );
        }
    }

    public static boolean verifyPassword(
            String password,
            String salt,
            String expectedHash) {

        if (password == null || salt == null || expectedHash == null) {
            return false;
        }

        String actualHash = hashPassword(password, salt);

        return MessageDigest.isEqual(
                actualHash.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }
}