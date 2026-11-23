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
import ma.translogix.dao.memory.InMemoryClientDao;
import ma.translogix.dao.memory.InMemoryFactureDao;
import ma.translogix.dao.memory.InMemoryPaiementDao;
import ma.translogix.model.Client;
import ma.translogix.model.Facture;
import ma.translogix.model.ModePaiement;
import ma.translogix.model.Paiement;
import ma.translogix.model.StatutFacture;
import ma.translogix.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Service side of the confirmation menus: cancellation and read-only payment history. */
class FactureActionsTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC);

    private FactureService service;
    private MissionsDeTest flotte;
    private Long clientId;

    @BeforeEach
    void setUp() {
        InMemoryClientDao clientDao = new InMemoryClientDao();
        InMemoryFactureDao factureDao = new InMemoryFactureDao();
        flotte = new MissionsDeTest(clientDao, factureDao, HORLOGE);
        service = new FactureService(factureDao, clientDao, new InMemoryPaiementDao(), flotte.missions, HORLOGE);
        clientId = clientDao.save(new Client(null, "Atlas Prime Transports", null, null, null, "Rabat", 30)).getId();
    }

    private Facture facture() {
        return service.creer(flotte.livree(clientId).getId(), new BigDecimal("1000"), null);
    }

    @Test
    void annulerGardeLaFactureDansLHistoriqueEtBloqueLeReglement() {
        Facture f = facture();

        service.annuler(f.getId());

        assertEquals(StatutFacture.ANNULEE, service.trouver(f.getId()).getStatut());
        assertEquals(1, service.lister().size()); // never a hard delete
        assertThrows(ValidationException.class,
                () -> service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-1", LocalDate.of(2026, 11, 17)));
    }

    @Test
    void annulerUneFactureInexistanteEstRefuseSansEffetDeBord() {
        assertThrows(ValidationException.class, () -> service.annuler(999L));
        assertTrue(service.lister().isEmpty());
    }

    @Test
    void historiqueDesReglementsEstRattacheALaFacture() {
        Facture f = facture();
        Facture autre = facture();
        service.enregistrerPaiement(f.getId(), ModePaiement.VIREMENT, "VIR-1", LocalDate.of(2026, 11, 17));
        service.enregistrerPaiement(autre.getId(), ModePaiement.CHEQUE, "CHQ-2", LocalDate.of(2026, 11, 17));

        List<Paiement> paiements = service.paiementsDe(f.getId());

        assertEquals(1, paiements.size()); // a full payment closes the invoice: no second payment
        assertEquals(List.of("VIR-1"), paiements.stream().map(Paiement::getReference).toList());
        assertTrue(paiements.stream().allMatch(p -> f.getId().equals(p.getFactureId())));
        assertEquals(List.of("CHQ-2"), service.paiementsDe(autre.getId()).stream()
                .map(Paiement::getReference).toList());
    }

    @Test
    void uneFactureSansReglementAUnHistoriqueVide() {
        assertTrue(service.paiementsDe(facture().getId()).isEmpty());
    }
}
