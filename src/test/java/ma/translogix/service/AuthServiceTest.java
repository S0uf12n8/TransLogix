package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.PasswordHasher;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private InMemoryUtilisateurDao dao;
    private UtilisateurService utilisateurs;
    private AuthService auth;
    private Utilisateur sara;

    @BeforeEach
    void setUp() {
        Clock horloge = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneOffset.UTC);
        dao = new InMemoryUtilisateurDao();
        utilisateurs = new UtilisateurService(dao, new AuditService(new InMemoryAuditDao(), horloge, () -> "Test"));
        auth = new AuthService(dao);
        sara = utilisateurs.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);
    }

    @Test
    void bonMotDePasseRenvoieLeCompte() {
        Utilisateur connecte = auth.connecter("sara@translogix.ma", "motdepasse1");

        assertEquals(sara.getId(), connecte.getId());
        assertEquals(Role.DISPATCHER, connecte.getRole());
    }

    @Test
    void emailEnMajusculesEtEspacesEstAccepte() {
        assertEquals(sara.getId(), auth.connecter("  SARA@Translogix.ma ", "motdepasse1").getId());
    }

    @Test
    void mauvaisMotDePasseEstRefuseAvecLeMessageGenerique() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> auth.connecter("sara@translogix.ma", "motdepasse2"));
        assertEquals("Email ou mot de passe incorrect.", e.getMessage());
    }

    @Test
    void emailInconnuEstRefuseAvecLeMemeMessage() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> auth.connecter("inconnu@translogix.ma", "motdepasse1"));
        assertEquals(AuthService.ECHEC, e.getMessage());
    }

    @Test
    void compteDesactiveEstRefuseMemeAvecLeBonMotDePasse() {
        utilisateurs.modifier(sara.getId(), "Alami", "Sara", "sara@translogix.ma", Role.DISPATCHER, false);

        ValidationException e = assertThrows(ValidationException.class,
                () -> auth.connecter("sara@translogix.ma", "motdepasse1"));
        assertEquals(AuthService.ECHEC, e.getMessage());
    }

    @Test
    void unAncienHashSha256EstRemplaceParPbkdf2ALaConnexion() throws Exception {
        sara.setMotDePasseHash(ancienHash("motdepasse1"));
        dao.update(sara);

        auth.connecter("sara@translogix.ma", "motdepasse1");

        String nouveau = dao.findById(sara.getId()).orElseThrow().getMotDePasseHash();
        assertTrue(nouveau.startsWith("pbkdf2-sha512$"), nouveau);
        assertTrue(PasswordHasher.verifier("motdepasse1", nouveau));
        assertEquals(sara.getId(), auth.connecter("sara@translogix.ma", "motdepasse1").getId());
    }

    @Test
    void unMauvaisMotDePasseNeTouchePasAuHash() throws Exception {
        String ancien = ancienHash("motdepasse1");
        sara.setMotDePasseHash(ancien);
        dao.update(sara);

        assertThrows(ValidationException.class, () -> auth.connecter("sara@translogix.ma", "faux"));

        assertEquals(ancien, dao.findById(sara.getId()).orElseThrow().getMotDePasseHash());
    }

    @Test
    void unHashAJourNEstPasReecritAChaqueConnexion() {
        String avant = dao.findById(sara.getId()).orElseThrow().getMotDePasseHash();

        auth.connecter("sara@translogix.ma", "motdepasse1");

        assertEquals(avant, dao.findById(sara.getId()).orElseThrow().getMotDePasseHash());
    }

    /** The iteration-2 format: Base64(salt) + ":" + Base64(SHA-256(salt + password)). */
    private static String ancienHash(String motDePasse) throws Exception {
        byte[] sel = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(sel);
        byte[] hash = md.digest(motDePasse.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sel) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    @Test
    void champsVidesSontRefuses() {
        assertThrows(ValidationException.class, () -> auth.connecter("", "motdepasse1"));
        assertThrows(ValidationException.class, () -> auth.connecter("sara@translogix.ma", ""));
        assertThrows(ValidationException.class, () -> auth.connecter(null, null));
    }
}
