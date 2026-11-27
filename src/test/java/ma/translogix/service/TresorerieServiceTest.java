package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryPaiementDao;
import ma.translogix.model.Client;
import ma.translogix.model.Facture;
import ma.translogix.model.ModePaiement;
import ma.translogix.model.StatutFacture;
import ma.translogix.service.TresorerieService.CritereTresorerie;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Trésorerie: invoiced revenue and cash-in are two distinct series, never added together. */
class TresorerieServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 17);
    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);

    private InMemoryClientDao clientDao;
    private InMemoryFactureDao factureDao;
    private InMemoryPaiementDao paiementDao;
    private FactureService factures;
    private MissionsDeTest flotte;
    private TresorerieService service;
    private Client premier;
    private Client second;

    @BeforeEach
    void setUp() {
        clientDao = new InMemoryClientDao();
        factureDao = new InMemoryFactureDao();
        paiementDao = new InMemoryPaiementDao();
        flotte = new MissionsDeTest(clientDao, factureDao, HORLOGE);
        factures = new FactureService(factureDao, clientDao, paiementDao, flotte.missions, HORLOGE);
        service = new TresorerieService(factures, new ClientService(clientDao, factureDao), HORLOGE);
        premier = clientDao.save(new Client(null, "LogiMaroc SARL", null, null, null, null, 30));
        second = clientDao.save(new Client(null, "Atlas Prime", null, null, null, null, 30));
    }

    private Facture facture(Client client, String ht, LocalDate emission) {
        return factures.creer(flotte.livree(client.getId()).getId(), new BigDecimal(ht), 30, emission);
    }

    @Test
    void factureEtReglementNeSontPasAdditionnes() {
        Facture f = facture(premier, "1000", LocalDate.of(2026, 11, 2));
        factures.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-001", LocalDate.of(2026, 11, 5));

        var resultat = service.generer(CritereTresorerie.TOUT);

        assertEquals(new BigDecimal("1000.00"), resultat.synthese().factureHt());
        assertEquals(new BigDecimal("1140.00"), resultat.synthese().factureTtc());
        assertEquals(new BigDecimal("1140.00"), resultat.synthese().encaisse());
        assertEquals(BigDecimal.ZERO.setScale(2), resultat.synthese().creancesTtc());
        assertEquals(new BigDecimal("100.00"), resultat.synthese().tauxEncaissement());
        assertEquals(1, resultat.synthese().nombreEncaissements());
        assertEquals(1, resultat.encaissements().size());
        assertEquals("VIR-001", resultat.encaissements().get(0).reference());
        assertEquals("LogiMaroc SARL", resultat.encaissements().get(0).client());
    }

    @Test
    void factureImpayeeCompteCommeCreanceEtPasCommeEncaissement() {
        facture(premier, "1000", LocalDate.of(2026, 11, 2));

        var resultat = service.generer(CritereTresorerie.TOUT);

        assertEquals(new BigDecimal("1140.00"), resultat.synthese().factureTtc());
        assertEquals(BigDecimal.ZERO.setScale(2), resultat.synthese().encaisse());
        assertEquals(new BigDecimal("1140.00"), resultat.synthese().creancesTtc());
        assertEquals(BigDecimal.ZERO.setScale(2), resultat.synthese().tauxEncaissement());
        assertTrue(resultat.encaissements().isEmpty());
    }

    @Test
    void factureAnnuleeApparaitDansLeStatutMaisPasDansLesMontants() {
        Facture annulee = facture(premier, "1000", LocalDate.of(2026, 11, 2));
        factures.annuler(annulee.getId());

        var resultat = service.generer(CritereTresorerie.TOUT);

        assertEquals(BigDecimal.ZERO.setScale(2), resultat.synthese().factureTtc());
        assertEquals(0, resultat.synthese().nombreFactures());
        assertEquals(1, resultat.statuts().stream()
                .filter(l -> "Annulée".equals(l.statut()))
                .mapToInt(TresorerieService.LigneStatut::nombre)
                .sum());
    }

    @Test
    void periodeFiltreLesFacturesEtLesEncaissements() {
        Facture ancienne = facture(premier, "1000", LocalDate.of(2026, 8, 2));
        factures.enregistrerPaiement(ancienne.getId(), ModePaiement.CHEQUE, "CHQ-001", LocalDate.of(2026, 8, 4));
        facture(premier, "500", LocalDate.of(2026, 11, 2));

        var resultat = service.generer(new CritereTresorerie(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, null));

        assertEquals(new BigDecimal("570.00"), resultat.synthese().factureTtc());
        assertEquals(BigDecimal.ZERO.setScale(2), resultat.synthese().encaisse());
        assertTrue(resultat.encaissements().isEmpty());
    }

    @Test
    void clientEtModeFilentLeDetailDesEncaissements() {
        Facture f1 = facture(premier, "1000", LocalDate.of(2026, 11, 2));
        Facture f2 = facture(second, "2000", LocalDate.of(2026, 11, 3));
        factures.enregistrerPaiement(f1.getId(), ModePaiement.VIREMENT, "VIR-001", LocalDate.of(2026, 11, 5));
        factures.enregistrerPaiement(f2.getId(), ModePaiement.CHEQUE, "CHQ-002", LocalDate.of(2026, 11, 6));

        var parClient = service.generer(new CritereTresorerie(null, null, second.getId(), null));
        assertEquals(new BigDecimal("2280.00"), parClient.synthese().factureTtc());
        assertEquals(new BigDecimal("2280.00"), parClient.synthese().encaisse());
        assertEquals(List.of("CHQ-002"), parClient.encaissements().stream()
                .map(TresorerieService.Encaissement::reference).toList());

        var parMode = service.generer(new CritereTresorerie(null, null, null, ModePaiement.VIREMENT));
        assertEquals(new BigDecimal("1140.00"), parMode.synthese().encaisse());
        assertEquals(List.of("VIR-001"), parMode.encaissements().stream()
                .map(TresorerieService.Encaissement::reference).toList());
    }

    @Test
    void courbeMensuelleSeparleFactureEtEncaissement() {
        Facture f = facture(premier, "1000", LocalDate.of(2026, 10, 2));
        factures.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-001", LocalDate.of(2026, 11, 5));

        var resultat = service.generer(new CritereTresorerie(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30), null, null));

        assertEquals(2, resultat.mois().size());
        TresorerieService.MoisTresorerie octobre = resultat.mois().get(0);
        TresorerieService.MoisTresorerie novembre = resultat.mois().get(1);
        assertEquals(YearMonth.of(2026, 10), octobre.mois());
        assertEquals(new BigDecimal("1140.00"), octobre.factureTtc());
        assertEquals(BigDecimal.ZERO.setScale(2), octobre.encaisse());
        assertEquals(BigDecimal.ZERO.setScale(2), novembre.factureTtc());
        assertEquals(new BigDecimal("1140.00"), novembre.encaisse());
    }

    @Test
    void periodeInverseeEstRefusee() {
        var critere = new CritereTresorerie(AUJOURDHUI, AUJOURDHUI.minusDays(1), null, null);
        assertThrows(ValidationException.class, () -> service.generer(critere));
    }

    @Test
    void statutsSontDansUnOrdreStableMemeSansDonnees() {
        var resultat = service.generer(CritereTresorerie.TOUT);

        assertEquals(List.of("Payée", "En attente", "En retard", "Annulée"),
                resultat.statuts().stream().map(TresorerieService.LigneStatut::statut).toList());
    }

    @Test
    void uneFactureEnRetardResteDansLaLigneEnRetard() {
        Facture f = facture(premier, "1000", LocalDate.of(2026, 9, 1));
        f.setDateEcheance(AUJOURDHUI.minusDays(10));
        factureDao.update(f);

        var resultat = service.generer(CritereTresorerie.TOUT);

        assertEquals(1, resultat.statuts().stream()
                .filter(l -> "En retard".equals(l.statut()))
                .mapToInt(TresorerieService.LigneStatut::nombre)
                .sum());
    }
}
