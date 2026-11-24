package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import ma.translogix.dao.memory.InMemoryEmployeDao;
import ma.translogix.model.Employe;
import ma.translogix.model.Poste;
import ma.translogix.model.TypeContrat;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Fictional identifiers only (no real CIN / CNSS in tests). */
class EmployeServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 15);

    private InMemoryEmployeDao dao;
    private EmployeService service;

    @BeforeEach
    void setUp() {
        dao = new InMemoryEmployeDao();
        service = new EmployeService(dao, HORLOGE);
    }

    private Employe creer(String nom, String cin, Poste poste, LocalDate embauche) {
        return service.creer(nom, "Test", cin, "100200300", poste, TypeContrat.CDI, embauche);
    }

    @Test
    void creerEnregistreUnEmployeActifSansCompte() {
        Employe e = creer("Alpha", "test-0001", Poste.CHAUFFEUR, LocalDate.of(2021, 3, 15));
        assertTrue(e.isActif());
        assertEquals("TEST-0001", e.getCin()); // normalised
        assertEquals(null, e.getUtilisateurId());
        assertEquals(1, service.lister().size());
    }

    @Test
    void lesChampsObligatoiresSontVerifies() {
        LocalDate d = LocalDate.of(2021, 1, 1);
        assertThrows(ValidationException.class,
                () -> service.creer(" ", "P", "TEST-1", null, Poste.CHAUFFEUR, TypeContrat.CDI, d));
        assertThrows(ValidationException.class,
                () -> service.creer("N", null, "TEST-1", null, Poste.CHAUFFEUR, TypeContrat.CDI, d));
        assertThrows(ValidationException.class,
                () -> service.creer("N", "P", "", null, Poste.CHAUFFEUR, TypeContrat.CDI, d));
        assertThrows(ValidationException.class,
                () -> service.creer("N", "P", "TEST-1", null, null, TypeContrat.CDI, d));
        assertThrows(ValidationException.class,
                () -> service.creer("N", "P", "TEST-1", null, Poste.CHAUFFEUR, null, d));
        assertThrows(ValidationException.class,
                () -> service.creer("N", "P", "TEST-1", null, Poste.CHAUFFEUR, TypeContrat.CDI, null));
        assertTrue(service.lister().isEmpty());
    }

    @Test
    void unCnssNonNumeriqueEstRefuse() {
        assertThrows(ValidationException.class, () -> service.creer("N", "P", "TEST-1", "12AB",
                Poste.CHAUFFEUR, TypeContrat.CDI, LocalDate.of(2021, 1, 1)));
    }

    @Test
    void unCinEnDoubleEstRefuseMemeAvecUneAutreCasse() {
        creer("Alpha", "TEST-0001", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        ValidationException e = assertThrows(ValidationException.class,
                () -> creer("Beta", " test-0001 ", Poste.MECANICIEN, LocalDate.of(2022, 1, 1)));
        assertEquals("Ce CIN est déjà enregistré pour un autre employé.", e.getMessage());
    }

    @Test
    void modifierRefuseLeCinDUnAutreMaisAccepteLeSien() {
        Employe a = creer("Alpha", "TEST-0001", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        creer("Beta", "TEST-0002", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        assertThrows(ValidationException.class, () -> service.modifier(a.getId(), "Alpha", "Test", "TEST-0002",
                null, Poste.CHAUFFEUR, TypeContrat.CDI, LocalDate.of(2021, 1, 1)));
        service.modifier(a.getId(), "Alpha", "Nouveau", "TEST-0001", null, Poste.MECANICIEN, TypeContrat.CDD,
                LocalDate.of(2021, 1, 1));
        assertEquals(Poste.MECANICIEN, service.trouver(a.getId()).getPoste());
        assertEquals("Nouveau", service.trouver(a.getId()).getPrenom());
    }

    @Test
    void leMatriculeEstGenereParAnneeDEmbauche() {
        Employe a = creer("Alpha", "TEST-0001", Poste.CHAUFFEUR, LocalDate.of(2021, 3, 15));
        Employe b = creer("Beta", "TEST-0002", Poste.CHAUFFEUR, LocalDate.of(2021, 9, 1));
        Employe c = creer("Gamma", "TEST-0003", Poste.ADMINISTRATIF, LocalDate.of(2023, 2, 1));
        assertEquals("MAT-2021-01", a.getMatricule());
        assertEquals("MAT-2021-02", b.getMatricule());
        assertEquals("MAT-2023-01", c.getMatricule());
    }

    @Test
    void leMatriculeNeChangePasALaModification() {
        Employe a = creer("Alpha", "TEST-0001", Poste.CHAUFFEUR, LocalDate.of(2021, 3, 15));
        service.modifier(a.getId(), "Alpha", "Test", "TEST-0001", null, Poste.CHAUFFEUR, TypeContrat.CDI,
                LocalDate.of(2024, 1, 1));
        assertEquals("MAT-2021-01", service.trouver(a.getId()).getMatricule());
    }

    @Test
    void uneDateDEmbaucheFutureEstRefusee() {
        assertThrows(ValidationException.class,
                () -> creer("Alpha", "TEST-0001", Poste.CHAUFFEUR, AUJOURDHUI.plusDays(1)));
        assertEquals("MAT-2026-01", creer("Alpha", "TEST-0001", Poste.CHAUFFEUR, AUJOURDHUI).getMatricule());
    }

    @Test
    void desactiverGardeLEmployeMaisLeRetireDesChauffeurs() {
        Employe a = creer("Alpha", "TEST-0001", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        service.desactiver(a.getId());
        assertFalse(service.trouver(a.getId()).isActif());
        assertEquals(1, service.lister().size());
        assertTrue(service.listerChauffeurs().isEmpty());
        assertThrows(ValidationException.class, () -> service.desactiver(a.getId()));
    }

    @Test
    void findByPosteFiltreParPoste() {
        creer("Alpha", "TEST-0001", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        creer("Beta", "TEST-0002", Poste.MECANICIEN, LocalDate.of(2021, 1, 1));
        creer("Gamma", "TEST-0003", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        assertEquals(List.of("Alpha", "Gamma"), dao.findByPoste(Poste.CHAUFFEUR).stream().map(Employe::getNom).toList());
        assertEquals(1, service.listerParPoste(Poste.MECANICIEN).size());
        assertTrue(dao.findByPoste(Poste.AGENT_DEPOT).isEmpty());
    }

    @Test
    void listerChauffeursNeRendQueLesChauffeursActifsTriesParNom() {
        creer("Zed", "TEST-0001", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        creer("Beta", "TEST-0002", Poste.ADMINISTRATIF, LocalDate.of(2021, 1, 1));
        creer("Alpha", "TEST-0003", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        Employe inactif = creer("Mid", "TEST-0004", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        service.desactiver(inactif.getId());
        assertEquals(List.of("Alpha", "Zed"), service.listerChauffeurs().stream().map(Employe::getNom).toList());
    }

    @Test
    void lierCompteEtRetrouverParUtilisateur() {
        Employe a = creer("Alpha", "TEST-0001", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        Employe b = creer("Beta", "TEST-0002", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        service.lierCompte(a.getId(), 7L);
        assertEquals(a.getId(), service.trouverParUtilisateur(7L).orElseThrow().getId());
        assertThrows(ValidationException.class, () -> service.lierCompte(b.getId(), 7L));
        assertTrue(service.trouverParUtilisateur(8L).isEmpty());
    }

    @Test
    void effectifsComptentLesActifs() {
        creer("A", "TEST-0001", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        creer("B", "TEST-0002", Poste.CHAUFFEUR, AUJOURDHUI.withDayOfMonth(2));
        service.creer("C", "Test", "TEST-0003", null, Poste.ADMINISTRATIF, TypeContrat.CDD, LocalDate.of(2023, 1, 1));
        Employe d = creer("D", "TEST-0004", Poste.CHAUFFEUR, LocalDate.of(2020, 1, 1));
        service.desactiver(d.getId());

        EmployeService.Effectifs e = service.effectifs();
        assertEquals(3, e.actifs());
        assertEquals(2, e.chauffeursActifs());
        assertEquals(3, e.chauffeurs());
        assertEquals(1, e.administratifEtDepot());
        assertEquals(2, e.cdi());
        assertEquals(1, e.cdd());
        assertEquals(0, e.essai());
        assertEquals(1, e.recrutementsDuMois());
    }

    @Test
    void employesParPosteCompteLesActifsDansLordreDesPostes() {
        creer("A", "TEST-0001", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        creer("B", "TEST-0002", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        creer("C", "TEST-0003", Poste.MECANICIEN, LocalDate.of(2021, 1, 1));
        creer("D", "TEST-0004", Poste.ADMINISTRATIF, LocalDate.of(2021, 1, 1));
        Employe inactif = creer("E", "TEST-0005", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        service.desactiver(inactif.getId());

        assertEquals(List.of(
                new EmployeService.Part("Chauffeur", 2),
                new EmployeService.Part("Agent de dépôt", 0),
                new EmployeService.Part("Mécanicien", 1),
                new EmployeService.Part("Administratif", 1)), service.employesParPoste());
    }

    @Test
    void employesParPosteGardeLesQuatrePostesSansDonnees() {
        assertEquals(List.of(
                new EmployeService.Part("Chauffeur", 0),
                new EmployeService.Part("Agent de dépôt", 0),
                new EmployeService.Part("Mécanicien", 0),
                new EmployeService.Part("Administratif", 0)), service.employesParPoste());
    }

    @Test
    void employesParPosteSuitLesModificationsEtDesactivations() {
        Employe a = creer("A", "TEST-0001", Poste.CHAUFFEUR, LocalDate.of(2021, 1, 1));
        assertEquals(List.of(1L, 0L, 0L, 0L), nombres(service.employesParPoste()));

        service.modifier(a.getId(), "A", "Test", "TEST-0001", null, Poste.AGENT_DEPOT, TypeContrat.CDI,
                LocalDate.of(2021, 1, 1));
        assertEquals(List.of(0L, 1L, 0L, 0L), nombres(service.employesParPoste()));

        service.desactiver(a.getId());
        assertEquals(List.of(0L, 0L, 0L, 0L), nombres(service.employesParPoste()));
    }

    private static List<Long> nombres(List<EmployeService.Part> parts) {
        return parts.stream().map(EmployeService.Part::nombre).toList();
    }
}
