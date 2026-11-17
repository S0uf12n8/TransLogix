package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.ResultatAudit;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditServiceTest {

    /** A clock the test can move forward. */
    private static final class HorlogeReglable extends Clock {
        private Instant maintenant = Instant.parse("2026-11-15T09:30:00Z");

        void avancerMinutes(long minutes) {
            maintenant = maintenant.plusSeconds(minutes * 60);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return maintenant; }
    }

    private HorlogeReglable horloge;
    private AtomicReference<String> acteur;
    private AuditService service;

    @BeforeEach
    void setUp() {
        horloge = new HorlogeReglable();
        acteur = new AtomicReference<>("Sara Alami (Gérant)");
        service = new AuditService(new InMemoryAuditDao(), horloge, acteur::get);
    }

    @Test
    void enregistrerStockeHorodatageActeurEtChamps() {
        EvenementAudit e = service.enregistrer(" Utilisateurs ", " Création du compte x ", ResultatAudit.REUSSI);

        assertNotNull(e.getId());
        assertEquals(LocalDateTime.of(2026, 11, 15, 9, 30), e.getHorodatage());
        assertEquals("Sara Alami (Gérant)", e.getUtilisateur());
        assertEquals("Utilisateurs", e.getModule());
        assertEquals("Création du compte x", e.getAction());
        assertEquals(ResultatAudit.REUSSI, e.getResultat());
    }

    @Test
    void lActeurEstLuAuMomentDeLEnregistrement() {
        service.enregistrer("Utilisateurs", "a", ResultatAudit.REUSSI);
        acteur.set("Système");
        service.enregistrer("Utilisateurs", "b", ResultatAudit.REUSSI);

        assertEquals("Système", service.lister().get(0).getUtilisateur());
        assertEquals("Sara Alami (Gérant)", service.lister().get(1).getUtilisateur());
    }

    @Test
    void acteurVideDevientInconnu() {
        acteur.set(" ");
        assertEquals("Inconnu", service.enregistrer("M", "A", ResultatAudit.REUSSI).getUtilisateur());
    }

    @Test
    void listerRenvoieLePlusRecentDAbord() {
        service.enregistrer("Utilisateurs", "premier", ResultatAudit.REUSSI);
        horloge.avancerMinutes(5);
        service.enregistrer("Utilisateurs", "deuxieme", ResultatAudit.REUSSI);
        service.enregistrer("Utilisateurs", "troisieme meme minute", ResultatAudit.REUSSI);

        List<String> actions = service.lister().stream().map(EvenementAudit::getAction).toList();
        assertEquals(List.of("troisieme meme minute", "deuxieme", "premier"), actions);
    }

    @Test
    void champsObligatoires() {
        assertThrows(ValidationException.class, () -> service.enregistrer(" ", "action", ResultatAudit.REUSSI));
        assertThrows(ValidationException.class, () -> service.enregistrer("module", null, ResultatAudit.REUSSI));
        assertThrows(ValidationException.class, () -> service.enregistrer("module", "action", null));
        assertTrue(service.lister().isEmpty());
    }

    @Test
    void rechercherIgnoreCasseEtAccents() {
        service.enregistrer("Utilisateurs", "Création du compte sara@translogix.ma", ResultatAudit.REUSSI);
        service.enregistrer("Utilisateurs", "Suppression refusée : dernier gérant", ResultatAudit.REFUSE);

        assertEquals(1, service.rechercher("CREATION").size());
        assertEquals(1, service.rechercher("DERNIER gerant").size());
        assertEquals(1, service.rechercher("refuse").size());
        assertEquals(2, service.rechercher("utilisateurs").size());
        assertEquals(2, service.rechercher("sara alami").size());
        assertEquals(2, service.rechercher("15/11/2026").size());
        assertEquals(0, service.rechercher("facture").size());
    }

    @Test
    void rechercheVideRenvoieTout() {
        service.enregistrer("Utilisateurs", "a", ResultatAudit.REUSSI);
        service.enregistrer("Utilisateurs", "b", ResultatAudit.REUSSI);

        assertEquals(2, service.rechercher("").size());
        assertEquals(2, service.rechercher(null).size());
    }
}
