package ma.translogix.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ma.translogix.dao.ClientDao;
import ma.translogix.dao.DonneesException;
import ma.translogix.dao.FactureDao;
import ma.translogix.dao.FactureDaoContrat;
import ma.translogix.model.Client;
import ma.translogix.model.StatutFacture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Integration test against a real MySQL (CI). Same contract as the in-memory DAO, plus the database rules. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class JdbcFactureDaoTest extends FactureDaoContrat {

    @Override
    protected FactureDao daoVide() {
        return new JdbcFactureDao(BaseDeTest.prete());
    }

    @Override
    protected ClientDao clientDaoVide() {
        return new JdbcClientDao(BaseDeTest.prete());
    }

    @Test
    void numeroEnDoubleRefuseParLaBase() {
        Client client = client("Atlas Transport", "ICE-F015");

        dao.save(facture(client, "FACT-2026-DOUBLON", "114.00", StatutFacture.EN_ATTENTE));
        DonneesException ex = assertThrows(DonneesException.class,
                () -> dao.save(facture(client, "FACT-2026-DOUBLON", "228.00", StatutFacture.PAYEE)));

        assertEquals(DonneesException.Type.DOUBLON, ex.getType());
        assertEquals(1, dao.findAll().size());
    }

    @Test
    void supprimerClientAvecFacturesRefuseParLaBase() {
        Client client = client("Atlas Transport", "ICE-F016");
        dao.save(facture(client, "FACT-2026-021", "114.00", StatutFacture.EN_ATTENTE));

        DonneesException ex = assertThrows(DonneesException.class, () -> clients.delete(client.getId()));

        assertEquals(DonneesException.Type.REFERENCE_UTILISEE, ex.getType());
    }
}
