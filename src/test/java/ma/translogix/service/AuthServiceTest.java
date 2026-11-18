package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private UtilisateurService utilisateurs;
    private AuthService auth;
    private Utilisateur sara;

    @BeforeEach
    void setUp() {
        Clock horloge = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneOffset.UTC);
        InMemoryUtilisateurDao dao = new InMemoryUtilisateurDao();
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
    void champsVidesSontRefuses() {
        assertThrows(ValidationException.class, () -> auth.connecter("", "motdepasse1"));
        assertThrows(ValidationException.class, () -> auth.connecter("sara@translogix.ma", ""));
        assertThrows(ValidationException.class, () -> auth.connecter(null, null));
    }
}
