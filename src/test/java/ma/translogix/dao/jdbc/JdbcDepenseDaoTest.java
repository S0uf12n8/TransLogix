package ma.translogix.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.translogix.dao.DepenseDao;
import ma.translogix.dao.DepenseDaoContrat;
import ma.translogix.dao.DonneesException;
import ma.translogix.model.Depense;
import ma.translogix.model.StatutDepense;
import ma.translogix.model.TypeDepense;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Integration test against a real MySQL (CI). Same contract as the in-memory DAO, plus the database rules. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class JdbcDepenseDaoTest extends DepenseDaoContrat {

    @Override
    protected DepenseDao daoVide() {
        return new JdbcDepenseDao(BaseDeTest.prete());
    }

    @Test
    void refuseUnMontantNegatifParLaContrainteDeLaBase() {
        Depense invalide = new Depense(null, 31L, 41L, TypeDepense.AUTRE, "Station ZZ", new BigDecimal("-1.00"),
                null, LocalDate.of(2026, 11, 17), StatutDepense.EN_ATTENTE, null);
        assertThrows(DonneesException.class, () -> dao.save(invalide));
    }
}
