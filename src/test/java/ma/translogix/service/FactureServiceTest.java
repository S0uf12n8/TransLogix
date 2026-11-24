package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryPaiementDao;
import ma.translogix.model.Client;
import ma.translogix.model.Facture;
import ma.translogix.model.Mission;
import ma.translogix.model.ModePaiement;
import ma.translogix.model.StatutFacture;
import ma.translogix.model.StatutMission;
import ma.translogix.service.FactureService.ClientARelancer;
import ma.translogix.service.FactureService.Recouvrement;
import ma.translogix.service.FactureService.TrancheAge;
import ma.translogix.service.FactureService.TrancheAgee;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FactureServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 17);
    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);

    private InMemoryClientDao clientDao;
    private InMemoryFactureDao factureDao;
    private InMemoryPaiementDao paiementDao;
    private FactureService service;
    private MissionsDeTest flotte;
    private Client client;

    @BeforeEach
    void setUp() {
        clientDao = new InMemoryClientDao();
        factureDao = new InMemoryFactureDao();
        paiementDao = new InMemoryPaiementDao();
        flotte = new MissionsDeTest(clientDao, factureDao, HORLOGE);
        service = new FactureService(factureDao, clientDao, paiementDao, flotte.missions, HORLOGE);
        client = clientDao.save(new Client(null, "LogiMaroc SARL", null, null, null, null, 30));
    }

    /** A new delivered mission of the client. */
    private Long livree() {
        return flotte.livree(client.getId()).getId();
    }

    private Facture facture(String ht) {
        return service.creer(livree(), new BigDecimal(ht), null);
    }

    /** Invoice of the default client whose due date is exactly {@code jours} days before "today". */
    private Facture factureEcheue(String ht, int jours) {
        return factureEcheue(client, ht, jours);
    }

    private Facture factureEcheue(Client destinataire, String ht, int jours) {
        return service.creer(flotte.livree(destinataire.getId()).getId(), new BigDecimal(ht), 0,
                AUJOURDHUI.minusDays(jours));
    }

    private static TrancheAgee tranche(Recouvrement recouvrement, TrancheAge age) {
        return recouvrement.tranches().stream()
                .filter(t -> t.tranche() == age)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void milleHtDonne140DeTvaEt1140Ttc() {
        Facture f = facture("1000.00");

        assertEquals(new BigDecimal("1000.00"), f.getMontantHT());
        assertEquals(new BigDecimal("140.00"), f.getMontantTva());
        assertEquals(new BigDecimal("1140.00"), f.getMontantTtc());
        assertEquals(new BigDecimal("14.00"), f.getTauxTva());
    }

    @Test
    void tvaArrondieHalfUpADeuxDecimales() {
        assertEquals(new BigDecimal("46.67"), facture("333.33").getMontantTva()); // 46.6662 -> 46.67
        assertEquals(new BigDecimal("380.00"), facture("333.33").getMontantTtc()); // 333.33 + 46.67
    }

    @ParameterizedTest
    @CsvSource({"21052.63, 24000.00", "39473.68, 45000.00", "17894.74, 20400.00", "33333.33, 38000.00"})
    void montantsDeLaMaquetteRetombentSurLeTtcAffiche(String ht, String ttc) {
        assertEquals(new BigDecimal(ttc), FactureService.calculerTtc(new BigDecimal(ht)));
    }

    @Test
    void echeanceEgaleEmissionPlusDelaiDuClient() {
        Facture f = facture("1000");

        assertEquals(AUJOURDHUI, f.getDateEmission());
        assertEquals(AUJOURDHUI.plusDays(30), f.getDateEcheance());
    }

    @Test
    void conditionChoisieRemplaceLeDelaiDuClient() {
        assertEquals(AUJOURDHUI.plusDays(60),
                service.creer(livree(), BigDecimal.TEN, 60).getDateEcheance());
        assertEquals(AUJOURDHUI, // comptant
                service.creer(livree(), BigDecimal.TEN, 0).getDateEcheance());
    }

    @Test
    void numerosGeneresParAnnee() {
        assertEquals("FACT-2026-001", facture("10").getNumero());
        assertEquals("FACT-2026-002", facture("10").getNumero());
        assertEquals("FACT-2025-001", service.creer(livree(), BigDecimal.TEN, null,
                LocalDate.of(2025, 12, 30)).getNumero());
        assertEquals("FACT-2026-003", facture("10").getNumero());
    }

    @Test
    void factureDUneMissionLivreeReprendSonClientEtSaReferenceEtLaPasseAFacturee() {
        Mission m = flotte.livree(client.getId());

        Facture f = service.creer(m.getId(), new BigDecimal("1000"), null);

        assertEquals(StatutFacture.EN_ATTENTE, f.getStatut());
        assertEquals(m.getId(), f.getMissionId());
        assertEquals(client.getId(), f.getClientId());
        assertEquals(m.getReference(), f.getReferenceTrajet());
        assertEquals(StatutMission.FACTUREE, flotte.missions.trouver(m.getId()).getStatut());
    }

    @Test
    void missionNonLivreeEstRefusee() {
        Mission planifiee = flotte.planifiee(client.getId());
        Mission enCours = flotte.planifiee(client.getId());
        flotte.missions.demarrer(enCours.getId());

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.creer(planifiee.getId(), new BigDecimal("1000"), null));
        assertEquals("Seule une mission livrée peut être facturée (" + planifiee.getReference()
                + " est « Planifiée »).", e.getMessage());
        assertThrows(ValidationException.class, () -> service.creer(enCours.getId(), new BigDecimal("1000"), null));
        assertTrue(service.lister().isEmpty());
        assertEquals(StatutMission.EN_COURS, flotte.missions.trouver(enCours.getId()).getStatut());
    }

    @Test
    void uneSecondeFacturePourLaMemeMissionEstRefusee() {
        Long mission = livree();
        Facture premiere = service.creer(mission, new BigDecimal("1000"), null);
        service.annuler(premiere.getId()); // even a cancelled invoice keeps the mission taken (mission_id UNIQUE)

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.creer(mission, new BigDecimal("1000"), null));
        assertTrue(e.getMessage().contains("déjà facturée (" + premiere.getNumero() + ")"), e.getMessage());
        assertEquals(1, service.lister().size());
    }

    @Test
    void seulesLesMissionsLivreesSansFactureSontProposees() {
        Long aFacturer = livree();
        Long facturee = livree();
        flotte.planifiee(client.getId());
        service.creer(facturee, BigDecimal.TEN, null);

        assertEquals(List.of(aFacturer),
                service.missionsAFacturer().stream().map(Mission::getId).toList());
    }

    @Test
    void montantNulOuNegatifEstRefuse() {
        assertThrows(ValidationException.class, () -> facture("0"));
        assertThrows(ValidationException.class, () -> facture("-5"));
        assertThrows(ValidationException.class,
                () -> service.creer(null, BigDecimal.TEN, null));
        assertThrows(ValidationException.class,
                () -> service.creer(livree(), null, null));
    }

    @Test
    void factureDontLEcheanceEstPasseeEstEnRetard() {
        Facture echue = service.creer(livree(), BigDecimal.TEN, 30, AUJOURDHUI.minusDays(31));
        Facture echueAujourdhui = service.creer(livree(), BigDecimal.TEN, 30, AUJOURDHUI.minusDays(30));
        Facture recente = facture("10");

        assertTrue(service.estEnRetard(echue));
        assertEquals("En retard", service.libelleStatut(echue));
        assertFalse(service.estEnRetard(echueAujourdhui)); // due today: not late yet
        assertFalse(service.estEnRetard(recente));
        assertEquals("En attente", service.libelleStatut(recente));
        assertEquals(StatutFacture.EN_ATTENTE, echue.getStatut()); // never stored as a status
    }

    @Test
    void factureRegleeOuAnnuleeNEstJamaisEnRetard() {
        Facture payee = service.creer(livree(), BigDecimal.TEN, 30, AUJOURDHUI.minusDays(60));
        Facture annulee = service.creer(livree(), BigDecimal.TEN, 30, AUJOURDHUI.minusDays(60));
        service.enregistrerPaiement(payee.getId(), ModePaiement.VIREMENT, "VIR-1", AUJOURDHUI);
        service.annuler(annulee.getId());

        assertFalse(service.estEnRetard(payee));
        assertFalse(service.estEnRetard(annulee));
    }

    @Test
    void paiementPasseLaFactureAPayee() {
        Facture f = facture("1000");

        service.enregistrerPaiement(f.getId(), ModePaiement.CHEQUE, " CHQ-4521 ", AUJOURDHUI);

        assertEquals(StatutFacture.PAYEE, service.trouver(f.getId()).getStatut());
        assertEquals(1, paiementDao.findByFacture(f.getId()).size());
        assertEquals("CHQ-4521", paiementDao.findByFacture(f.getId()).get(0).getReference());
    }

    @Test
    void factureAnnuleeNePeutPasEtreReglee() {
        Facture f = facture("1000");
        service.annuler(f.getId());

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-1", AUJOURDHUI));
        assertEquals("Une facture annulée ne peut pas être réglée.", e.getMessage());
        assertTrue(paiementDao.findAll().isEmpty());
    }

    @Test
    void factureDejaRegleeNePeutPasEtreRegleeDeuxFois() {
        Facture f = facture("1000");
        service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-1", AUJOURDHUI);

        assertThrows(ValidationException.class,
                () -> service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-2", AUJOURDHUI));
        assertEquals(1, paiementDao.findAll().size());
    }

    @Test
    void paiementIncompletOuDateIncoherenteEstRefuse() {
        Facture f = facture("1000");

        assertThrows(ValidationException.class,
                () -> service.enregistrerPaiement(f.getId(), null, "VIR-1", AUJOURDHUI));
        assertThrows(ValidationException.class,
                () -> service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, " ", AUJOURDHUI));
        assertThrows(ValidationException.class,
                () -> service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-1", null));
        assertThrows(ValidationException.class,
                () -> service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-1", AUJOURDHUI.plusDays(1)));
        assertThrows(ValidationException.class,
                () -> service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-1", AUJOURDHUI.minusDays(1)));
        assertEquals(StatutFacture.EN_ATTENTE, service.trouver(f.getId()).getStatut());
    }

    @Test
    void factureRegleeNePeutPasEtreAnnulee() {
        Facture f = facture("1000");
        service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-1", AUJOURDHUI);

        assertThrows(ValidationException.class, () -> service.annuler(f.getId()));
    }

    @Test
    void syntheseCalculeeDepuisLesFactures() {
        Facture payee = facture("1000");                     // this month, paid today
        facture("2000");                                     // this month, pending
        service.creer(livree(), new BigDecimal("500"), 30, AUJOURDHUI.minusDays(45)); // overdue
        Facture annulee = facture("9999");
        service.annuler(annulee.getId());
        service.enregistrerPaiement(payee.getId(), ModePaiement.VIREMENT, "VIR-1", AUJOURDHUI);

        FactureService.Synthese s = service.synthese();

        assertEquals(new BigDecimal("3000.00"), s.chiffreAffairesMoisHT()); // cancelled excluded
        assertEquals(2, s.facturesDuMois());
        assertEquals(new BigDecimal("3420.00"), s.factureMoisTtc());
        assertEquals(new BigDecimal("1140.00"), s.encaissementsMois());
        assertEquals(new BigDecimal("570.00"), s.creancesEnRetard());
        assertEquals(1, s.facturesEnRetard());
    }

    @Test
    void listeTrieeDeLaPlusRecenteALaPlusAncienne() {
        Facture ancienne = service.creer(livree(), BigDecimal.TEN, null, AUJOURDHUI.minusDays(10));
        Facture recente = facture("10");

        assertEquals(recente.getId(), service.lister().get(0).getId());
        assertEquals(ancienne.getId(), service.dernieres(5).get(1).getId());
    }

    @Test
    void missionInconnueOuClientDisparuSontRefusesSansEffetDeBord() {
        assertThrows(ValidationException.class, () -> service.creer(999L, BigDecimal.TEN, null));
        assertTrue(service.lister().isEmpty());

        Mission mission = flotte.livree(client.getId());
        clientDao.delete(client.getId());
        assertThrows(ValidationException.class, () -> service.creer(mission.getId(), BigDecimal.TEN, null));
        assertTrue(service.lister().isEmpty());
    }

    @Test
    void delaiDePaiementNegatifEstRefuse() {
        assertThrows(ValidationException.class, () -> service.creer(livree(), BigDecimal.TEN, -1));
        assertTrue(service.lister().isEmpty());
    }

    @Test
    void paiementALaDateDEcheanceEstAccepteMemeSiLaFactureEstAncienne() {
        Facture f = service.creer(livree(), BigDecimal.TEN, 30, AUJOURDHUI.minusDays(30));

        assertFalse(service.estEnRetard(f)); // due today, not late
        service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-ECHEANCE", AUJOURDHUI);

        assertEquals(StatutFacture.PAYEE, service.trouver(f.getId()).getStatut());
        assertEquals("Payée", service.libelleStatut(service.trouver(f.getId())));
    }

    @Test
    void annulerDeuxFoisResteIdempotentEtBloqueLeReglement() {
        Facture f = facture("1000");
        service.annuler(f.getId());
        service.annuler(f.getId());

        assertEquals(StatutFacture.ANNULEE, service.trouver(f.getId()).getStatut());
        assertEquals(1, service.lister().size());
        assertTrue(paiementDao.findAll().isEmpty());
        assertThrows(ValidationException.class,
                () -> service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-1", AUJOURDHUI));
    }

    @Test
    void trouverUneFactureInconnueDonneUnMessageComprehensible() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.trouver(999L));
        assertEquals("Facture introuvable.", e.getMessage());
        assertThrows(ValidationException.class, () -> service.enregistrerPaiement(999L,
                ModePaiement.VIREMENT, "VIR-1", AUJOURDHUI));
        assertThrows(ValidationException.class, () -> service.annuler(999L));
    }

    @Test
    void syntheseSansDonneeEstAZero() {
        FactureService.Synthese s = service.synthese();

        assertEquals(0, s.chiffreAffairesMoisHT().compareTo(BigDecimal.ZERO));
        assertEquals(0, s.facturesDuMois());
        assertEquals(0, s.factureMoisTtc().compareTo(BigDecimal.ZERO));
        assertEquals(0, s.encaissementsMois().compareTo(BigDecimal.ZERO));
        assertEquals(0, s.creancesEnRetard().compareTo(BigDecimal.ZERO));
        assertEquals(0, s.facturesEnRetard());
    }

    @Test
    void dernieresRespecteLaLimiteDemandee() {
        facture("10");
        facture("20");
        facture("30");

        assertEquals(2, service.dernieres(2).size());
        assertTrue(service.dernieres(0).isEmpty());
        assertEquals(3, service.dernieres(100).size());
    }

    @ParameterizedTest(name = "échéance à J-{0} -> {1}")
    @CsvSource({
            "0, NON_ECHUE",
            "1, J1_A_30",
            "30, J1_A_30",
            "31, J31_A_60",
            "60, J31_A_60",
            "61, J61_A_90",
            "90, J61_A_90",
            "91, PLUS_DE_90"
    })
    void uneFactureVaDansLaTrancheExacteSelonLesJoursDeRetard(int jours, TrancheAge attendue) {
        factureEcheue("1000", jours);

        TrancheAge obtenue = service.recouvrement().tranches().stream()
                .filter(t -> t.nombreFactures() > 0)
                .map(TrancheAgee::tranche)
                .findFirst()
                .orElseThrow();

        assertEquals(attendue, obtenue);
    }

    @Test
    void balanceAgeeCompteEtSommeLesFacturesParTranche() {
        for (int jours : new int[] {0, 1, 30, 31, 60, 61, 90, 91}) {
            factureEcheue("1000", jours);
        }

        Recouvrement r = service.recouvrement();

        assertEquals(1, tranche(r, TrancheAge.NON_ECHUE).nombreFactures());
        assertEquals(new BigDecimal("1140.00"), tranche(r, TrancheAge.NON_ECHUE).montantTtc());
        assertEquals(2, tranche(r, TrancheAge.J1_A_30).nombreFactures());
        assertEquals(new BigDecimal("2280.00"), tranche(r, TrancheAge.J1_A_30).montantTtc());
        assertEquals(2, tranche(r, TrancheAge.J31_A_60).nombreFactures());
        assertEquals(new BigDecimal("2280.00"), tranche(r, TrancheAge.J31_A_60).montantTtc());
        assertEquals(2, tranche(r, TrancheAge.J61_A_90).nombreFactures());
        assertEquals(new BigDecimal("2280.00"), tranche(r, TrancheAge.J61_A_90).montantTtc());
        assertEquals(1, tranche(r, TrancheAge.PLUS_DE_90).nombreFactures());
        assertEquals(new BigDecimal("1140.00"), tranche(r, TrancheAge.PLUS_DE_90).montantTtc());
        assertEquals(new BigDecimal("7980.00"), r.totalEnRetard());
        assertEquals(1, r.nombreClientsARelancer());
        assertEquals(Long.valueOf(91), r.plusAncienDelaiJours());
    }

    @Test
    void facturesRegleesEtAnnuleesSontExcluesDuRecouvrement() {
        Facture payee = factureEcheue("1000", 45);
        Facture annulee = factureEcheue("1000", 45);
        service.enregistrerPaiement(payee.getId(), ModePaiement.VIREMENT, "VIR-REC-1", AUJOURDHUI);
        service.annuler(annulee.getId());

        Recouvrement r = service.recouvrement();

        assertEquals(0, r.totalEnRetard().compareTo(BigDecimal.ZERO));
        assertTrue(r.clientsARelancer().isEmpty());
        assertNull(r.plusAncienDelaiJours());
        assertEquals(0, tranche(r, TrancheAge.J31_A_60).nombreFactures());
    }

    @Test
    void clientsARelancerSontGroupesEtTriesDuPlusUrgentAuMoinsUrgent() {
        Client urgent = clientDao.save(new Client(null, "Atlas Prime", "ICE-REC-1", null, null, null, 30));
        Client recent = clientDao.save(new Client(null, "Souss Express", "ICE-REC-2", null, null, null, 30));
        factureEcheue(urgent, "2000", 95);
        factureEcheue(urgent, "1000", 30);
        factureEcheue(recent, "3000", 5);

        Recouvrement r = service.recouvrement();

        assertEquals(2, r.nombreClientsARelancer());
        assertEquals(Long.valueOf(95), r.plusAncienDelaiJours());
        assertEquals(new BigDecimal("6840.00"), r.totalEnRetard());
        assertEquals(List.of("Atlas Prime", "Souss Express"),
                r.clientsARelancer().stream().map(ClientARelancer::nomClient).toList());

        ClientARelancer premier = r.clientsARelancer().get(0);
        assertEquals(urgent.getId(), premier.clientId());
        assertEquals("ICE-REC-1", premier.ice());
        assertEquals(2, premier.nombreFacturesEnRetard());
        assertEquals(new BigDecimal("3420.00"), premier.totalDuTtc());
        assertEquals(AUJOURDHUI.minusDays(95), premier.echeanceLaPlusAncienne());
        assertEquals(95L, premier.joursDeRetard());
    }

    @Test
    void aEgalRetardLeMontantDuLePlusElevePasseAvant() {
        Client petit = clientDao.save(new Client(null, "Petit Client", "ICE-REC-3", null, null, null, 30));
        Client gros = clientDao.save(new Client(null, "Gros Client", "ICE-REC-4", null, null, null, 30));
        factureEcheue(petit, "1000", 20);
        factureEcheue(gros, "5000", 20);

        List<String> noms = service.recouvrement().clientsARelancer().stream()
                .map(ClientARelancer::nomClient)
                .toList();

        assertEquals(List.of("Gros Client", "Petit Client"), noms);
    }

    @Test
    void factureSansEcheanceCompteCommeNonEchue() {
        Facture sansEcheance = new Facture();
        sansEcheance.setNumero("FACT-2026-900");
        sansEcheance.setClientId(client.getId());
        sansEcheance.setMontantTtc(new BigDecimal("1140.00"));
        sansEcheance.setStatut(StatutFacture.EN_ATTENTE);
        factureDao.save(sansEcheance);

        Recouvrement r = service.recouvrement();

        assertEquals(1, tranche(r, TrancheAge.NON_ECHUE).nombreFactures());
        assertEquals(new BigDecimal("1140.00"), tranche(r, TrancheAge.NON_ECHUE).montantTtc());
        assertTrue(r.clientsARelancer().isEmpty());
        assertNull(r.plusAncienDelaiJours());
    }

    @Test
    void recouvrementSansFactureEstAZero() {
        Recouvrement r = service.recouvrement();

        assertEquals(5, r.tranches().size());
        assertTrue(r.tranches().stream().allMatch(t -> t.nombreFactures() == 0));
        assertTrue(r.clientsARelancer().isEmpty());
        assertEquals(0, r.totalEnRetard().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.nombreClientsARelancer());
        assertNull(r.plusAncienDelaiJours());
    }
}
