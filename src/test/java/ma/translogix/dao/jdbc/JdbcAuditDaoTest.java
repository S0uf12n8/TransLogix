package ma.translogix.dao.jdbc;

import ma.translogix.dao.AuditDao;
import ma.translogix.dao.AuditDaoContrat;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Integration test against a real MySQL (CI). Same contract as the in-memory audit DAO. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class JdbcAuditDaoTest extends AuditDaoContrat {

    @Override
    protected AuditDao daoVide() {
        return new JdbcAuditDao(BaseDeTest.prete());
    }
}
