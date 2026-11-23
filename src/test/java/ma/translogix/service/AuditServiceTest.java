package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

    // ------------------------------------------------------------------ multi-criteria search

    /** 15/11 09:30 user created, 15/11 10:30 refusal, 16/11 09:30 invoice paid. */
    private void troisEvenements() {
        service.enregistrer("Utilisateurs", "Création du compte sara@translogix.ma", ResultatAudit.REUSSI);
        horloge.avancerMinutes(60);
        service.enregistrer("Utilisateurs", "Suppression refusée : compte Gérant", ResultatAudit.REFUSE);
        horloge.avancerMinutes(23 * 60);
        service.enregistrer("Factures", "Paiement de FACT-2026-001", ResultatAudit.REUSSI);
    }

    private List<String> actions(AuditService.CritereAudit critere) {
        return service.filtrer(critere).stream().map(EvenementAudit::getAction).toList();
    }

    @Test
    void criteresAuditSeCombinentEnEt() {
        troisEvenements();

        assertEquals(List.of("Suppression refusée : compte Gérant", "Création du compte sara@translogix.ma"),
                actions(new AuditService.CritereAudit(null, "Utilisateurs", null, null, null)));
        assertEquals(List.of("Suppression refusée : compte Gérant"),
                actions(new AuditService.CritereAudit(null, "Utilisateurs", ResultatAudit.REFUSE, null, null)));
        assertEquals(List.of("Création du compte sara@translogix.ma"),
                actions(new AuditService.CritereAudit("sara@", null, ResultatAudit.REUSSI, null, null)));
        assertEquals(List.of(), actions(new AuditService.CritereAudit("sara@", "Factures", null, null, null)));
    }

    @Test
    void periodeAuditBornesIncluses() {
        troisEvenements();
        LocalDate le15 = LocalDate.of(2026, 11, 15);
        LocalDate le16 = LocalDate.of(2026, 11, 16);

        assertEquals(2, actions(new AuditService.CritereAudit(null, null, null, le15, le15)).size());
        assertEquals(List.of("Paiement de FACT-2026-001"),
                actions(new AuditService.CritereAudit(null, null, null, le16, null)));
        assertEquals(3, actions(new AuditService.CritereAudit(null, null, null, null, le16)).size());
    }

    @Test
    void periodeAuditInverseeEstRefusee() {
        AuditService.CritereAudit inverse = new AuditService.CritereAudit(null, null, null,
                LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 15));

        assertThrows(ValidationException.class, () -> service.filtrer(inverse));
    }

    @Test
    void modulesDistinctsParOrdreAlphabetique() {
        troisEvenements();

        assertEquals(List.of("Factures", "Utilisateurs"), service.modules());
        assertEquals(3, service.filtrer(null).size());
    }
}
