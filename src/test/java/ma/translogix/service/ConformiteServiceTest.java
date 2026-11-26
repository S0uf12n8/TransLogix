package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import ma.translogix.dao.memory.InMemoryDocumentConformiteDao;
import ma.translogix.dao.memory.InMemoryEmployeDao;
import ma.translogix.model.DocumentConformite;
import ma.translogix.model.Employe;
import ma.translogix.model.Poste;
import ma.translogix.model.StatutConformite;
import ma.translogix.model.TypeContrat;
import ma.translogix.model.TypeDocument;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConformiteServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 15);

    private EmployeService employes;
    private ConformiteService service;
    private Employe chauffeur;

    @BeforeEach
    void setUp() {
        InMemoryEmployeDao employeDao = new InMemoryEmployeDao();
        employes = new EmployeService(employeDao, HORLOGE);
        service = new ConformiteService(new InMemoryDocumentConformiteDao(), employeDao, HORLOGE);
        // Fictional identifiers only (no real CIN / CNSS in tests).
        chauffeur = employes.creer("Test", "Chauffeur", "TEST-0001", null, Poste.CHAUFFEUR, TypeContrat.CDI,
                LocalDate.of(2020, 1, 10));
    }

    @Test
    void expireHierEstExpire() {
        assertEquals(StatutConformite.EXPIRE, service.statut(AUJOURDHUI.minusDays(1)));
    }

    @Test
    void expireAujourdhuiEstARenouveler() {
        assertEquals(StatutConformite.A_RENOUVELER, service.statut(AUJOURDHUI));
    }

    @Test
    void expireDans30JoursEstARenouveler() {
        assertEquals(StatutConformite.A_RENOUVELER, service.statut(AUJOURDHUI.plusDays(30)));
    }

    @Test
    void expireDans31JoursEstValide() {
        assertEquals(StatutConformite.VALIDE, service.statut(AUJOURDHUI.plusDays(31)));
    }

    @Test
    void lesAlertesNeContiennentQueExpireEtARenouvelerDuPlusUrgentAuMoinsUrgent() {
        Employe autre = employes.creer("Test", "Second", "TEST-0002", null, Poste.CHAUFFEUR, TypeContrat.CDD,
                LocalDate.of(2022, 6, 1));
        service.renouveler(chauffeur.getId(), TypeDocument.PERMIS, "PRM-TEST-1", "EC", AUJOURDHUI.plusDays(400));
        DocumentConformite visite = service.renouveler(chauffeur.getId(), TypeDocument.VISITE_MEDICALE, null, null,
                AUJOURDHUI.plusDays(15));
        DocumentConformite permisExpire = service.renouveler(autre.getId(), TypeDocument.PERMIS, "PRM-TEST-2", "C",
                AUJOURDHUI.minusDays(3));
        service.renouveler(autre.getId(), TypeDocument.FIMC, "FIMC-TEST", null, AUJOURDHUI.plusDays(31));

        List<DocumentConformite> alertes = service.alertes();

        assertEquals(List.of(permisExpire.getId(), visite.getId()),
                alertes.stream().map(DocumentConformite::getId).toList());
    }

    @Test
    void lesDocumentsDUnEmployeDesactiveNeSontPlusEnAlerte() {
        service.renouveler(chauffeur.getId(), TypeDocument.PERMIS, "PRM-TEST-1", "EC", AUJOURDHUI.minusDays(1));
        employes.desactiver(chauffeur.getId());
        assertTrue(service.alertes().isEmpty());
    }

    @Test
    void uneDateDExpirationManquanteEstRefusee() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.renouveler(chauffeur.getId(), TypeDocument.VISITE_MEDICALE, null, null, null));
        assertEquals("La date d'expiration est obligatoire.", e.getMessage());
    }

    @Test
    void unPermisSansNumeroEstRefuse() {
        assertThrows(ValidationException.class,
                () -> service.renouveler(chauffeur.getId(), TypeDocument.PERMIS, " ", "C", AUJOURDHUI.plusDays(90)));
    }

    @Test
    void renouvelerRemplaceLeDocumentDuMemeType() {
        service.renouveler(chauffeur.getId(), TypeDocument.PERMIS, "PRM-OLD", "C", AUJOURDHUI.minusDays(2));
        service.renouveler(chauffeur.getId(), TypeDocument.PERMIS, "PRM-NEW", "EC", AUJOURDHUI.plusDays(1000));

        List<DocumentConformite> documents = service.documentsDe(chauffeur.getId());
        assertEquals(1, documents.size());
        assertEquals("PRM-NEW", documents.get(0).getNumero());
        assertEquals("EC", documents.get(0).getCategorie());
        assertEquals(StatutConformite.VALIDE, service.statut(documents.get(0)));
    }

    @Test
    void laCategorieNEstGardeeQuePourLePermis() {
        DocumentConformite fimc = service.renouveler(chauffeur.getId(), TypeDocument.FIMC, "FIMC-1", "EC",
                AUJOURDHUI.plusDays(200));
        assertEquals(null, fimc.getCategorie());
    }

    @Test
    void leStatutGlobalEstLePireDesDocuments() {
        assertTrue(service.statutGlobal(chauffeur.getId()).isEmpty());
        service.renouveler(chauffeur.getId(), TypeDocument.PERMIS, "PRM-1", "C", AUJOURDHUI.plusDays(500));
        assertEquals(StatutConformite.VALIDE, service.statutGlobal(chauffeur.getId()).orElseThrow());
        service.renouveler(chauffeur.getId(), TypeDocument.FIMC, "FIMC-1", null, AUJOURDHUI.plusDays(10));
        assertEquals(StatutConformite.A_RENOUVELER, service.statutGlobal(chauffeur.getId()).orElseThrow());
        service.renouveler(chauffeur.getId(), TypeDocument.VISITE_MEDICALE, null, null, AUJOURDHUI.minusDays(1));
        assertEquals(StatutConformite.EXPIRE, service.statutGlobal(chauffeur.getId()).orElseThrow());
    }

    @Test
    void joursRestantsCompteDepuisAujourdhui() {
        DocumentConformite d = service.renouveler(chauffeur.getId(), TypeDocument.FIMC, "FIMC-1", null,
                AUJOURDHUI.plusDays(15));
        assertEquals(15, service.joursRestants(d));
    }

    @Test
    void unEmployeDesactiveNePeutPasEtreMisAJour() {
        employes.desactiver(chauffeur.getId());
        assertThrows(ValidationException.class, () -> service.renouveler(chauffeur.getId(), TypeDocument.FIMC,
                "FIMC-1", null, AUJOURDHUI.plusDays(90)));
    }

    @Test
    void documentsParStatutCompteLesDocumentsSelonLeStatutCalcule() {
        service.renouveler(chauffeur.getId(), TypeDocument.PERMIS, "PRM-TEST-1", "EC", AUJOURDHUI.minusDays(1));
        service.renouveler(chauffeur.getId(), TypeDocument.FIMC, "FIMC-TEST", null, AUJOURDHUI.plusDays(15));
        service.renouveler(chauffeur.getId(), TypeDocument.VISITE_MEDICALE, null, null, AUJOURDHUI.plusDays(31));

        assertEquals(List.of(
                new ConformiteService.Part("Expiré", 1),
                new ConformiteService.Part("À renouveler", 1),
                new ConformiteService.Part("Conforme", 1)), service.documentsParStatut());
    }

    @Test
    void documentsParStatutGardeLesTroisStatutsSansDonnees() {
        assertEquals(List.of(
                new ConformiteService.Part("Expiré", 0),
                new ConformiteService.Part("À renouveler", 0),
                new ConformiteService.Part("Conforme", 0)), service.documentsParStatut());
    }

    @Test
    void documentsParStatutIgnoreLesDocumentsDesEmployesDesactives() {
        service.renouveler(chauffeur.getId(), TypeDocument.PERMIS, "PRM-TEST-1", "C", AUJOURDHUI.minusDays(1));
        employes.desactiver(chauffeur.getId());
        assertEquals(List.of(0L, 0L, 0L), nombres(service.documentsParStatut()));
    }

    @Test
    void documentsParStatutSuitLeRenouvellement() {
        service.renouveler(chauffeur.getId(), TypeDocument.PERMIS, "PRM-TEST-1", "C", AUJOURDHUI.minusDays(1));
        assertEquals(List.of(1L, 0L, 0L), nombres(service.documentsParStatut()));

        service.renouveler(chauffeur.getId(), TypeDocument.PERMIS, "PRM-TEST-2", "EC", AUJOURDHUI.plusDays(1000));
        assertEquals(List.of(0L, 0L, 1L), nombres(service.documentsParStatut()));
    }

    private static List<Long> nombres(List<ConformiteService.Part> parts) {
        return parts.stream().map(ConformiteService.Part::nombre).toList();
    }
}
