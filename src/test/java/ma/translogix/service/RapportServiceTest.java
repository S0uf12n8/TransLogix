package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import ma.translogix.dao.memory.InMemoryAuditDao;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryDocumentConformiteDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryPaiementDao;
import ma.translogix.dao.memory.InMemoryUtilisateurDao;
import ma.translogix.model.Client;
import ma.translogix.model.Employe;
import ma.translogix.model.Facture;
import ma.translogix.model.Mission;
import ma.translogix.model.ModePaiement;
import ma.translogix.model.Role;
import ma.translogix.model.TypeDocument;
import ma.translogix.model.Utilisateur;
import ma.translogix.util.CsvExporter;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Every report is built from data created in the in-memory DAOs. Fictional data only. */
class RapportServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 17);
    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);

    private MissionsDeTest flotte;
    private FactureService factures;
    private ConformiteService conformite;
    private UtilisateurService utilisateurs;
    private RapportService service;
    private Client alpha;
    private Client beta;

    @BeforeEach
    void setUp() {
        InMemoryClientDao clientDao = new InMemoryClientDao();
        InMemoryFactureDao factureDao = new InMemoryFactureDao();
        flotte = new MissionsDeTest(clientDao, factureDao, HORLOGE);
        factures = new FactureService(factureDao, clientDao, new InMemoryPaiementDao(), flotte.missions, HORLOGE);
        conformite = new ConformiteService(new InMemoryDocumentConformiteDao(), flotte.employeDao, HORLOGE);
        utilisateurs = new UtilisateurService(new InMemoryUtilisateurDao(),
                new AuditService(new InMemoryAuditDao(), HORLOGE, () -> "Test"));
        service = new RapportService(utilisateurs, factures, flotte.missions,
                new ClientService(clientDao, factureDao), flotte.vehicules, flotte.employes, conformite, HORLOGE);
        alpha = clientDao.save(new Client(null, "Alpha Transport SARL", null, null, null, null, 30));
        beta = clientDao.save(new Client(null, "Beta Logistique", null, null, null, null, 30));
    }

    private Facture facture(Client client, String ht, LocalDate emission) {
        return factures.creer(flotte.livree(client.getId()).getId(), new BigDecimal(ht), null, emission);
    }

    private static List<String> colonne(Rapport r, int index) {
        return r.lignes().stream().map(l -> l.get(index)).toList();
    }

    // ------------------------------------------------------------------ invoices

    @Test
    void facturationSansFiltreListeToutesLesFacturesParDate() {
        facture(alpha, "1000.00", LocalDate.of(2026, 11, 10));
        facture(beta, "2000.00", LocalDate.of(2026, 9, 1));

        Rapport r = service.generer(TypeRapport.FACTURATION, FiltreRapport.TOUT);

        assertEquals(8, r.entetes().size());
        assertEquals(List.of("Beta Logistique", "Alpha Transport SARL"), colonne(r, 2));
        assertEquals(List.of("2000,00", "1000,00"), colonne(r, 4));
        assertEquals(List.of("2280,00", "1140,00"), colonne(r, 6));
        assertEquals(List.of("En retard", "En attente"), colonne(r, 7)); // due 01/10 is past
        assertTrue(r.lignes().stream().allMatch(l -> l.get(3).startsWith("TRJ-")), "mission reference resolved");
        assertEquals("2 factures · Total HT 3 000,00 MAD · Total TTC 3 420,00 MAD", r.resume());
        assertEquals("rapport-facturation-2026-11-17.csv", r.nomFichier());
    }

    @Test
    void facturationFiltreeParPeriodeClientEtStatut() {
        facture(alpha, "1000.00", LocalDate.of(2026, 11, 1));
        facture(alpha, "1500.00", LocalDate.of(2026, 11, 30));
        facture(alpha, "700.00", LocalDate.of(2026, 10, 31));
        facture(beta, "9000.00", LocalDate.of(2026, 11, 5));
        Facture payee = facture(alpha, "300.00", LocalDate.of(2026, 11, 6));
        factures.enregistrerPaiement(payee.getId(), ModePaiement.VIREMENT, "VIR-TEST", AUJOURDHUI);

        LocalDate du = LocalDate.of(2026, 11, 1);
        LocalDate au = LocalDate.of(2026, 11, 30);
        Rapport periodeEtClient = service.generer(TypeRapport.FACTURATION, new FiltreRapport(du, au, alpha.getId(), null));
        Rapport payees = service.generer(TypeRapport.FACTURATION, new FiltreRapport(du, au, alpha.getId(), "Payée"));

        assertEquals(List.of("1000,00", "300,00", "1500,00"), colonne(periodeEtClient, 4)); // bounds included
        assertEquals(List.of("300,00"), colonne(payees, 4));
    }

    @Test
    void totauxIgnorentLesFacturesAnnulees() {
        facture(alpha, "1000.00", AUJOURDHUI);
        factures.annuler(facture(alpha, "5000.00", AUJOURDHUI).getId());

        Rapport r = service.generer(TypeRapport.FACTURATION, FiltreRapport.TOUT);

        assertEquals(2, r.lignes().size());
        assertEquals("2 factures · Total HT 1 000,00 MAD · Total TTC 1 140,00 MAD (hors factures annulées)",
                r.resume());
    }

    @Test
    void montantsRestentDesNombresPourExcel() {
        facture(alpha, "12345.60", AUJOURDHUI);

        Rapport r = service.generer(TypeRapport.FACTURATION, FiltreRapport.TOUT);

        assertEquals("12345,60", r.lignes().get(0).get(4));
        String csv = CsvExporter.versTexte(r.entetes(), r.lignes());
        assertTrue(csv.contains(";12345,60;"), csv);
    }

    // ------------------------------------------------------------------ payments

    @Test
    void reglementsSontGroupesParMoisAvecLeMontantTtcDeLaFactureReglee() {
        Facture octobre = facture(alpha, "1000.00", LocalDate.of(2026, 10, 20));
        factures.enregistrerPaiement(octobre.getId(), ModePaiement.VIREMENT, "VIR-A", LocalDate.of(2026, 11, 2));
        Facture novembre = facture(beta, "2000.00", LocalDate.of(2026, 11, 1));
        factures.enregistrerPaiement(novembre.getId(), ModePaiement.CHEQUE, "CHQ-B", LocalDate.of(2026, 11, 15));

        Rapport r = service.generer(TypeRapport.PAIEMENTS, FiltreRapport.TOUT);

        assertEquals(List.of("Mois", "Date d'encaissement", "N° facture", "Client", "Mode de règlement",
                "Référence", "Montant encaissé (MAD)"), r.entetes());
        assertEquals(List.of("2026-11", "2026-11"), colonne(r, 0));
        assertEquals(List.of("Alpha Transport SARL", "Beta Logistique"), colonne(r, 3));
        assertEquals(List.of("Virement bancaire", "Chèque bancaire"), colonne(r, 4));
        assertEquals(List.of("1140,00", "2280,00"), colonne(r, 6));
        assertEquals("2 règlements · Total encaissé 3 420,00 MAD · 1 mois", r.resume());
        assertEquals("rapport-reglements-2026-11-17.csv", r.nomFichier());
    }

    @Test
    void reglementsFiltresParPeriodeClientEtModeDeReglement() {
        Facture a = facture(alpha, "1000.00", LocalDate.of(2026, 10, 20));
        factures.enregistrerPaiement(a.getId(), ModePaiement.VIREMENT, "VIR-A", LocalDate.of(2026, 10, 25));
        Facture b = facture(beta, "2000.00", LocalDate.of(2026, 11, 1));
        factures.enregistrerPaiement(b.getId(), ModePaiement.CHEQUE, "CHQ-B", LocalDate.of(2026, 11, 15));

        Rapport novembre = service.generer(TypeRapport.PAIEMENTS,
                new FiltreRapport(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, null));
        Rapport alphaOnly = service.generer(TypeRapport.PAIEMENTS, new FiltreRapport(null, null, alpha.getId(), null));
        Rapport virements = service.generer(TypeRapport.PAIEMENTS,
                new FiltreRapport(null, null, null, ModePaiement.VIREMENT.getLibelle()));

        assertEquals(List.of("CHQ-B"), colonne(novembre, 5));
        assertEquals(List.of("VIR-A"), colonne(alphaOnly, 5));
        assertEquals(List.of("VIR-A"), colonne(virements, 5));
        assertTrue(service.generer(TypeRapport.PAIEMENTS,
                new FiltreRapport(LocalDate.of(2026, 12, 1), null, null, null)).lignes().isEmpty());
    }

    @Test
    void rapportDesReglementsEstExportableEnCsvExcel() {
        Facture f = facture(alpha, "12345.60", AUJOURDHUI);
        factures.enregistrerPaiement(f.getId(), ModePaiement.EFFET_DE_COMMERCE, "EFF-1", AUJOURDHUI);

        Rapport r = service.generer(TypeRapport.PAIEMENTS, FiltreRapport.TOUT);
        String csv = CsvExporter.versTexte(r.entetes(), r.lignes());

        assertTrue(csv.startsWith(CsvExporter.BOM));
        assertTrue(csv.contains(";14073,98\r\n"), csv); // 12345,60 HT + 14 % TVA
        assertTrue(csv.contains("EFF-1"), csv);
    }

    // ------------------------------------------------------------------ missions

    @Test
    void missionsAvecNomsResolusEtFiltres() {
        Mission m1 = flotte.planifiee(alpha.getId()); // 05/01/2026 08:00
        flotte.planifiee(beta.getId());              // 05/01/2026 11:00
        Mission m3 = flotte.planifiee(alpha.getId());
        flotte.missions.demarrer(m3.getId());

        Rapport tout = service.generer(TypeRapport.MISSIONS, FiltreRapport.TOUT);
        Rapport alphaPlanifiees = service.generer(TypeRapport.MISSIONS,
                new FiltreRapport(null, null, alpha.getId(), "Planifiée"));

        assertEquals(3, tout.lignes().size());
        List<String> premiere = tout.lignes().get(0);
        assertEquals(m1.getReference(), premiere.get(0));
        assertEquals("Alpha Transport SARL", premiere.get(1));
        assertEquals(flotte.chauffeur.getNomComplet(), premiere.get(2));
        assertEquals("11111-A-1", premiere.get(3));
        assertEquals("05/01/2026 08:00", premiere.get(6));
        assertEquals("12", premiere.get(8));
        assertEquals(List.of(m1.getReference()), colonne(alphaPlanifiees, 0));
        assertEquals("1 mission", alphaPlanifiees.resume());
    }

    @Test
    void missionsFiltreesParDateDeDebut() {
        flotte.planifiee(alpha.getId()); // all fixture missions start on 05/01/2026

        LocalDate jour = LocalDate.of(2026, 1, 5);
        assertEquals(1, service.generer(TypeRapport.MISSIONS, new FiltreRapport(jour, jour, null, null)).lignes().size());
        assertEquals(0, service.generer(TypeRapport.MISSIONS,
                new FiltreRapport(jour.plusDays(1), null, null, null)).lignes().size());
    }

    // ------------------------------------------------------------------ RH and accounts

    @Test
    void conformiteListeLesDocumentsATraiterEtIgnoreLeClient() {
        Employe gamma = flotte.nouveauChauffeur("Gamma");
        conformite.renouveler(gamma.getId(), TypeDocument.PERMIS, "PRM-TEST-1", "C", AUJOURDHUI.minusDays(3));
        conformite.renouveler(gamma.getId(), TypeDocument.FIMC, "FIMC-TEST-1", null, AUJOURDHUI.plusDays(10));
        conformite.renouveler(gamma.getId(), TypeDocument.VISITE_MEDICALE, null, null, AUJOURDHUI.plusYears(1));

        Rapport tout = service.generer(TypeRapport.CONFORMITE, new FiltreRapport(null, null, alpha.getId(), null));
        Rapport expires = service.generer(TypeRapport.CONFORMITE, new FiltreRapport(null, null, null, "Expiré"));

        assertEquals(List.of("Permis de conduire", "Carte FIMC / FCO"), colonne(tout, 1)); // valid one left out
        assertEquals(List.of("-3", "10"), colonne(tout, 4));
        assertEquals(gamma.getNomComplet(), tout.lignes().get(0).get(0));
        assertEquals(List.of("PRM-TEST-1"), colonne(expires, 2));
        assertEquals("2 documents à traiter", tout.resume());
    }

    @Test
    void comptesFiltresParStatutEtSansDates() {
        Utilisateur sara = utilisateurs.creer("Alami", "Sara", "sara@translogix.ma", "motdepasse1", Role.DISPATCHER);
        utilisateurs.modifier(sara.getId(), "Alami", "Sara", "sara@translogix.ma", Role.DISPATCHER, false);
        utilisateurs.creer("Bennani", "Ali", "ali@translogix.ma", "motdepasse1", Role.RH);

        Rapport inactifs = service.generer(TypeRapport.COMPTES,
                new FiltreRapport(LocalDate.of(2030, 1, 1), null, null, "Inactif"));

        assertEquals(List.of("Nom", "Prénom", "Email", "Rôle", "Statut"), inactifs.entetes());
        assertEquals(List.of(List.of("Alami", "Sara", "sara@translogix.ma", "Dispatcher", "Inactif")),
                inactifs.lignes());
    }

    // ------------------------------------------------------------------ rules

    @Test
    void periodeInverseeEstRefusee() {
        FiltreRapport inverse = new FiltreRapport(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1), null, null);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.generer(TypeRapport.FACTURATION, inverse));
        assertEquals("La date de fin doit être après la date de début.", ex.getMessage());
    }

    @Test
    void chaqueRapportProposeSesStatutsEtDesLignesCompletes() {
        assertEquals(List.of("En attente", "En retard", "Payée", "Annulée"), service.statuts(TypeRapport.FACTURATION));
        assertEquals(List.of("Virement bancaire", "Chèque bancaire", "Effet de commerce"),
                service.statuts(TypeRapport.PAIEMENTS));
        assertEquals(DashboardService.STATUTS_MISSION, service.statuts(TypeRapport.MISSIONS));
        assertEquals(List.of("Expiré", "À renouveler"), service.statuts(TypeRapport.CONFORMITE));
        assertEquals(List.of("Actif", "Inactif"), service.statuts(TypeRapport.COMPTES));

        facture(alpha, "1000.00", AUJOURDHUI);
        for (TypeRapport type : TypeRapport.values()) {
            Rapport r = service.generer(type, null);
            r.lignes().forEach(l -> assertEquals(r.entetes().size(), l.size(), type.name()));
        }
    }
}
