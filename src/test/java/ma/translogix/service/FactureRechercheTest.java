package ma.translogix.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import ma.translogix.dao.CritereFacture;
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryPaiementDao;
import ma.translogix.model.Client;
import ma.translogix.model.Facture;
import ma.translogix.model.ModePaiement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Multi-criteria invoice search: every criterion narrows the result, none needs the production database. */
class FactureRechercheTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 17);

    private InMemoryClientDao clientDao;
    private FactureService service;
    private Client logiMaroc;
    private Client atlas;
    private Facture factureRecent;
    private Facture factureAncienne;
    private Facture facturePayee;

    @BeforeEach
    void setUp() {
        clientDao = new InMemoryClientDao();
        InMemoryFactureDao factureDao = new InMemoryFactureDao();
        MissionsDeTest flotte = new MissionsDeTest(clientDao, factureDao, HORLOGE);
        service = new FactureService(factureDao, clientDao, new InMemoryPaiementDao(), flotte.missions, HORLOGE);

        logiMaroc = clientDao.save(new Client(null, "LogiMaroc SARL", "001525487000012", "0522 45 18 90",
                null, "Casablanca", 30));
        atlas = clientDao.save(new Client(null, "Atlas Prime Transports", "002136985000047", null,
                null, "Rabat", 30));

        factureRecent = service.creer(flotte.livree(logiMaroc.getId()).getId(), new BigDecimal("1000"), null);
        factureAncienne = service.creer(flotte.livree(atlas.getId()).getId(), new BigDecimal("2000"), null,
                AUJOURDHUI.minusDays(45));
        facturePayee = service.creer(flotte.livree(logiMaroc.getId()).getId(), new BigDecimal("3000"), null);
        service.enregistrerPaiement(facturePayee.getId(), ModePaiement.VIREMENT, "VIR-1", AUJOURDHUI);
    }

    private List<Long> ids(CritereFacture critere) {
        return service.rechercher(critere).stream().map(Facture::getId).toList();
    }

    @Test
    void sansCritereToutesLesFacturesSontRenvoyeesLesPlusRecentesDAbord() {
        assertEquals(List.of(facturePayee.getId(), factureRecent.getId(), factureAncienne.getId()),
                ids(CritereFacture.TOUT));
        assertEquals(3, ids(null).size());
    }

    @Test
    void rechercheParNumeroEtParReferenceDeTrajet() {
        assertEquals(List.of(factureRecent.getId()),
                ids(new CritereFacture(factureRecent.getNumero(), null, null, null, null, null, null)));
        assertEquals(List.of(factureAncienne.getId()),
                ids(new CritereFacture(factureAncienne.getReferenceTrajet(), null, null, null, null, null, null)));
    }

    @Test
    void rechercheParNomDeClientSansTenirCompteDesAccentsNiDeLaCasse() {
        assertEquals(List.of(facturePayee.getId(), factureRecent.getId()),
                ids(new CritereFacture("logimaroc", null, null, null, null, null, null)));
        assertEquals(List.of(factureAncienne.getId()),
                ids(new CritereFacture("ATLAS PRIME", null, null, null, null, null, null)));
    }

    @Test
    void rechercheParIceEtParTelephoneDuClient() {
        assertEquals(List.of(facturePayee.getId(), factureRecent.getId()),
                ids(new CritereFacture("001525487", null, null, null, null, null, null)));
        assertEquals(List.of(facturePayee.getId(), factureRecent.getId()),
                ids(new CritereFacture("0522", null, null, null, null, null, null)));
    }

    @Test
    void filtreParClient() {
        assertEquals(List.of(factureAncienne.getId()),
                ids(new CritereFacture(null, atlas.getId(), null, null, null, null, null)));
    }

    @Test
    void filtreParStatutStockeEtParStatutCalculeEnRetard() {
        assertEquals(List.of(facturePayee.getId()),
                ids(new CritereFacture(null, null, "Payée", null, null, null, null)));
        assertEquals(List.of(factureAncienne.getId()), // overdue is shown as "En retard", never stored
                ids(new CritereFacture(null, null, "En retard", null, null, null, null)));
        assertEquals(List.of(factureRecent.getId()), // an overdue EN_ATTENTE is not shown as "En attente"
                ids(new CritereFacture(null, null, "En attente", null, null, null, null)));
    }

    @Test
    void filtreParPeriodeEtParMontantTtc() {
        assertEquals(List.of(facturePayee.getId(), factureRecent.getId()),
                ids(new CritereFacture(null, null, null, AUJOURDHUI.minusDays(1), AUJOURDHUI, null, null)));
        assertEquals(List.of(factureAncienne.getId()), // 2000 HT -> 2280 TTC
                ids(new CritereFacture(null, null, null, null, null, new BigDecimal("2000"),
                        new BigDecimal("2300"))));
        assertEquals(List.of(facturePayee.getId()), // 3000 HT -> 3420 TTC
                ids(new CritereFacture(null, null, null, null, null, new BigDecimal("3000"), null)));
    }

    @Test
    void lesCriteresSeCombinentEtUnResultatVideEstSansDanger() {
        CritereFacture combine = new CritereFacture("logimaroc", logiMaroc.getId(), "En attente",
                AUJOURDHUI.minusDays(1), AUJOURDHUI, new BigDecimal("1000"), new BigDecimal("1140"));
        assertEquals(List.of(factureRecent.getId()), ids(combine));

        CritereFacture impossible = new CritereFacture("logimaroc", atlas.getId(), "Annulée", null, null, null, null);
        assertTrue(service.rechercher(impossible).isEmpty());
    }
}
