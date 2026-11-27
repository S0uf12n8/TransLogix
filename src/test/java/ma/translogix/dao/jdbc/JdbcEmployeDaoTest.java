package ma.translogix.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import ma.translogix.dao.DonneesException;
import ma.translogix.dao.DocumentConformiteDao;
import ma.translogix.dao.EmployeDao;
import ma.translogix.dao.EmployeDaoContrat;
import ma.translogix.model.DocumentConformite;
import ma.translogix.model.Employe;
import ma.translogix.model.Poste;
import ma.translogix.model.TypeDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Integration test against a real MySQL (CI). Same contract as the in-memory DAO, plus the database rules. */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class JdbcEmployeDaoTest extends EmployeDaoContrat {

    @Override
    protected EmployeDao daoVide() {
        return new JdbcEmployeDao(BaseRhTest.prete());
    }

    @Test
    void cinEnDoubleRefuseParLaBase() {
        dao.save(employe("MAT-TEST-JDBC-001", "PremierFictif", "ZZ-JDBC-001", Poste.CHAUFFEUR));

        DonneesException ex = assertThrows(DonneesException.class,
                () -> dao.save(employe("MAT-TEST-JDBC-002", "SecondFictif", "ZZ-JDBC-001", Poste.CHAUFFEUR)));

        assertEquals(DonneesException.Type.DOUBLON, ex.getType());
        assertEquals(1, dao.findAll().size());
    }

    @Test
    void matriculeEnDoubleRefuseParLaBase() {
        dao.save(employe("MAT-TEST-JDBC-003", "PremierFictif", "ZZ-JDBC-003", Poste.CHAUFFEUR));

        DonneesException ex = assertThrows(DonneesException.class,
                () -> dao.save(employe("MAT-TEST-JDBC-003", "SecondFictif", "ZZ-JDBC-004", Poste.CHAUFFEUR)));

        assertEquals(DonneesException.Type.DOUBLON, ex.getType());
        assertEquals(1, dao.findAll().size());
    }

    @Test
    void compteDejaLieRefuseParLaBase() {
        Employe premier = employe("MAT-TEST-JDBC-005", "PremierFictif", "ZZ-JDBC-005", Poste.CHAUFFEUR);
        premier.setUtilisateurId(1234L);
        dao.save(premier);
        Employe second = employe("MAT-TEST-JDBC-006", "SecondFictif", "ZZ-JDBC-006", Poste.CHAUFFEUR);
        second.setUtilisateurId(1234L);

        DonneesException ex = assertThrows(DonneesException.class, () -> dao.save(second));

        assertEquals(DonneesException.Type.DOUBLON, ex.getType());
        assertEquals(1, dao.findAll().size());
    }

    @Test
    void employeAvecDocumentsNePeutPasEtreSupprime() {
        Employe employe = dao.save(employe("MAT-TEST-JDBC-007", "ParentFictif", "ZZ-JDBC-007", Poste.CHAUFFEUR));
        DocumentConformiteDao documents = new JdbcDocumentConformiteDao(BaseDeTest.connexion());
        documents.save(new DocumentConformite(null, employe.getId(), TypeDocument.PERMIS, "PERMIS-TEST-JDBC-007",
                null, LocalDate.now().plusYears(1)));

        DonneesException ex = assertThrows(DonneesException.class, () -> dao.delete(employe.getId()));

        assertEquals(DonneesException.Type.REFERENCE_UTILISEE, ex.getType());
        assertEquals(1, dao.findAll().size());
    }
}
