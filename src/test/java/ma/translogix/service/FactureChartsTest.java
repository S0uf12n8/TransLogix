package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Data behind the accountant charts: status distribution and monthly revenue (same rule as the KPI). */
class FactureChartsTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 17);

    private FactureService service;
    private MissionsDeTest flotte;
    private Client client;

    @BeforeEach
    void setUp() {
        InMemoryClientDao clientDao = new InMemoryClientDao();
        InMemoryFactureDao factureDao = new InMemoryFactureDao();
        flotte = new MissionsDeTest(clientDao, factureDao, HORLOGE);
        service = new FactureService(factureDao, clientDao, new InMemoryPaiementDao(), flotte.missions, HORLOGE);
        client = clientDao.save(new Client(null, "LogiMaroc SARL", null, null, null, "Casablanca", 30));
    }

    private Facture facture(String ht, LocalDate emission) {
        return service.creer(flotte.livree(client.getId()).getId(), new BigDecimal(ht), null, emission);
    }

    @Test
    void statutsComptentLeRetardCalculeSansLeStocker() {
        facture("100", AUJOURDHUI);                       // En attente
        facture("200", AUJOURDHUI.minusDays(45));         // En retard (30-day delay)
        Facture payee = facture("300", AUJOURDHUI);
        service.enregistrerPaiement(payee.getId(), ModePaiement.VIREMENT, "VIR-1", AUJOURDHUI);
        Facture annulee = facture("400", AUJOURDHUI);
        service.annuler(annulee.getId());

        assertEquals(List.of(
                new FactureService.Part("Payée", 1),
                new FactureService.Part("En attente", 1),
                new FactureService.Part("En retard", 1),
                new FactureService.Part("Annulée", 1)), service.facturesParStatut());
    }

    @Test
    void gardeLesQuatreTranchesMemeSansDonnees() {
        assertEquals(List.of(
                new FactureService.Part("Payée", 0),
                new FactureService.Part("En attente", 0),
                new FactureService.Part("En retard", 0),
                new FactureService.Part("Annulée", 0)), service.facturesParStatut());
    }

    @Test
    void chiffreAffairesParMoisRemplitLesMoisVidesEtIgnoreLesAnnulees() {
        facture("1000", LocalDate.of(2026, 11, 5));       // 1000 HT this month
        facture("2000", LocalDate.of(2026, 9, 5));        // 2000 HT, two months ago
        facture("5000", LocalDate.of(2026, 5, 5));        // outside a 6-month window
        Facture annulee = facture("9999", LocalDate.of(2026, 11, 6));
        service.annuler(annulee.getId());

        List<FactureService.ChiffreMensuel> mois = service.chiffreAffairesParMois(6);

        assertEquals(6, mois.size());
        assertEquals(YearMonth.of(2026, 6), mois.get(0).mois());
        assertEquals(BigDecimal.ZERO.setScale(2), mois.get(0).montantHt());
        assertEquals(BigDecimal.ZERO.setScale(2), mois.get(2).montantHt()); // October: no invoice
        assertEquals(new BigDecimal("2000.00"), mois.get(3).montantHt());
        assertEquals(new BigDecimal("1000.00"), mois.get(5).montantHt());
    }

    @Test
    void periodeDeDouzeMoisInclutLesAnciennesFacturesEtPasLeFutur() {
        facture("1500", LocalDate.of(2026, 1, 10));
        facture("2500", LocalDate.of(2025, 12, 10));
        facture("9000", LocalDate.of(2026, 12, 10)); // future: excluded

        List<FactureService.ChiffreMensuel> mois = service.chiffreAffairesParMois(12);

        assertEquals(12, mois.size());
        assertEquals(YearMonth.of(2025, 12), mois.get(0).mois());
        assertEquals(new BigDecimal("2500.00"), mois.get(0).montantHt());
        assertEquals(new BigDecimal("1500.00"), mois.get(1).montantHt());
        assertEquals(BigDecimal.ZERO.setScale(2), mois.get(11).montantHt());
    }

    @Test
    void unPaiementNeCreePasDeChiffreDAffairesSupplementaire() {
        Facture f = facture("1000", AUJOURDHUI);
        List<FactureService.ChiffreMensuel> avant = service.chiffreAffairesParMois(6);
        service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-1", AUJOURDHUI);

        assertEquals(avant, service.chiffreAffairesParMois(6));
    }
}
