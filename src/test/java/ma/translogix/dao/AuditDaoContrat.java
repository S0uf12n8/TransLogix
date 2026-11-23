package ma.translogix.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import ma.translogix.model.EvenementAudit;
import ma.translogix.model.ResultatAudit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** What every AuditDao must do, whatever the storage (memory and MySQL run the same tests). */
public abstract class AuditDaoContrat {

    protected AuditDao dao;

    /** A DAO with no event in it. */
    protected abstract AuditDao daoVide();

    @BeforeEach
    void preparer() {
        dao = daoVide();
    }

    private static EvenementAudit evenement(String action, LocalDateTime quand) {
        return new EvenementAudit(null, quand, "Sara Alami (Gérant)", "Utilisateurs", action, ResultatAudit.REUSSI);
    }

    @Test
    void journalVide() {
        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    void saveDonneUnIdEtRelitTousLesChamps() {
        LocalDateTime quand = LocalDateTime.of(2026, 11, 15, 9, 30, 12, 345_678_000);
        EvenementAudit e = dao.save(new EvenementAudit(null, quand, "Système", "Facturation",
                "Création de FACT-2026-001 — 24 000,00 MAD", ResultatAudit.REFUSE));

        assertNotNull(e.getId());
        EvenementAudit relu = dao.findAll().get(0);
        assertEquals(e.getId(), relu.getId());
        assertEquals(quand, relu.getHorodatage()); // microseconds kept (DATETIME(6))
        assertEquals("Système", relu.getUtilisateur());
        assertEquals("Facturation", relu.getModule());
        assertEquals("Création de FACT-2026-001 — 24 000,00 MAD", relu.getAction());
        assertEquals(ResultatAudit.REFUSE, relu.getResultat());
    }

    @Test
    void findAllDansLOrdreDInsertionMemeSiLesDatesNeLeSontPas() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 15, 10, 0);
        dao.save(evenement("premier", t.plusHours(2)));
        dao.save(evenement("deuxième", t));
        dao.save(evenement("troisième", t.plusHours(1)));

        List<String> actions = dao.findAll().stream().map(EvenementAudit::getAction).toList();

        assertEquals(List.of("premier", "deuxième", "troisième"), actions);
    }
}
