package ma.translogix.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ma.translogix.dao.DonneesException;
import ma.translogix.dao.UtilisateurDao;
import ma.translogix.dao.UtilisateurDaoContrat;
import ma.translogix.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Integration test against a real MySQL (CI). Same contract as the in-memory DAO, plus the database rules. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class JdbcUtilisateurDaoTest extends UtilisateurDaoContrat {

    @Override
    protected UtilisateurDao daoVide() {
        return new JdbcUtilisateurDao(BaseDeTest.prete());
    }

    @Test
    void emailEnDoubleRefuseParLaBaseMemeEnMajuscules() {
        dao.save(compte("sara@translogix.ma", Role.DISPATCHER));

        DonneesException ex = assertThrows(DonneesException.class,
                () -> dao.save(compte("SARA@translogix.ma", Role.RH)));

        assertEquals(DonneesException.Type.DOUBLON, ex.getType());
        assertEquals(1, dao.findAll().size());
    }

    @Test
    void roleInconnuRefuseParLaContrainteCheck() {
        // A CHECK violation is MySQL error 3819: not a known code, so it is reported without leaking SQL.
        DonneesException ex = assertThrows(DonneesException.class, () -> BaseDeTest.executer(
                "INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, role) "
                        + "VALUES ('A', 'B', 'x@translogix.ma', 'h', 'PIRATE')"));

        assertEquals(DonneesException.Type.AUTRE, ex.getType());
    }
}
