package ma.translogix.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ma.translogix.dao.ClientDao;
import ma.translogix.dao.ClientDaoContrat;
import ma.translogix.dao.DonneesException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Integration test against a real MySQL (CI). Same contract as the in-memory DAO, plus the database rules. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class JdbcClientDaoTest extends ClientDaoContrat {

    @Override
    protected ClientDao daoVide() {
        return new JdbcClientDao(BaseDeTest.prete());
    }

    @Test
    void iceEnDoubleRefuseParLaBase() {
        dao.save(client("Premier", "ICE-0009"));

        DonneesException ex = assertThrows(DonneesException.class,
                () -> dao.save(client("Second", "ICE-0009")));

        assertEquals(DonneesException.Type.DOUBLON, ex.getType());
        assertEquals(1, dao.findAll().size());
    }
}
