package ma.translogix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.translogix.model.Client;
import ma.translogix.model.Facture;
import ma.translogix.model.ModePaiement;
import ma.translogix.model.Paiement;
import ma.translogix.model.StatutFacture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What every PaiementDao must do, whatever the storage. The parent invoice is created first so the
 * same tests run against the in-memory lists and against MySQL with its foreign key.
 */
public abstract class PaiementDaoContrat {

    protected PaiementDao dao;
    protected ClientDao clients;
    protected FactureDao factures;

    protected abstract PaiementDao daoVide();

    protected abstract ClientDao clientDaoVide();

    protected abstract FactureDao factureDaoVide();

    @BeforeEach
    void preparer() {
        dao = daoVide();
        clients = clientDaoVide();
        factures = factureDaoVide();
    }

    protected Facture facture(String suffixe) {
        Client client = clients.save(new Client(null, "Client " + suffixe, "ICE-P" + suffixe,
                null, null, null, 30));
        Facture facture = new Facture();
        facture.setNumero("FACT-P-" + suffixe);
        facture.setClientId(client.getId());
        facture.setReferenceTrajet("TRJ-P-" + suffixe);
        facture.setMontantHT(new BigDecimal("100.00"));
        facture.setTauxTva(new BigDecimal("14.00"));
        facture.setMontantTva(new BigDecimal("14.00"));
        facture.setMontantTtc(new BigDecimal("114.00"));
        facture.setDateEmission(LocalDate.of(2026, 11, 1));
        facture.setDateEcheance(LocalDate.of(2026, 12, 1));
        facture.setStatut(StatutFacture.EN_ATTENTE);
        return factures.save(facture);
    }

    protected static Paiement paiement(Facture facture, ModePaiement mode, String reference, LocalDate date) {
        return new Paiement(null, facture.getId(), mode, reference, date);
    }

    @Test
    void saveDonneUnIdEtRelitTousLesChamps() {
        Facture facture = facture("001");
        Paiement cree = dao.save(paiement(facture, ModePaiement.VIREMENT, "VIR-TEST-001",
                LocalDate.of(2026, 11, 20)));

        assertNotNull(cree.getId());
        Paiement relu = dao.findByFacture(facture.getId()).get(0);
        assertEquals(facture.getId(), relu.getFactureId());
        assertEquals(ModePaiement.VIREMENT, relu.getMode());
        assertEquals("VIR-TEST-001", relu.getReference());
        assertEquals(LocalDate.of(2026, 11, 20), relu.getDate());
    }

    @Test
    void idsDistinctsEtFindAllRenvoieTout() {
        Facture facture = facture("002");
        Paiement a = dao.save(paiement(facture, ModePaiement.VIREMENT, "VIR-TEST-002",
                LocalDate.of(2026, 11, 20)));
        Paiement b = dao.save(paiement(facture, ModePaiement.CHEQUE, "CHQ-TEST-002",
                LocalDate.of(2026, 11, 21)));

        assertNotEquals(a.getId(), b.getId());
        assertEquals(List.of("VIR-TEST-002", "CHQ-TEST-002"),
                dao.findAll().stream().map(Paiement::getReference).toList());
    }

    @Test
    void findByFactureNeRenvoieQueLesPaiementsDeLaFacture() {
        Facture premiere = facture("003");
        Facture seconde = facture("004");
        dao.save(paiement(premiere, ModePaiement.VIREMENT, "VIR-TEST-003", LocalDate.of(2026, 11, 20)));
        dao.save(paiement(seconde, ModePaiement.CHEQUE, "CHQ-TEST-003", LocalDate.of(2026, 11, 21)));

        assertEquals(List.of("VIR-TEST-003"),
                dao.findByFacture(premiere.getId()).stream().map(Paiement::getReference).toList());
        assertEquals(List.of("CHQ-TEST-003"),
                dao.findByFacture(seconde.getId()).stream().map(Paiement::getReference).toList());
    }

    @Test
    void factureSansPaiementDonneVide() {
        Facture facture = facture("005");

        assertTrue(dao.findByFacture(facture.getId()).isEmpty());
        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    void referenceNulleEtTousLesModesSontConserves() {
        Facture facture = facture("006");
        dao.save(paiement(facture, ModePaiement.CHEQUE, null, LocalDate.of(2026, 11, 20)));
        dao.save(paiement(facture, ModePaiement.EFFET_DE_COMMERCE, "EFF-TEST-006",
                LocalDate.of(2026, 11, 21)));

        List<Paiement> paiements = dao.findAll();
        assertEquals(ModePaiement.CHEQUE, paiements.get(0).getMode());
        assertNull(paiements.get(0).getReference());
        assertEquals(ModePaiement.EFFET_DE_COMMERCE, paiements.get(1).getMode());
        assertEquals("EFF-TEST-006", paiements.get(1).getReference());
    }
}
