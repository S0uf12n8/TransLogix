package ma.translogix.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import ma.translogix.dao.DonneesException;
import org.junit.jupiter.api.Test;

/** Runs everywhere, no database needed. */
class ConnexionBaseTest {

    @Test
    void sansDbUrlPasDeBase() {
        assertTrue(ConnexionBase.depuis(Map.of()).isEmpty());
        assertTrue(ConnexionBase.depuis(Map.of("DB_URL", "  ")).isEmpty());
        assertTrue(ConnexionBase.depuis(Map.of("DB_URL", "jdbc:mysql://h/b", "DB_USER", "u")).isPresent());
    }

    @Test
    void serveurInjoignableDonneUneErreurDeConnexionLisible() {
        // Nothing listens on port 1: the driver fails at once, as when MySQL is stopped.
        ConnexionBase base = new ConnexionBase("jdbc:mysql://127.0.0.1:1/translogix_db?connectTimeout=2000",
                "translogix_user", "secret");

        DonneesException ex = assertThrows(DonneesException.class, base::ouvrir);

        assertEquals(DonneesException.Type.CONNEXION, ex.getType());
        assertFalse(ex.getMessage().contains("jdbc"), ex.getMessage());
        assertFalse(ex.getMessage().contains("secret"), ex.getMessage());
    }

    @Test
    void toStringMasqueLeMotDePasseDeLUrl() {
        String texte = new ConnexionBase("jdbc:mysql://h/b?user=u&password=secret&useSSL=false", "u", "p").toString();

        assertFalse(texte.contains("secret"), texte);
        assertTrue(texte.contains("password=***"), texte);
    }

    @Test
    void schemaDecoupeEnCinqTablesSansCommentaire() {
        List<String> instructions = BaseDeTest.instructions();

        assertEquals(5, instructions.size());
        assertTrue(instructions.get(0).startsWith("CREATE TABLE IF NOT EXISTS utilisateurs"));
        assertTrue(instructions.get(1).startsWith("CREATE TABLE IF NOT EXISTS audit_evenements"));
        assertTrue(instructions.get(2).startsWith("CREATE TABLE IF NOT EXISTS clients"));
        assertTrue(instructions.get(3).startsWith("CREATE TABLE IF NOT EXISTS factures"));
        assertTrue(instructions.get(4).startsWith("CREATE TABLE IF NOT EXISTS paiements"));
        assertTrue(instructions.stream().noneMatch(s -> s.contains("--")));
    }
}
