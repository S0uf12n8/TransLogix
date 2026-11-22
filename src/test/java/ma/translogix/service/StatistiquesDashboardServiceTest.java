package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryDocumentConformiteDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryPaiementDao;
import ma.translogix.model.Client;
import ma.translogix.model.Employe;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.Facture;
import ma.translogix.model.Mission;
import ma.translogix.model.ResultatAudit;
import ma.translogix.model.StatutVehicule;
import ma.translogix.model.TypeDocument;
import ma.translogix.service.DashboardService.Alerte;
import ma.translogix.service.DashboardService.ChiffreMensuel;
import ma.translogix.service.DashboardService.Niveau;
import ma.translogix.service.DashboardService.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Every figure is checked against data created in the in-memory DAOs: nothing is hardcoded. Fictional data only. */
class StatistiquesDashboardServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 17);
    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);

    private MissionsDeTest flotte;
    private FactureService factures;
    private ConformiteService conformite;
    private AuditService audit;
    private DashboardService service;
    private Client client;

    @BeforeEach
    void setUp() {
        InMemoryClientDao clientDao = new InMemoryClientDao();
        InMemoryFactureDao factureDao = new InMemoryFactureDao();
        flotte = new MissionsDeTest(clientDao, factureDao, HORLOGE);
        factures = new FactureService(factureDao, clientDao, new InMemoryPaiementDao(), flotte.missions, HORLOGE);
        conformite = new ConformiteService(new InMemoryDocumentConformiteDao(), flotte.employeDao, HORLOGE);
        audit = new AuditService(new InMemoryAuditDao(), HORLOGE, () -> "Test");
        service = new StatistiquesDashboardService(flotte.missions, flotte.vehicules, factures, conformite,
                flotte.employes, audit,
                HORLOGE);
        client = clientDao.save(new Client(null, "Client Test SARL", null, null, null, null, 30));
    }

    /** An invoice of a new delivered mission, issued on that date. */
    private Facture facture(String ht, LocalDate emission) {
        return factures.creer(flotte.livree(client.getId()).getId(), new BigDecimal(ht), null, emission);
    }

    @Test
    void nEstPasMarqueDemo() {
        assertFalse(service.estDemo());
    }

    // ------------------------------------------------------------------ revenue

    @Test
    void sansFactureLesMoisSontAZero() {
        List<ChiffreMensuel> mois = service.chiffreAffairesParMois(6);

        assertEquals(6, mois.size());
        assertEquals(YearMonth.of(2026, 6), mois.get(0).mois());
        assertEquals(YearMonth.of(2026, 11), mois.get(5).mois());
        assertTrue(mois.stream().allMatch(m -> m.montantMad() == 0));
    }

    @Test
    void chiffreAffairesAdditionneLesMontantsHtParMoisDEmission() {
        facture("1000.00", LocalDate.of(2026, 11, 2));
        facture("2500.40", LocalDate.of(2026, 11, 10));
        facture("700.00", LocalDate.of(2026, 9, 30));

        List<ChiffreMensuel> mois = service.chiffreAffairesParMois(3);

        assertEquals(List.of(YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11)),
                mois.stream().map(ChiffreMensuel::mois).toList());
        assertEquals(List.of(700L, 0L, 3500L), mois.stream().map(ChiffreMensuel::montantMad).toList());
    }

    @Test
    void facturesAnnuleesEtHorsPeriodeSontIgnorees() {
        facture("1000.00", LocalDate.of(2026, 11, 5));
        factures.annuler(facture("9999.00", LocalDate.of(2026, 11, 6)).getId());
        facture("5000.00", LocalDate.of(2026, 4, 1)); // before the 6-month window

        List<ChiffreMensuel> mois = service.chiffreAffairesParMois(6);

        assertEquals(1000L, mois.stream().mapToLong(ChiffreMensuel::montantMad).sum());
    }

    @Test
    void montantArrondiAuDirhamLePlusProche() {
        facture("100.49", LocalDate.of(2026, 11, 3));
        facture("100.01", LocalDate.of(2026, 10, 3));
        facture("100.50", LocalDate.of(2026, 10, 4));

        List<ChiffreMensuel> mois = service.chiffreAffairesParMois(2);

        assertEquals(201L, mois.get(0).montantMad()); // 200.51 -> 201
        assertEquals(100L, mois.get(1).montantMad()); // 100.49 -> 100
    }

    @Test
    void nombreDeMoisInvalideEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> service.chiffreAffairesParMois(0));
        assertEquals(24, service.chiffreAffairesParMois(24).size());
    }

    // ------------------------------------------------------------------ missions

    @Test
    void missionsCompteesParStatutDansLOrdreDuContrat() {
        flotte.planifiee(client.getId());
        flotte.planifiee(client.getId());
        flotte.missions.demarrer(flotte.planifiee(client.getId()).getId());
        flotte.livree(client.getId());
        flotte.missions.annuler(flotte.planifiee(client.getId()).getId());
        facture("1000.00", AUJOURDHUI); // one more delivered mission, now FACTUREE

        List<Part> parts = service.missionsParStatut();

        assertEquals(DashboardService.STATUTS_MISSION, parts.stream().map(Part::libelle).toList());
        assertEquals(List.of("Planifiée", "En cours", "Livrée", "Facturée", "Annulée"),
                parts.stream().map(Part::libelle).toList());
        assertEquals(List.of(2, 1, 1, 1, 1), parts.stream().map(Part::nombre).toList());
    }

    @Test
    void sansMissionToutesLesPartsSontAZero() {
        List<Part> parts = service.missionsParStatut();

        assertEquals(DashboardService.STATUTS_MISSION.size(), parts.size());
        assertTrue(parts.stream().allMatch(p -> p.nombre() == 0));
    }

    // ------------------------------------------------------------------ fleet

    @Test
    void flotteRepartitEntreEnMissionDisponibleEtMaintenance() {
        // The fixture truck goes on the road; two more stay available; one in maintenance; one out of service.
        Mission enCours = flotte.planifiee(client.getId());
        flotte.missions.demarrer(enCours.getId());
        flotte.vehicules.creer("22222-B-2", "Scania", "R500", new BigDecimal("26"));
        flotte.vehicules.creer("33333-C-3", "MAN", "TGX", new BigDecimal("32"));
        Long atelier = flotte.vehicules.creer("44444-D-4", "DAF", "XF", new BigDecimal("30")).getId();
        flotte.vehicules.changerStatut(atelier, StatutVehicule.MAINTENANCE);
        Long horsService = flotte.vehicules.creer("55555-E-5", "Iveco", "S-Way", new BigDecimal("26")).getId();
        flotte.vehicules.changerStatut(horsService, StatutVehicule.HORS_SERVICE);

        List<Part> etats = service.etatFlotte();

        assertEquals(List.of("En mission", "Disponible", "En maintenance"), etats.stream().map(Part::libelle).toList());
        assertEquals(List.of(1, 2, 1), etats.stream().map(Part::nombre).toList());
    }

    @Test
    void missionLivreeOuPlanifieeNeMetPasLeVehiculeEnMission() {
        flotte.livree(client.getId());
        flotte.planifiee(client.getId());

        assertEquals(List.of(0, 1, 0), service.etatFlotte().stream().map(Part::nombre).toList());
    }

    // ------------------------------------------------------------------ alerts

    @Test
    void sansProblemeAucuneAlerte() {
        facture("1000.00", AUJOURDHUI); // not yet due

        assertTrue(service.alertesConformite().isEmpty());
    }

    @Test
    void alertesReellesTrieesDeLaPlusGraveALaMoinsGrave() {
        Long atelier = flotte.vehicules.creer("44444-D-4", "DAF", "XF", new BigDecimal("30")).getId();
        flotte.vehicules.changerStatut(atelier, StatutVehicule.MAINTENANCE);
        facture("1000.00", LocalDate.of(2026, 9, 1));  // due 01/10: overdue, 1 140 TTC
        facture("2000.00", LocalDate.of(2026, 9, 15)); // due 15/10: overdue, 2 280 TTC
        Employe beta = flotte.nouveauChauffeur("Beta");
        conformite.renouveler(beta.getId(), TypeDocument.PERMIS, "PRM-TEST-1", "C", AUJOURDHUI.minusDays(3));
        conformite.renouveler(beta.getId(), TypeDocument.FIMC, "FIMC-TEST-1", null, AUJOURDHUI.plusDays(10));
        conformite.renouveler(beta.getId(), TypeDocument.VISITE_MEDICALE, null, null, AUJOURDHUI.plusDays(20));

        List<Alerte> alertes = service.alertesConformite();

        assertEquals(List.of("Documents expirés", "Documents à renouveler", "Factures en retard", "Maintenance flotte"),
                alertes.stream().map(Alerte::titre).toList());
        assertEquals(List.of(Niveau.CRITIQUE, Niveau.ATTENTION, Niveau.ATTENTION, Niveau.INFO),
                alertes.stream().map(Alerte::niveau).toList());
        assertEquals("1 document de conformité expiré.", alertes.get(0).detail());
        assertEquals("2 documents expirent dans moins de 30 jours.", alertes.get(1).detail());
        assertEquals("2 factures impayées après échéance : 3 420,00 MAD.", alertes.get(2).detail());
        assertEquals("1 véhicule à l'atelier.", alertes.get(3).detail());
        assertEquals(List.of("rh/conformite.fxml", "rh/conformite.fxml", "comptable/factures.fxml",
                "dispatcher/flotte.fxml"), alertes.stream().map(Alerte::cibleFxml).toList());
    }

    // ------------------------------------------------------------------ audit

    @Test
    void auditRenvoieLesDerniersEvenementsReels() {
        audit.enregistrer("Utilisateurs", "action 1", ResultatAudit.REUSSI);
        audit.enregistrer("Utilisateurs", "action 2", ResultatAudit.REUSSI);

        assertEquals(List.of("action 2"),
                service.derniersEvenementsAudit(1).stream().map(EvenementAudit::getAction).toList());
        assertThrows(IllegalArgumentException.class, () -> service.derniersEvenementsAudit(-1));
    }

    // ------------------------------------------------------------------ KPI cards

    @Test
    void indicateursSansDonnees() {
        DashboardService.Indicateurs k = service.indicateurs();

        assertEquals(0, k.chiffreMoisMad());
        assertEquals(null, k.variationPourcent()); // no revenue last month: no fake percentage
        assertEquals(1, k.flotteOperationnelle()); // the fixture truck
        assertEquals(1, k.flotteTotale());
        assertEquals(0, k.creancesMad());
        assertEquals(100, k.conformitePourcent());
    }

    @Test
    void chiffreDuMoisEtVariation() {
        facture("1000.00", LocalDate.of(2026, 10, 5));
        facture("1250.00", LocalDate.of(2026, 11, 5));

        DashboardService.Indicateurs k = service.indicateurs();

        assertEquals(1250, k.chiffreMoisMad());
        assertEquals(25, k.variationPourcent());
    }

    @Test
    void flotteOperationnelleSurLeParcComplet() {
        Long atelier = flotte.vehicules.creer("44444-D-4", "DAF", "XF", new BigDecimal("30")).getId();
        flotte.vehicules.changerStatut(atelier, StatutVehicule.MAINTENANCE);
        Long horsService = flotte.vehicules.creer("55555-E-5", "Iveco", "S-Way", new BigDecimal("26")).getId();
        flotte.vehicules.changerStatut(horsService, StatutVehicule.HORS_SERVICE);

        DashboardService.Indicateurs k = service.indicateurs();

        assertEquals(1, k.flotteOperationnelle());
        assertEquals(3, k.flotteTotale());
        assertEquals(1, k.enMaintenance());
    }

    @Test
    void creancesTtcDesFacturesImpayeesSeulement() {
        facture("1000.00", AUJOURDHUI);                     // unpaid, not due: 1 140 TTC
        facture("2000.00", LocalDate.of(2026, 9, 1));       // unpaid and overdue: 2 280 TTC
        Facture payee = facture("5000.00", AUJOURDHUI);
        factures.enregistrerPaiement(payee.getId(), ma.translogix.model.ModePaiement.VIREMENT, "VIR-T", AUJOURDHUI);
        factures.annuler(facture("9000.00", AUJOURDHUI).getId());

        DashboardService.Indicateurs k = service.indicateurs();

        assertEquals(3420, k.creancesMad());
        assertEquals(1, k.facturesEnRetard());
    }

    @Test
    void conformiteRhPartDesEmployesActifsSansDocumentATraiter() {
        Employe expire = flotte.nouveauChauffeur("Beta");
        Employe bientot = flotte.nouveauChauffeur("Gamma");
        Employe enRegle = flotte.nouveauChauffeur("Delta");
        conformite.renouveler(expire.getId(), TypeDocument.PERMIS, "PRM-T1", "C", AUJOURDHUI.minusDays(1));
        conformite.renouveler(bientot.getId(), TypeDocument.FIMC, "FIMC-T1", null, AUJOURDHUI.plusDays(10));
        conformite.renouveler(enRegle.getId(), TypeDocument.PERMIS, "PRM-T2", "C", AUJOURDHUI.plusYears(2));

        DashboardService.Indicateurs k = service.indicateurs();

        assertEquals(50, k.conformitePourcent()); // 2 of 4 active employees (fixture driver included)
        assertEquals(1, k.documentsExpires());
        assertEquals(1, k.documentsARenouveler());
    }

    // ------------------------------------------------------------------ recent trips

    @Test
    void trajetsRecentsDuPlusRecentAvecChauffeurCamionEtFacture() {
        Mission premiere = flotte.planifiee(client.getId());
        Mission livree = flotte.livree(client.getId());
        factures.creer(livree.getId(), new BigDecimal("1000.00"), null, AUJOURDHUI);
        Mission derniere = flotte.planifiee(client.getId());

        List<DashboardService.Trajet> trajets = service.trajetsRecents(5);

        assertEquals(List.of(derniere.getReference(), livree.getReference(), premiere.getReference()),
                trajets.stream().map(DashboardService.Trajet::reference).toList());
        DashboardService.Trajet facturee = trajets.get(1);
        assertEquals(flotte.chauffeur.getNomComplet(), facturee.chauffeur());
        assertEquals("11111-A-1 · Volvo", facturee.camion());
        assertEquals("Facturée", facturee.statut());
        assertEquals("1 000,00 MAD", facturee.montantFacture());
        assertEquals(null, trajets.get(0).montantFacture());
        assertEquals(2, service.trajetsRecents(2).size());
    }
}
