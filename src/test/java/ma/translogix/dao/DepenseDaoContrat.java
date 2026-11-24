package ma.translogix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import ma.translogix.model.Depense;
import ma.translogix.model.StatutDepense;
import ma.translogix.model.TypeDepense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** What every DepenseDao must do, whatever the storage (in-memory lists or MySQL). */
public abstract class DepenseDaoContrat {

    protected DepenseDao dao;

    protected abstract DepenseDao daoVide();

    @BeforeEach
    void initialiser() {
        dao = daoVide();
    }

    protected static Depense depense() {
        return new Depense(null, 11L, 21L, TypeDepense.CARBURANT, "Station ZZ Casablanca",
                new BigDecimal("850.50"), new BigDecimal("62.40"), LocalDate.of(2026, 11, 17),
                StatutDepense.EN_ATTENTE, null);
    }

    @Test
    void enregistreEtRelitUneDepense() {
        Depense enregistree = dao.save(depense());
        assertNotNull(enregistree.getId());
        Depense relue = dao.findById(enregistree.getId()).orElseThrow();
        assertEquals(11L, relue.getMissionId());
        assertEquals(21L, relue.getChauffeurId());
        assertEquals(TypeDepense.CARBURANT, relue.getType());
        assertEquals("Station ZZ Casablanca", relue.getStation());
        assertEquals(new BigDecimal("850.50"), relue.getMontant());
        assertEquals(new BigDecimal("62.40"), relue.getLitres());
        assertEquals(LocalDate.of(2026, 11, 17), relue.getDate());
        assertEquals(StatutDepense.EN_ATTENTE, relue.getStatut());
    }

    @Test
    void gardeLesChampsOptionnelsVides() {
        Depense enregistree = dao.save(new Depense(null, 12L, 22L, TypeDepense.PEAGE, "Péage ZZ Rabat",
                new BigDecimal("35.00"), null, LocalDate.of(2026, 11, 18), StatutDepense.EN_ATTENTE, null));
        Depense relue = dao.findById(enregistree.getId()).orElseThrow();
        assertEquals(null, relue.getLitres());
        assertEquals(null, relue.getMotifRefus());
    }

    @Test
    void metAJourLeStatutSansPerdreLHistorique() {
        Depense enregistree = dao.save(depense());
        enregistree.setStatut(StatutDepense.REFUSEE);
        enregistree.setMotifRefus("Justificatif illisible");
        dao.update(enregistree);

        Depense relue = dao.findById(enregistree.getId()).orElseThrow();
        assertEquals(StatutDepense.REFUSEE, relue.getStatut());
        assertEquals("Justificatif illisible", relue.getMotifRefus());
        assertEquals(new BigDecimal("850.50"), relue.getMontant());
    }

    @Test
    void refuseUneMiseAJourSurUnIdInconnu() {
        Depense inconnue = depense();
        inconnue.setId(999999L);
        assertThrows(IllegalArgumentException.class, () -> dao.update(inconnue));
    }

    @Test
    void listeToutesLesDepensesSansSuppression() {
        assertTrue(dao.findAll().isEmpty());
        dao.save(depense());
        dao.save(new Depense(null, 13L, 23L, TypeDepense.REPAS, "Restaurant ZZ Fès", new BigDecimal("120.00"),
                null, LocalDate.of(2026, 11, 19), StatutDepense.VALIDEE, null));
        List<Depense> tout = dao.findAll();
        assertEquals(2, tout.size());
    }
}
