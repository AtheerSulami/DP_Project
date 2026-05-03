package phase3;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * PasswordUtil – lightweight SHA-256 hashing for faculty passwords.
 * In a production system you would use BCrypt; SHA-256 is used here
 * to keep the project dependency-free.
 */
public class PasswordUtil {

    private PasswordUtil() {}

    public static String hash(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(plainText.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in standard Java
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
