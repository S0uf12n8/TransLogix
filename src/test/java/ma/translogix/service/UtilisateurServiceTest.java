package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.ResultatAudit;
import ma.translogix.model.Role;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.PasswordHasher;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UtilisateurServiceTest {

    private UtilisateurService service;
    private AuditService audit;

    @BeforeEach
    void setUp() {
        Clock horloge = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
        audit = new AuditService(new InMemoryAuditDao(), horloge, () -> "Testeur (Gérant)");
        service = new UtilisateurService(new InMemoryUtilisateurDao(), audit);
    }

    @Test
    void creerUtilisateurValideLeSauvegardeAvecMotDePasseHache() {
        Utilisateur u = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        assertNotNull(u.getId());
        assertNotEquals("motdepasse1", u.getMotDePasseHash());
        assertTrue(PasswordHasher.verifier("motdepasse1", u.getMotDePasseHash()));
    }

    @Test
    void emailDejaUtiliseEstRefuseMemeAvecMajuscules() {
        service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        assertThrows(ValidationException.class,
                () -> service.creer("Bennani", "Ali", "SARA@translogix.ma", "motdepasse2", Role.RH));
    }

    @Test
    void emailInvalideEstRefuse() {
        assertThrows(ValidationException.class,
                () -> service.creer("Bennani", "Ali", "pas-un-email", "motdepasse2", Role.RH));
    }

    @Test
    void motDePasseTropCourtEstRefuse() {
        assertThrows(ValidationException.class,
                () -> service.creer("Bennani", "Ali", "ali@translogix.ma", "court", Role.RH));
    }

    @Test
    void dernierGerantNePeutPasEtreSupprime() {
        Utilisateur gerant = service.creer("Principal", "Admin", "admin@translogix.ma", "motdepasse1", Role.GERANT);

        assertThrows(ValidationException.class, () -> service.supprimer(gerant.getId()));
    }

    @Test
    void modifierMetAJourLesChamps() {
        Utilisateur u = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        service.modifier(u.getId(), "Alami", "Sara", "sara@translogix.ma", Role.COMPTABLE, false);

        Utilisateur modifie = service.lister().get(0);
        assertEquals(Role.COMPTABLE, modifie.getRole());
        assertEquals(false, modifie.isActif());
    }

    // ---------------------------------------------------------------- audit

    @Test
    void creationEstJournalisee() {
        service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        List<EvenementAudit> journal = audit.lister();
        assertEquals(1, journal.size());
        EvenementAudit e = journal.get(0);
        assertEquals(UtilisateurService.MODULE_AUDIT, e.getModule());
        assertEquals("Testeur (Gérant)", e.getUtilisateur());
        assertTrue(e.getAction().contains("Création"));
        assertTrue(e.getAction().contains("sara@translogix.ma"));
        assertEquals(ResultatAudit.REUSSI, e.getResultat());
    }

    @Test
    void modificationEtSuppressionSontJournalisees() {
        Utilisateur u = service.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);

        service.modifier(u.getId(), "Alami", "Sara", "sara@translogix.ma", Role.RH, true);
        service.supprimer(u.getId());

        List<EvenementAudit> journal = audit.lister(); // most recent first
        assertEquals(3, journal.size());
        assertTrue(journal.get(0).getAction().startsWith("Suppression du compte"));
        assertTrue(journal.get(1).getAction().startsWith("Modification du compte"));
        assertTrue(journal.stream().allMatch(e -> e.getResultat() == ResultatAudit.REUSSI));
    }

    @Test
    void suppressionRefuseeEstJournaliseeCommeRefusee() {
        Utilisateur gerant = service.creer("Principal", "Admin", "admin@translogix.ma", "motdepasse1", Role.GERANT);

        assertThrows(ValidationException.class, () -> service.supprimer(gerant.getId()));

        EvenementAudit dernier = audit.lister().get(0);
        assertEquals(ResultatAudit.REFUSE, dernier.getResultat());
        assertTrue(dernier.getAction().contains("dernier gérant"));
        assertEquals(1, service.lister().size());
    }

    @Test
    void erreurDeSaisieNEstPasJournalisee() {
        assertThrows(ValidationException.class,
                () -> service.creer("Bennani", "Ali", "pas-un-email", "motdepasse2", Role.RH));

        assertTrue(audit.lister().isEmpty());
    }
}
