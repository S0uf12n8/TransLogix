package ma.translogix.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Salted SHA-256. Good enough for iteration 2; replace with BCrypt later. Stored format: salt:hash (Base64). */
public final class PasswordHasher {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String motDePasse) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return encode(salt) + ":" + encode(digest(salt, motDePasse));
    }

    public static boolean verifier(String motDePasse, String stocke) {
        if (motDePasse == null || stocke == null) {
            return false;
        }
        String[] parts = stocke.split(":");
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] attendu = Base64.getDecoder().decode(parts[1]);
        return MessageDigest.isEqual(attendu, digest(salt, motDePasse));
    }

    private static byte[] digest(byte[] salt, String motDePasse) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            return md.digest(motDePasse.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
