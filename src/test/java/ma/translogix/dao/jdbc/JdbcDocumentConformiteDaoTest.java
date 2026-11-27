package ma.translogix.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import ma.translogix.dao.DonneesException;
import ma.translogix.dao.DocumentConformiteDao;
import ma.translogix.dao.DocumentConformiteDaoContrat;
import ma.translogix.dao.EmployeDao;
import ma.translogix.model.TypeDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Integration test against a real MySQL (CI). Same contract as the in-memory DAO, plus the foreign key. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class JdbcDocumentConformiteDaoTest extends DocumentConformiteDaoContrat {

    @Override
    protected DocumentConformiteDao daoVide() {
        return new JdbcDocumentConformiteDao(BaseRhTest.prete());
    }

    @Override
    protected EmployeDao employeDaoVide() {
        return new JdbcEmployeDao(BaseRhTest.prete());
    }

    @Test
    void documentSansEmployeRefuseParLaBase() {
        DonneesException ex = assertThrows(DonneesException.class,
                () -> dao.save(document(999_999L, TypeDocument.PERMIS, "PERMIS-TEST-JDBC-008",
                        LocalDate.now().plusYears(1))));

        assertEquals(DonneesException.Type.REFERENCE_INCONNUE, ex.getType());
    }
}
