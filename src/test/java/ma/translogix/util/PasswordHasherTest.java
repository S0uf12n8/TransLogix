package ma.translogix.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void leHashEstAuFormatPbkdf2AvecSesIterationsEtNeContientPasLeMotDePasse() {
        String hash = PasswordHasher.hash("Secret2026!", 1_000);

        String[] parts = hash.split("\\$");
        assertEquals(4, parts.length, hash);
        assertEquals("pbkdf2-sha512", parts[0]);
        assertEquals("1000", parts[1]);
        assertEquals(16, Base64.getDecoder().decode(parts[2]).length, "sel de 16 octets");
        assertEquals(64, Base64.getDecoder().decode(parts[3]).length, "hash de 512 bits");
        assertFalse(hash.contains("Secret2026!"));
    }

    @Test
    void bonMotDePasseAccepteMauvaisRefuse() {
        String hash = PasswordHasher.hash("Secret2026!");

        assertTrue(PasswordHasher.verifier("Secret2026!", hash));
        assertFalse(PasswordHasher.verifier("secret2026!", hash));
        assertFalse(PasswordHasher.verifier("", hash));
        assertFalse(PasswordHasher.verifier(null, hash));
    }

    @Test
    void memeMotDePasseDonneDeuxHashDifferentsGraceAuSel() {
        assertNotEquals(PasswordHasher.hash("Secret2026!"), PasswordHasher.hash("Secret2026!"));
    }

    @Test
    void lesIterationsStockeesSontRelues() {
        String hash = PasswordHasher.hash("Secret2026!", 1_234);

        assertTrue(PasswordHasher.verifier("Secret2026!", hash));
    }

    @Test
    void parDefautOnUtiliseLes210000IterationsOwasp() {
        String avant = System.clearProperty(PasswordHasher.PROPRIETE_ITERATIONS);
        try {
            assertEquals(210_000, PasswordHasher.iterations());
        } finally {
            if (avant != null) {
                System.setProperty(PasswordHasher.PROPRIETE_ITERATIONS, avant);
            }
        }
    }

    @Test
    void unHashDeLIteration2EstEncoreAccepteMaisAMettreAJour() throws Exception {
        String ancien = ancienHash("admin1234");

        assertTrue(PasswordHasher.verifier("admin1234", ancien));
        assertFalse(PasswordHasher.verifier("autre", ancien));
        assertTrue(PasswordHasher.aMettreAJour(ancien));
    }

    @Test
    void unHashAvecMoinsDIterationsQuAujourdhuiEstAMettreAJour() {
        int aujourdhui = PasswordHasher.iterations();

        assertTrue(PasswordHasher.aMettreAJour(PasswordHasher.hash("x", aujourdhui - 1)));
        assertFalse(PasswordHasher.aMettreAJour(PasswordHasher.hash("x", aujourdhui)));
    }

    @Test
    void unHashMalFormeNeCorrespondJamaisEtNeLevePasDErreur() {
        assertFalse(PasswordHasher.verifier("x", "pbkdf2-sha512$abc$%%%$***"));
        assertFalse(PasswordHasher.verifier("x", "pbkdf2-sha512$10"));
        assertFalse(PasswordHasher.verifier("x", "pas-un-hash"));
        assertFalse(PasswordHasher.verifier("x", "!!:??"));
        assertTrue(PasswordHasher.aMettreAJour("pbkdf2-sha512$abc$x$y"));
    }

    @Test
    void laVerificationFacticeRefuseToujours() {
        assertFalse(PasswordHasher.verifierFactice("compte-inexistant"));
        assertFalse(PasswordHasher.verifierFactice(null));
    }

    /** The iteration-2 format: Base64(salt) + ":" + Base64(SHA-256(salt + password)). */
    private static String ancienHash(String motDePasse) throws Exception {
        byte[] sel = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(sel);
        byte[] hash = md.digest(motDePasse.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sel) + ":" + Base64.getEncoder().encodeToString(hash);
    }
}
