package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import ma.translogix.dao.memory.InMemoryCongeDao;
import ma.translogix.dao.memory.InMemoryEmployeDao;
import ma.translogix.model.DemandeConge;
import ma.translogix.model.Employe;
import ma.translogix.model.Poste;
import ma.translogix.model.StatutConge;
import ma.translogix.model.TypeConge;
import ma.translogix.model.TypeContrat;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Fictional identifiers only (no real CIN / CNSS in tests). */
class CongeServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate LUNDI = LocalDate.of(2026, 11, 16);
    private static final LocalDate VENDREDI = LocalDate.of(2026, 11, 20);
    private static final LocalDate SAMEDI = LocalDate.of(2026, 11, 21);
    private static final LocalDate DIMANCHE = LocalDate.of(2026, 11, 22);
    private static final LocalDate LUNDI_SUIVANT = LocalDate.of(2026, 11, 23);

    private EmployeService employes;
    private CongeService service;

    @BeforeEach
    void setUp() {
        employes = new EmployeService(new InMemoryEmployeDao(), HORLOGE);
        service = new CongeService(new InMemoryCongeDao(), employes, HORLOGE);
    }

    private Employe employe(String nom, String cin) {
        return employes.creer(nom, "Test", cin, "100200300", Poste.CHAUFFEUR, TypeContrat.CDI,
                LocalDate.of(2021, 3, 15));
    }

    private DemandeConge demander(Employe e, LocalDate debut, LocalDate fin) {
        return service.creer(e.getId(), TypeConge.PAYE, debut, fin, "Congé annuel");
    }

    @Test
    void listeVideAuDemarrage() {
        assertTrue(service.lister().isEmpty());
        assertEquals(0, service.aValider());
        assertEquals(new CongeService.Compteurs(0, 0, 0), service.compteurs());
    }

    @Test
    void creerInitialiseEnAttenteEtCompteLesJoursOuvres() {
        Employe e = employe("Alpha", "test-0001");
        DemandeConge d = demander(e, LUNDI, VENDREDI);

        assertEquals(StatutConge.EN_ATTENTE, d.getStatut());
        assertEquals(5, d.getNbJours());
        assertEquals(LUNDI, d.getDateDebut());
        assertEquals(VENDREDI, d.getDateFin());
        assertEquals(LUNDI, d.getDateDemande());
        assertEquals(TypeConge.PAYE, d.getType());
        assertEquals(e.getId(), d.getEmployeId());
        assertEquals(1, service.aValider());
    }

    @Test
    void motifFacultatifEstStockeSansEspaces() {
        DemandeConge d = service.creer(employe("Alpha", "test-0001").getId(), TypeConge.MALADIE,
                LUNDI, VENDREDI, "  Justificatif  ");
        assertEquals("Justificatif", d.getMotif());

        DemandeConge sans = service.creer(employe("Beta", "test-0002").getId(), TypeConge.SANS_SOLDE,
                LUNDI, VENDREDI, "  ");
        assertNull(sans.getMotif());
    }

    @Test
    void unWeekendSeulEstRefuse() {
        Employe e = employe("Alpha", "test-0001");
        assertThrows(ValidationException.class, () -> demander(e, SAMEDI, DIMANCHE));
        assertTrue(service.lister().isEmpty());
    }

    @Test
    void unEmployeInactifOuInconnuEstRefuse() {
        Employe e = employe("Alpha", "test-0001");
        employes.desactiver(e.getId());
        assertThrows(ValidationException.class, () -> demander(e, LUNDI, VENDREDI));
        assertThrows(ValidationException.class,
                () -> service.creer(999L, TypeConge.PAYE, LUNDI, VENDREDI, null));
    }

    @Test
    void lesDatesEtLeTypeSontVerifies() {
        Employe e = employe("Alpha", "test-0001");
        assertThrows(ValidationException.class,
                () -> service.creer(e.getId(), null, LUNDI, VENDREDI, null));
        assertThrows(ValidationException.class,
                () -> service.creer(e.getId(), TypeConge.PAYE, null, VENDREDI, null));
        assertThrows(ValidationException.class,
                () -> service.creer(e.getId(), TypeConge.PAYE, LUNDI, null, null));
        assertThrows(ValidationException.class, () -> demander(e, VENDREDI, LUNDI));
        assertTrue(service.lister().isEmpty());
    }

    @Test
    void chevauchementAvecUneDemandeApprouveeEstRefuse() {
        Employe e = employe("Alpha", "test-0001");
        DemandeConge premiere = demander(e, LUNDI, VENDREDI);
        service.approuver(premiere.getId());

        assertThrows(ValidationException.class, () -> demander(e, VENDREDI, LUNDI_SUIVANT));
        assertEquals(1, service.lister().size());
    }

    @Test
    void uneDemandeEnAttenteNeBloquePasLaSuivante() {
        Employe e = employe("Alpha", "test-0001");
        demander(e, LUNDI, VENDREDI);
        demander(e, VENDREDI, LUNDI_SUIVANT);
        assertEquals(2, service.lister().size());
    }

    @Test
    void approuverEtRefuserChangentLeStatut() {
        Employe e = employe("Alpha", "test-0001");
        DemandeConge premiere = demander(e, LUNDI, VENDREDI);
        DemandeConge seconde = demander(e, LUNDI_SUIVANT, LocalDate.of(2026, 11, 27));

        service.approuver(premiere.getId());
        service.refuser(seconde.getId());

        assertEquals(StatutConge.APPROUVE, service.lister().get(0).getStatut());
        assertEquals(StatutConge.REFUSE, service.lister().get(1).getStatut());
        assertEquals(0, service.aValider());
    }

    @Test
    void uneDemandeDejaTraiteeEstInmodifiable() {
        DemandeConge d = demander(employe("Alpha", "test-0001"), LUNDI, VENDREDI);
        service.approuver(d.getId());

        assertThrows(ValidationException.class, () -> service.approuver(d.getId()));
        assertThrows(ValidationException.class, () -> service.refuser(d.getId()));
        assertThrows(ValidationException.class, () -> service.refuser(999L));
        assertThrows(ValidationException.class, () -> service.refuser(null));
    }

    @Test
    void compteursEtListesParStatutSontReels() {
        Employe e = employe("Alpha", "test-0001");
        DemandeConge a = demander(e, LUNDI, VENDREDI);
        DemandeConge b = demander(e, LUNDI_SUIVANT, LocalDate.of(2026, 11, 27));
        DemandeConge c = demander(e, LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 4));

        service.approuver(a.getId());
        service.refuser(b.getId());

        assertEquals(new CongeService.Compteurs(1, 1, 1), service.compteurs());
        assertEquals(1, service.listerParStatut(StatutConge.EN_ATTENTE).size());
        assertEquals(1, service.listerParStatut(StatutConge.APPROUVE).size());
        assertEquals(1, service.listerParStatut(StatutConge.REFUSE).size());
        assertEquals(3, service.listerParEmploye(e.getId()).size());
        assertEquals(c.getId(), service.listerParStatut(StatutConge.EN_ATTENTE).get(0).getId());
    }

    @Test
    void comptageDesJoursOuvresInclutLesBornes() {
        assertEquals(5, CongeService.compterJoursOuvres(LUNDI, VENDREDI));
        assertEquals(0, CongeService.compterJoursOuvres(SAMEDI, DIMANCHE));
        assertEquals(1, CongeService.compterJoursOuvres(VENDREDI, DIMANCHE));
    }
}
