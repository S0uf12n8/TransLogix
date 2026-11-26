package ma.translogix.dao.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Cleans the two RH tables before a JDBC RH test. BaseDeTest already applies db/schema.sql and empties
 * the admin tables; documents must go before their parent employees because of the foreign key.
 */
final class BaseRhTest {

    private BaseRhTest() {
    }

    static ConnexionBase prete() {
        ConnexionBase base = BaseDeTest.prete();
        try (Connection c = base.ouvrir(); Statement st = c.createStatement()) {
            st.execute("DELETE FROM documents_conformite");
            st.execute("DELETE FROM employes");
        } catch (SQLException e) {
            throw new IllegalStateException("Préparation des tables RH impossible", e);
        }
        return base;
    }
}
