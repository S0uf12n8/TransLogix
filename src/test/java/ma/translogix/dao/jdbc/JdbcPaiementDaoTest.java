package ma.translogix.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import ma.translogix.dao.ClientDao;
import ma.translogix.dao.DonneesException;
import ma.translogix.dao.FactureDao;
import ma.translogix.dao.PaiementDao;
import ma.translogix.dao.PaiementDaoContrat;
import ma.translogix.model.ModePaiement;
import ma.translogix.model.Paiement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Integration test against a real MySQL (CI). Same contract as the in-memory DAO, plus the database rules. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class JdbcPaiementDaoTest extends PaiementDaoContrat {

    @Override
    protected PaiementDao daoVide() {
        return new JdbcPaiementDao(BaseDeTest.prete());
    }

    @Override
    protected ClientDao clientDaoVide() {
        return new JdbcClientDao(BaseDeTest.prete());
    }

    @Override
    protected FactureDao factureDaoVide() {
        return new JdbcFactureDao(BaseDeTest.prete());
    }

    @Test
    void factureInconnueRefuseParLaBase() {
        Paiement paiement = new Paiement(null, 999_999L, ModePaiement.VIREMENT,
                "VIR-INCONNU", LocalDate.of(2026, 11, 23));

        DonneesException ex = assertThrows(DonneesException.class, () -> dao.save(paiement));

        assertEquals(DonneesException.Type.REFERENCE_INCONNUE, ex.getType());
        assertTrue(dao.findAll().isEmpty());
    }
}
