package org.aueb.util;

/**
 * Utility class for performing secure password hashing and verification using BCrypt.
 */
public final class PasswordEncoder {

    /** The BCrypt work factor (cost). Higher value means slower execution,
    * which increases security against brute-force attacks. 10-12 is common.
     */
    private static final int LOG_ROUNDS = 12;

    private PasswordEncoder() {
        /** Prevent instantiation  */
    }

    /**
     * Hashes a clear-text password using the BCrypt algorithm.
     * @param rawPassword The clear-text password to hash.
     * @return The resulting BCrypt hash string.
     */
    public static String encode(String rawPassword) {
        /** The BCrypt class handles salting automatically within gensalt() and hashpw().  */
        return org.mindrot.jbcrypt.BCrypt.hashpw(rawPassword, org.mindrot.jbcrypt.BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * Checks a clear-text password against a stored BCrypt hash.
     * @param rawPassword The clear-text password provided for verification.
     * @param encodedPassword The stored BCrypt hash.
     * @return true if the passwords match, false otherwise.
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return org.mindrot.jbcrypt.BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
