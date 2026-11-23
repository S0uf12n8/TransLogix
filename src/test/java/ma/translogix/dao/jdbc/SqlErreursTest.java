package ma.translogix.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import ma.translogix.dao.DonneesException;
import ma.translogix.dao.DonneesException.Type;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/** The SQLExceptions are built by hand with the codes and states MySQL 8 really sends: no database needed. */
class SqlErreursTest {

    @ParameterizedTest(name = "MySQL {0} / {1} -> {2}")
    @CsvSource({
        "1045, 28000, CONNEXION",
        "1049, 42000, CONNEXION",
        "1062, 23000, DOUBLON",
        "1451, 23000, REFERENCE_UTILISEE",
        "1452, 23000, REFERENCE_INCONNUE",
        "1048, 23000, CHAMP_OBLIGATOIRE",
        "1364, HY000, CHAMP_OBLIGATOIRE",
        "1406, 22001, VALEUR_INVALIDE",
        "1264, 22003, VALEUR_INVALIDE",
        "1366, HY000, VALEUR_INVALIDE",
        "1205, HY000, CONFLIT",
        "1213, 40001, CONFLIT",
        "0, 08S01, CONNEXION",
        "0, 08001, CONNEXION",
        "0, 28000, CONNEXION",
        "0, 40001, CONFLIT",
        "0, 23000, AUTRE",
        "1146, 42S02, AUTRE",
    })
    void codeMySqlPuisClasseSqlState(int code, String etat, Type attendu) {
        DonneesException d = SqlErreurs.traduire(new SQLException("message technique", etat, code));

        assertEquals(attendu, d.getType());
    }

    @Test
    void connexionPerdueSansSqlState() {
        DonneesException d = SqlErreurs.traduire(new SQLNonTransientConnectionException("Communications link failure"));

        assertEquals(Type.CONNEXION, d.getType());
        assertEquals(SqlErreurs.MESSAGE_CONNEXION, d.getMessage());
    }

    @Test
    void messageNeMontreNiSqlNiNomDeTable() {
        SQLException sql = new SQLException("Cannot delete or update a parent row: a foreign key constraint fails "
                + "(`translogix_db`.`missions`, CONSTRAINT `fk_missions_client` FOREIGN KEY (`client_id`))",
                "23000", 1451);

        DonneesException d = SqlErreurs.traduire(sql);

        assertFalse(d.getMessage().contains("missions`"), d.getMessage());
        assertFalse(d.getMessage().contains("translogix_db"), d.getMessage());
        assertFalse(d.getMessage().contains("constraint"), d.getMessage());
        assertSame(sql, d.getCause(), "the technical exception stays attached for the log");
    }

    @Test
    void doublonCiteLaValeurDeLUtilisateurMaisPasLaCle() {
        String message = SqlErreurs.messageDoublon(
                "Duplicate entry 'sara@translogix.ma' for key 'utilisateurs.uk_utilisateurs_email'");

        assertEquals("La valeur « sara@translogix.ma » existe déjà alors qu'elle doit être unique "
                + "(email, ICE, matricule, numéro…). Choisissez-en une autre.", message);
        assertFalse(message.contains("uk_utilisateurs_email"));
    }

    @Test
    void doublonSansValeurLisible() {
        assertEquals("Cette valeur existe déjà alors qu'elle doit être unique (email, ICE, matricule, numéro…). "
                + "Choisissez-en une autre.", SqlErreurs.messageDoublon(null));
    }

    @Test
    void executerRenvoieLeResultatOuTraduitLErreur() {
        assertEquals(42, SqlErreurs.executer(() -> 42));

        DonneesException d = assertThrows(DonneesException.class, () -> SqlErreurs.executer(() -> {
            throw new SQLException("Duplicate entry 'X1' for key 'k'", "23000", 1062);
        }));
        assertEquals(Type.DOUBLON, d.getType());
    }
}
