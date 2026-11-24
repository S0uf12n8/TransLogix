package ma.translogix.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import ma.translogix.dao.DonneesException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Real MySQL check of the rollback boundary; skipped unless DB_URL is set (same rule as the other JDBC tests). */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class TransactionsIntegrationTest {

    @Test
    void erreurApresInsertAnnuleTouteLaTransaction() throws SQLException {
        ConnexionBase base = BaseDeTest.prete();
        String email = "rollback-test@translogix.ma";

        assertThrows(DonneesException.class, () -> Transactions.dansTransaction(base, connexion -> {
            try (PreparedStatement ps = connexion.prepareStatement(
                    "INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, role, actif)"
                            + " VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, "Rollback");
                ps.setString(2, "Test");
                ps.setString(3, email);
                ps.setString(4, "hash");
                ps.setString(5, "DISPATCHER");
                ps.setBoolean(6, true);
                ps.executeUpdate();
            }
            throw new SQLException("erreur simulée après l'insert", "23000", 1062);
        }));

        try (Connection connexion = base.ouvrir();
             PreparedStatement ps = connexion.prepareStatement(
                     "SELECT COUNT(*) FROM utilisateurs WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(0, rs.getInt(1), "l'insert doit avoir été annulé avec le rollback");
            }
        }
    }
}
