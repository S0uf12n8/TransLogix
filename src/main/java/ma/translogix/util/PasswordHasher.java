package ma.translogix.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing with PBKDF2-HMAC-SHA512 (built into the JDK, no extra dependency), 210 000 iterations and a
 * random 16-byte salt per password, as recommended by the OWASP Password Storage Cheat Sheet.
 * <p>
 * Stored format: {@code pbkdf2-sha512$<iterations>$<salt Base64>$<hash Base64>}. The iteration count is stored with
 * each hash, so it can be raised later without breaking existing passwords.
 * <p>
 * Hashes of iteration 2 (one salted SHA-256, format {@code salt:hash}) are still accepted, and
 * {@link #aMettreAJour(String)} tells AuthService to replace them at the next successful login.
 * <p>
 * The iteration count can be lowered with the system property {@value #PROPRIETE_ITERATIONS}: only the test build
 * does that (pom.xml, surefire), so the hundreds of accounts created by the tests stay fast.
 */
public final class PasswordHasher {

    public static final String PROPRIETE_ITERATIONS = "translogix.hash.iterations";
    static final int ITERATIONS_PAR_DEFAUT = 210_000;
    static final String PREFIXE = "pbkdf2-sha512";

    private static final String ALGORITHME = "PBKDF2WithHmacSHA512";
    private static final int TAILLE_SEL = 16;
    private static final int TAILLE_HASH_BITS = 512;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Verified when the email is unknown, so an unknown email takes as long as a wrong password. */
    private static final class Factice {
        static final String HASH = hash("compte-inexistant"); // computed on first use only
    }

    private PasswordHasher() {
    }

    public static String hash(String motDePasse) {
        return hash(motDePasse, iterations());
    }

    static String hash(String motDePasse, int iterations) {
        if (motDePasse == null) {
            throw new IllegalArgumentException("Mot de passe absent");
        }
        byte[] sel = new byte[TAILLE_SEL];
        RANDOM.nextBytes(sel);
        return PREFIXE + "$" + iterations + "$" + encode(sel) + "$" + encode(pbkdf2(motDePasse, sel, iterations));
    }

    /** True when the password matches the stored hash (current or iteration-2 format). Constant-time compare. */
    public static boolean verifier(String motDePasse, String stocke) {
        if (motDePasse == null || stocke == null) {
            return false;
        }
        try {
            if (stocke.startsWith(PREFIXE + "$")) {
                String[] parts = stocke.split("\\$");
                if (parts.length != 4) {
                    return false;
                }
                int iterations = Integer.parseInt(parts[1]);
                byte[] sel = decode(parts[2]);
                byte[] attendu = decode(parts[3]);
                return MessageDigest.isEqual(attendu, pbkdf2(motDePasse, sel, iterations));
            }
            String[] parts = stocke.split(":");
            if (parts.length != 2) {
                return false;
            }
            return MessageDigest.isEqual(decode(parts[1]), ancienSha256(decode(parts[0]), motDePasse));
        } catch (IllegalArgumentException e) { // malformed Base64 or iteration count: never a match
            return false;
        }
    }

    /** Burns the same time as a real check; used when the account does not exist. Always false. */
    public static boolean verifierFactice(String motDePasse) {
        verifier(motDePasse == null ? "" : motDePasse, Factice.HASH);
        return false;
    }

    /** True for an iteration-2 hash or a hash with fewer iterations than today: rehash it after a login. */
    public static boolean aMettreAJour(String stocke) {
        if (stocke == null || !stocke.startsWith(PREFIXE + "$")) {
            return true;
        }
        String[] parts = stocke.split("\\$");
        try {
            return parts.length != 4 || Integer.parseInt(parts[1]) < iterations();
        } catch (NumberFormatException e) {
            return true;
        }
    }

    static int iterations() {
        String valeur = System.getProperty(PROPRIETE_ITERATIONS);
        if (valeur == null || valeur.isBlank()) {
            return ITERATIONS_PAR_DEFAUT;
        }
        int iterations = Integer.parseInt(valeur.trim());
        if (iterations < 1) {
            throw new IllegalStateException(PROPRIETE_ITERATIONS + " doit être positif");
        }
        return iterations;
    }

    private static byte[] pbkdf2(String motDePasse, byte[] sel, int iterations) {
        KeySpec spec = new PBEKeySpec(motDePasse.toCharArray(), sel, iterations, TAILLE_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHME).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(ALGORITHME + " indisponible", e);
        }
    }

    /** Iteration 2 format, only to check old hashes until they are replaced. */
    private static byte[] ancienSha256(byte[] sel, String motDePasse) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(sel);
            return md.digest(motDePasse.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private static String encode(byte[] octets) {
        return Base64.getEncoder().encodeToString(octets);
    }

    private static byte[] decode(String texte) {
        return Base64.getDecoder().decode(texte);
    }
}
